<template>
  <div class="page">
    <div v-if="needDiners" class="modal-mask">
      <div class="modal">
        <div class="modal-title">请先填写用餐人数</div>
        <div class="modal-desc">桌号：{{ store.tableNo ?? '...' }}</div>
        <div class="row">
          <label>用餐人数</label>
          <input v-model.number="dinersInput" type="number" min="1" placeholder="例如：3" />
        </div>
        <button class="primary" @click="confirmDiners">确定</button>
        <div v-if="error" class="error">{{ error }}</div>
      </div>
    </div>

    <aside class="panel sidebar">
      <div class="tablebox">
        <div class="table-pill">桌号 {{ store.tableNo ?? '--' }}</div>
        <div class="diners-pill">人数 {{ store.diners ?? '--' }}</div>
      </div>
      <div class="cat-title">分类</div>
      <div class="cat-scroll">
        <button
          v-for="c in store.categories"
          :key="c.key"
          class="cat"
          :class="{ active: store.selectedCategory === c.key }"
          @click="selectCategory(c.key)"
        >
          {{ c.label }}
        </button>
      </div>
    </aside>

    <section class="panel main">
      <div class="topline">
        <div class="search">
          <input v-model="store.searchQuery" @input="debouncedSearch" placeholder="搜索菜名/描述..." />
        </div>
      </div>
      <div class="row" style="margin-bottom:12px;">
        <label style="min-width:70px;">备注</label>
        <input v-model="store.remark" placeholder="例如：少辣/不要香菜/过敏提示..." />
      </div>

      <div class="grid">
        <div v-for="dish in store.dishes" :key="dish.id" class="dish">
          <div class="img">
            <img v-if="dish.imageUrl" :src="dish.imageUrl" alt="" />
            <div v-else class="placeholder">暂无图片</div>
          </div>
          <div class="meta">
            <div>
              <div class="name">{{ dish.name }}</div>
              <div class="desc">{{ dish.description }}</div>
            </div>
            <div class="price">
              <template v-if="dish.portionOption === 'SMALL_ONLY'">小份 ¥ {{ dish.price }}</template>
              <template v-else-if="dish.portionOption === 'LARGE_ONLY'">大份 ¥ {{ largePrice(dish.price) }}</template>
              <template v-else>小份 ¥ {{ dish.price }} / 大份 ¥ {{ largePrice(dish.price) }}</template>
            </div>
            <div class="portion">
              <button v-if="canOrderSmall(dish)" @click="store.addToCart(dish.id, 'SMALL')">小份 +1</button>
              <button v-if="canOrderLarge(dish)" @click="store.addToCart(dish.id, 'LARGE')">大份 +1</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel side">
      <h2 class="panel-title">购物车</h2>
      <div v-if="Object.keys(store.cart).length === 0" class="muted">当前为空</div>
      <div v-for="k in Object.keys(store.cart)" :key="k" class="cart-item">
        <div class="cart-name">{{ cartTitle(k) }}</div>
        <div class="cart-qty">
          <button @click="dec(k)">-</button>
          <span class="qty">{{ store.cart[k] }}</span>
          <button @click="inc(k)">+</button>
        </div>
      </div>
      <div class="actions">
        <button class="primary" @click="pay" :disabled="paying.show">
          {{ paying.show ? '支付处理中...' : '去付款' }}
        </button>
        <button @click="store.clearCart" v-if="Object.keys(store.cart).length">清空</button>
      </div>
      <div v-if="error" class="error">{{ error }}</div>
    </section>

    <section class="panel orders">
      <h2 class="panel-title">本桌订单（实时）</h2>
      <div v-if="store.myOrders.length === 0" class="muted">暂无订单</div>
      <div v-for="o in store.myOrders" :key="o.id" class="order">
        <div class="order-top">
          <div>订单 #{{ o.id }}</div>
          <div class="status">状态：{{ o.status }}</div>
        </div>
        <div class="order-mid">
          <div>时间：{{ formatTime(o.createdAt) }}</div>
          <div>金额：¥ {{ o.amount ?? '--' }}</div>
          <div>
            付款：{{ payLabel(o.paymentMethod) }} / {{ payStatusLabel(o.paymentStatus) }}
            <span v-if="o.paymentExpiresAt && o.paymentStatus !== 'PAID' && o.paymentStatus !== 'EXPIRED'">
              （剩余 {{ timeLeft(o.paymentExpiresAt) }}）
            </span>
          </div>
        </div>
        <div class="items">
          <div v-for="it in o.items" :key="it.dishId + '-' + it.portion" class="item">
            <span class="it-name">{{ it.dishName }}（{{ it.portion === 'LARGE' ? '大份' : '小份' }}）</span>
            <span class="it-qty">x{{ it.quantity }}</span>
            <span class="it-price">¥ {{ it.totalPrice }}</span>
          </div>
        </div>
        <div class="row" style="margin-top:10px;">
          <RouterLink class="receipt" :to="`/receipt/${o.id}?t=${store.tableToken}`">查看/打印小票</RouterLink>
        </div>
      </div>
    </section>
  </div>

  <div v-if="paying.show" class="modal-mask">
    <div class="modal">
      <div class="modal-title">确认付款</div>
      <div class="modal-desc">
        桌号：{{ store.tableNo }}｜人数：{{ store.diners }}｜订单：#{{ paying.orderId ?? '--' }}
      </div>
      <div class="modal-desc" v-if="currentPayOrder">
        支付状态：{{ payStatusLabel(currentPayOrder.paymentStatus) }}｜剩余：{{ timeLeft(currentPayOrder.paymentExpiresAt) }}
      </div>
      <div class="modal-desc">请扫描下方微信收款二维码完成付款</div>
      <div class="pay-qr-box">
        <img :src="wechatPayQr" alt="微信收款码" />
      </div>
      <div class="row">
        <label>支付方式</label>
        <select v-model="paymentMethod">
          <option value="WECHAT">微信支付</option>
          <option value="ALIPAY">支付宝</option>
          <option value="CASH">现金</option>
        </select>
      </div>
      <button class="primary" @click="confirmPay" :disabled="paying.loading">
        {{ paying.loading ? '支付处理中...' : '确认支付(模拟成功)' }}
      </button>
      <button style="margin-top:10px;" @click="failPay" :disabled="paying.loading">模拟失败</button>
      <button style="margin-top:10px;" @click="cancelPay" :disabled="paying.loading">取消</button>
      <div v-if="paying.error" class="error">{{ paying.error }}</div>
      <div class="hint">真实场景请由商家后台核实到账后再确认。</div>
    </div>
  </div>

  <div v-if="payDone.show" class="modal-mask">
    <div class="modal">
      <div class="modal-title">支付成功</div>
      <button class="primary" @click="payDone.show = false">我知道了</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useHaomeiStore } from '../store/haomei.js'
