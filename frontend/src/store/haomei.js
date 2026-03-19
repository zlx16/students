import { defineStore } from 'pinia'
import { api } from '../services/api.js'
import { connectHaomeiWs } from '../services/ws.js'

function normalizeOrderUpdate(msg) {
  return {
    id: msg.orderId,
    tableNo: msg.tableNo,
    diners: msg.diners,
    customerName: msg.customerName,
    status: msg.status,
    createdAt: msg.createdAt,
    amount: msg.amount,
    paymentMethod: msg.paymentMethod,
    paid: msg.paid,
    paymentStatus: msg.paymentStatus,
    paymentExpiresAt: msg.paymentExpiresAt,
    paymentAttemptId: msg.paymentAttemptId,
    paymentStartedAt: msg.paymentStartedAt,
    items: msg.items || []
  }
}

export const useHaomeiStore = defineStore('haomei', {
  state: () => ({
    dishes: [],
    adminDishes: [],
    categories: [
      { key: 'ALL', label: '全部' },
      { key: 'RECOMMEND', label: '推荐' },
      { key: 'HOT', label: '热菜' },
      { key: 'STAPLE', label: '主食' },
      { key: 'DESSERT', label: '甜品' },
      { key: 'DRINK', label: '饮品' }
    ],
    selectedCategory: 'ALL',
    searchQuery: '',

    tableToken: '',
    tableNo: null,
    diners: null,
    remark: '',

    // cart key: `${dishId}:${portion}` -> qty
    cart: {},
    customerName: localStorage.getItem('haomei_customerName') || '',
    myOrders: [],
    adminOrders: [],
    wsReady: false
  }),
  actions: {
    isActiveOrder(order) {
      return order && order.status !== 'COMPLETED' && order.status !== 'CANCELLED'
    },

    setCustomerName(name) {
      this.customerName = name
      localStorage.setItem('haomei_customerName', name)
    },

    setSelectedCategory(key) {
      this.selectedCategory = key
    },

    setSearchQuery(q) {
      this.searchQuery = q
    },

    async initFromToken(tableToken) {
      this.tableToken = tableToken
      const { data } = await api.get('/api/tables/resolve', { params: { token: tableToken } })
      this.tableNo = data.tableNo
    },

    setDiners(n) {
      const v = Number(n)
      if (!Number.isFinite(v) || v <= 0) throw new Error('用餐人数必须大于0')
      this.diners = v
    },

    cartKey(dishId, portion) {
      return `${dishId}:${portion}`
    },

    cartQty(dishId, portion) {
      return this.cart[this.cartKey(dishId, portion)] || 0
    },

    addToCart(dishId, portion = 'SMALL') {
      const k = this.cartKey(dishId, portion)
      this.cart[k] = (this.cart[k] || 0) + 1
    },

    setCartQty(dishId, portion, qty) {
      const q = Math.max(0, Number(qty) || 0)
      const k = this.cartKey(dishId, portion)
      if (q <= 0) delete this.cart[k]
      else this.cart[k] = q
    },

    clearCart() {
      this.cart = {}
    },

    async fetchDishes() {
      const params = {}
      if (this.searchQuery) params.q = this.searchQuery
      if (this.selectedCategory && this.selectedCategory !== 'ALL') {
        params.category = this.selectedCategory
      }
      const { data } = await api.get('/api/dishes', { params })
      this.dishes = data || []
    },

    async fetchAdminDishes() {
      const { data } = await api.get('/api/admin/dishes')
      this.adminDishes = data || []
    },

    async fetchMyOrders() {
      if (!this.tableNo) return
      const { data } = await api.get('/api/orders', { params: { tableNo: this.tableNo } })
      this.myOrders = (data || []).filter(o => this.isActiveOrder(o))
    },

    async fetchAdminOrders() {
      const { data } = await api.get('/api/admin/orders')
      this.adminOrders = data || []
    },

    async createOrderFromCart() {
      if (!this.tableToken) throw new Error('缺少桌台二维码 token')
      if (!this.diners) throw new Error('请先填写用餐人数')

      const items = Object.entries(this.cart).map(([k, qty]) => {
        const [dishId, portion] = k.split(':')
        return { dishId: Number(dishId), portion, quantity: qty }
      })

      if (!items.length) throw new Error('购物车为空')

      const payload = {
        tableToken: this.tableToken,
        diners: this.diners,
        customerName: this.customerName,
        remark: this.remark,
        items
      }
      const { data } = await api.post('/api/orders', payload)
      this.upsertOrder(this.myOrders, data)
      return data
    },

    async fetchTableBill() {
      if (!this.tableToken) throw new Error('缺少桌台二维码 token')
      const { data } = await api.get('/api/tables/bill', { params: { tableToken: this.tableToken } })
      return data
    },

    async payTableBill(paymentMethod, success = true) {
      if (!this.tableToken) throw new Error('缺少桌台二维码 token')
      const { data } = await api.post('/api/tables/bill/pay', { tableToken: this.tableToken, paymentMethod, success })
      return data
    },

    async payOrder(orderId, paymentMethod) {
      if (!paymentMethod) throw new Error('请选择付款方式')
      // 1) start pay (PAYING)
      const { data: s } = await api.post(`/api/orders/${orderId}/pay/start`, { paymentMethod })
      this.upsertOrder(this.myOrders, s)
      return s
    },

    async confirmPay(orderId, paymentAttemptId, success) {
      const { data } = await api.post(`/api/orders/${orderId}/pay/confirm`, { paymentAttemptId, success })
      this.upsertOrder(this.myOrders, data)
      return data
    },

    async cancelPay(orderId) {
      const { data } = await api.post(`/api/orders/${orderId}/pay/cancel`)
      this.upsertOrder(this.myOrders, data)
      return data
    },

    upsertOrder(list, order) {
      const idx = list.findIndex(o => o.id === order.id)
      if (idx >= 0) list[idx] = order
      else list.unshift(order)
    },

    upsertOrderUpdateMsg(list, msg) {
      const order = normalizeOrderUpdate(msg)
      this.upsertOrder(list, order)
    },

    applyMenuUpdate() {
      // 由于后端可能只推了变化信息，简单起见这里直接重拉菜单
      this.fetchDishes().catch(() => {})
      this.fetchAdminDishes().catch(() => {})
    },

    connectRealtime() {
      if (this.wsReady) return

      connectHaomeiWs({
        onOrder: (msg) => {
          // admin sees all
          this.upsertOrderUpdateMsg(this.adminOrders, msg)

          // user sees only same table
          if (this.tableNo && msg.tableNo === this.tableNo) {
            const order = normalizeOrderUpdate(msg)
            const idx = this.myOrders.findIndex(o => o.id === order.id)
            if (!this.isActiveOrder(order)) {
              if (idx >= 0) this.myOrders.splice(idx, 1)
            } else if (idx >= 0) {
              this.myOrders[idx] = order
            } else {
              this.myOrders.unshift(order)
            }
          }
        },
        onMenu: () => {
          this.applyMenuUpdate()
        }
      })

      this.wsReady = true
    }
  }
})