import wechatPayQr from '../assets/wechat-pay.png'

const store = useHaomeiStore()
const error = ref('')
const paymentMethod = ref('WECHAT')
const dinersInput = ref(2)
const route = useRoute()

const needDiners = computed(() => !store.diners)
const paying = ref({ show: false, orderId: null, loading: false, error: '' })
const payDone = ref({ show: false, orderId: null })
const nowTick = ref(Date.now())
const currentPayOrder = computed(() => store.myOrders.find(o => o.id === paying.value.orderId))
let clockTimer = null
let menuSyncTimer = null

function largePrice(p) {
  const n = Number(p)
  if (!Number.isFinite(n)) return '--'
  return (n * 1.5).toFixed(2)
}

function cartTitle(k) {
  const [dishId, portion] = k.split(':')
  const idNum = Number(dishId)
  const name = store.dishes.find(d => d.id === idNum)?.name || `菜品#${dishId}`
  return `${name}（${portion === 'LARGE' ? '大份' : '小份'}）`
}

function canOrderSmall(dish) {
  return !dish?.portionOption || dish.portionOption === 'BOTH' || dish.portionOption === 'SMALL_ONLY'
}

function canOrderLarge(dish) {
  return !dish?.portionOption || dish.portionOption === 'BOTH' || dish.portionOption === 'LARGE_ONLY'
}

function inc(k) {
  const [dishId, portion] = k.split(':')
  store.setCartQty(dishId, portion, (store.cart[k] || 0) + 1)
}
function dec(k) {
  const [dishId, portion] = k.split(':')
  store.setCartQty(dishId, portion, (store.cart[k] || 0) - 1)
}

let searchTimer = null
function debouncedSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    store.fetchDishes().catch(() => {})
  }, 250)
}

function selectCategory(key) {
  store.setSelectedCategory(key)
  store.fetchDishes().catch(() => {})
}

function payLabel(m) {
  if (m === 'WECHAT') return '微信'
  if (m === 'ALIPAY') return '支付宝'
  if (m === 'CASH') return '现金'
  return m || '--'
}

function payStatusLabel(s) {
  if (s === 'UNPAID') return '未支付'
  if (s === 'PAYING') return '支付中'
  if (s === 'PAID') return '已支付'
  if (s === 'FAILED') return '支付失败'
  if (s === 'EXPIRED') return '支付超时'
  return s || '--'
}

function timeLeft(expiresAt) {
  nowTick.value
  if (!expiresAt) return '--'
  const ms = new Date(expiresAt).getTime() - Date.now()
  if (!Number.isFinite(ms)) return '--'
  const s = Math.max(0, Math.floor(ms / 1000))
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

function formatTime(t) {
  try {
    return new Date(t).toLocaleString()
  } catch (e) {
    return t || '--'
  }
}

async function confirmDiners() {
  error.value = ''
  try {
    store.setDiners(dinersInput.value)
    await store.fetchMyOrders()
  } catch (e) {
    error.value = e?.message || '设置失败'
  }
}

onMounted(async () => {
  const token = route.query.t
  if (!token) {
    error.value = '缺少二维码参数：请使用桌台二维码进入'
    return
  }

  try {
    await store.initFromToken(token)
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '桌台二维码无效'
    return
  }

  try {
    store.connectRealtime()
    await store.fetchDishes()
    await store.fetchMyOrders()
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '接口请求失败（请确认后端已启动）'
  }

  clockTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)

  // 兜底同步：即使 WebSocket 异常，用户端也会定时拉取最新菜单
  menuSyncTimer = setInterval(() => {
    store.fetchDishes().catch(() => {})
  }, 2000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (menuSyncTimer) clearInterval(menuSyncTimer)
})

async function pay() {
  error.value = ''
  try {
    const order = await store.createOrderFromCart()
    paying.value = { show: true, orderId: order.id, loading: false, error: '' }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '付款失败'
  }
}

async function confirmPay() {
  paying.value.error = ''
  paying.value.loading = true
  try {
    const started = await store.payOrder(paying.value.orderId, paymentMethod.value)
    const delayMs = 1000 + Math.floor(Math.random() * 2000)
    await new Promise(resolve => setTimeout(resolve, delayMs))
    await store.confirmPay(paying.value.orderId, started.paymentAttemptId, true)
    store.clearCart()
    paying.value.show = false
  } catch (e) {
    paying.value.error = e?.response?.data?.message || e?.message || '支付失败'
  } finally {
    paying.value.loading = false
  }
}

async function failPay() {
  paying.value.error = ''
  paying.value.loading = true
  try {
    const started = await store.payOrder(paying.value.orderId, paymentMethod.value)
    const delayMs = 1000 + Math.floor(Math.random() * 2000)
    await new Promise(resolve => setTimeout(resolve, delayMs))
    await store.confirmPay(paying.value.orderId, started.paymentAttemptId, false)
  } catch (e) {
    paying.value.error = e?.response?.data?.message || e?.message || '模拟失败失败'
  } finally {
    paying.value.loading = false
  }
}

async function cancelPay() {
  paying.value.error = ''
  paying.value.loading = true
  try {
    await store.cancelPay(paying.value.orderId)
    paying.value.show = false
  } catch (e) {
    paying.value.error = e?.response?.data?.message || e?.message || '取消失败'
  } finally {
    paying.value.loading = false
  }
}

watch(
  () => store.myOrders,
  (orders) => {
    if (!paying.value.orderId) return
    const target = (orders || []).find(o => o.id === paying.value.orderId)
    if (!target) return
    if (target.paymentStatus === 'PAID' || target.status === 'COMPLETED') {
      paying.value.show = false
      payDone.value = { show: true, orderId: target.id }
      store.clearCart()
    }
  },
  { deep: true }
)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  min-height: calc(100vh - 100px);
  max-width: 440px;
  margin: 0 auto;
  padding: 12px 10px 22px;
  border-radius: 22px;
  overflow: hidden;
  background-image: url('../assets/haomei-user-china-bg.svg');
  background-size: cover;
  background-position: center;
}
.page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.76) 0%, rgba(255, 255, 255, 0.58) 46%, rgba(255, 255, 255, 0.76) 100%);
}
.page > * {
  position: relative;
  z-index: 1;
}
.panel {
  background: rgba(255, 255, 255, 0.93);
  border: 1px solid rgba(226, 232, 240, 0.95);
  backdrop-filter: blur(6px);
  border-radius: 18px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.1);
  padding: 14px;
}
.sidebar {
  position: static;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  min-width: 0;
}
.tablebox {
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  background: linear-gradient(180deg, #f7faf9 0%, #edf7f1 100%);
  border: 1px solid #dfeee6;
  border-radius: 12px;
  padding: 9px 11px;
}
.table-pill,
.diners-pill {
  box-sizing: border-box;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  max-width: calc(100% - 2px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.table-pill {
  color: #166534;
  background: #dcfce7;
}
.diners-pill {
  color: #1e40af;
  background: #dbeafe;
}
.table-no {
  font-weight: 800;
  font-size: 15px;
  color: #0f172a;
}
.diners {
  color: #475569;
  font-size: 12px;
}
.cat-title {
  width: auto;
  font-weight: 800;
  margin: 0;
  color: #334155;
  font-size: 13px;
}
.cat-scroll {
  display: flex;
  width: 100%;
  max-width: 100%;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 2px;
  scrollbar-width: none;
}
.cat-scroll::-webkit-scrollbar {
  display: none;
}
.cat {
  flex: 0 0 auto;
  margin: 0;
  border-radius: 999px;
  padding: 7px 13px;
  background: #f5f7f6;
  border-color: #e5e7eb;
  font-size: 13px;
  transition: all .15s ease;
}
.cat.active {
  border-color: #07c160;
  background: #e8f9ef;
  color: #05984d;
  font-weight: 800;
}
.main {
  min-height: 200px;
}
.topline {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}
.search {
  width: 100%;
  min-width: 0;
}
.side {
  position: static;
  height: auto;
}
.orders {
  grid-column: auto;
}
.row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  min-width: 0;
}
.row label {
  min-width: 62px;
  font-size: 13px;
  color: #475569;
  flex-shrink: 0;
}
input {
  box-sizing: border-box;
  display: block;
  max-width: 100%;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d4dbe6;
  border-radius: 10px;
  font-size: 14px;
  transition: border-color .15s, box-shadow .15s;
}
input:focus,
select:focus {
  outline: none;
  border-color: #36b37e;
  box-shadow: 0 0 0 3px rgba(7, 193, 96, 0.15);
}
select {
  box-sizing: border-box;
  max-width: 100%;
}
.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}
.dish {
  display: flex;
  gap: 10px;
  border: 1px solid #e5edf0;
  border-radius: 14px;
  padding: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}
.img {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
  background: #f3f4f6;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
.img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder {
  font-size: 12px;
  color: #6b7280;
}
.meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 88px;
  gap: 8px;
}
.name {
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}
.desc {
  color: #64748b;
  font-size: 12px;
  margin-top: 2px;
  min-height: 28px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.price {
  font-weight: 700;
  color: #065f46;
  font-size: 13px;
}
.portion {
  display: flex;
  gap: 8px;
  margin-top: 2px;
}
.portion button {
  flex: 1;
  padding: 7px 8px;
  font-size: 12px;
}
button {
  cursor: pointer;
  border-radius: 10px;
  border: 1px solid #d8dee8;
  padding: 8px 10px;
  background: #fff;
  font-size: 13px;
  transition: all .15s ease;
}
button.primary {
  background: linear-gradient(180deg, #07c160 0%, #059b4d 100%);
  color: #fff;
  border-color: #059b4d;
}
button:active { transform: translateY(1px); }
.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px dashed #e2e8f0;
  padding: 10px 0;
}
.cart-name {
  flex: 1;
  font-size: 13px;
  margin-right: 10px;
}
.cart-qty {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qty {
  width: 28px;
  text-align: center;
  font-weight: 700;
  color: #0f172a;
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.actions button.primary {
  flex: 1 1 100%;
  font-weight: 700;
}
.panel-title {
  margin: 0 0 8px;
  font-size: 17px;
  color: #0f172a;
}
.pay-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.error {
  margin-top: 10px;
  color: #dc2626;
  font-weight: 700;
}
.muted {
  color: #6b7280;
}
.order {
  border: 1px solid #dfe8ea;
  border-radius: 14px;
  padding: 12px;
  margin-top: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fcff 100%);
}
.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.order-top > div:first-child {
  font-weight: 800;
  color: #0f172a;
}
.order-mid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 8px;
}
.status {
  font-weight: 700;
  color: #2563eb;
}
.item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #374151;
  padding: 3px 0;
}
.receipt {
  color: #05984d;
  text-decoration: none;
  font-weight: 700;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.modal {
  width: min(520px, calc(100vw - 24px));
  background: #ffffff;
  border-radius: 18px;
  padding: 18px;
  border: 1px solid #dfe8ea;
  box-shadow: 0 24px 50px rgba(0, 0, 0, 0.24);
}
.modal-title {
  font-weight: 900;
  font-size: 19px;
  color: #0f172a;
}
.modal-desc {
  color: #6b7280;
  margin: 6px 0 12px;
}
.pay-qr-box {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}
.pay-qr-box img {
  width: 230px;
  height: 230px;
  object-fit: contain;
  border-radius: 14px;
  background: #f3f4f6;
  border: 1px solid #e2e8f0;
}

@media (max-width: 420px) {
  .page {
    padding: 10px 8px 18px;
  }
  .panel {
    padding: 12px;
    border-radius: 14px;
  }
  .pay-qr-box img {
    width: 200px;
    height: 200px;
  }
  .tablebox {
    flex-wrap: wrap;
    gap: 6px;
  }
  .portion {
    flex-direction: column;
  }
}
</style>

