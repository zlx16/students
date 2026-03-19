<template>
  <div class="page" :class="`theme-${adminTheme}`">
    <!-- 登录 -->
    <div class="login-wrap" v-if="!isAuthed">
      <section class="panel login-panel">
        <div class="login-brand">
          <img class="login-brand-logo" :src="logoUrl" alt="好美味 logo" />
          <div class="login-brand-text">好美味</div>
        </div>
        <h2>管理员登录</h2>
        <div class="row">
          <label>用户名</label>
          <input v-model="login.username" placeholder="admin" />
        </div>
        <div class="row">
          <label>密码</label>
          <input v-model="login.password" type="password" placeholder="admin123" />
        </div>
        <button class="primary" @click="doLogin">登录</button>
        <div v-if="error" class="error">{{ error }}</div>
        <div class="hint">默认演示账号可在后端 application.yml 修改</div>
      </section>
    </div>

    <!-- 已登录 -->
    <div v-else>
      <div class="admin-head">
        <button class="theme-toggle" @click="toggleTheme">{{ themeButtonText }}</button>
        <RouterLink to="/" class="switch-link">切换到用户端</RouterLink>
        <button class="danger" @click="logout">退出登录</button>
      </div>

      <!-- 10桌状态总览 -->
      <section class="panel" style="margin-bottom:12px;">
        <h2>桌台状态</h2>
        <div class="status-overview">
          <div v-for="t in tables" :key="'s-'+t.tableNo"
               class="status-card" :class="{ selected: focusTable && focusTable.tableNo === t.tableNo }"
               @click="selectTable(t)">
            <div class="s-no">桌号 {{ t.tableNo }}</div>
            <div class="badge" :class="{ busy: t.inUse, idle: !t.inUse }">{{ t.inUse ? '正在用餐' : '空闲' }}</div>
            <button v-if="t.inUse" class="reset-btn" @click.stop="resetTable(t)">重置</button>
          </div>
        </div>
      </section>

      <!-- 选中桌号详情 -->
      <div class="grid" v-if="focusTable">
        <!-- 左：该桌订单 + 点单 -->
        <section class="panel">
          <h2>桌号 {{ focusTable.tableNo }} — 订单</h2>
          <div v-if="tableOrders.length === 0" class="muted" style="margin:10px 0;">暂无订单</div>
          <div v-for="o in tableOrders" :key="o.id" class="order">
            <div class="order-top">
              <div>订单 #{{ o.id }}</div>
              <div class="right">{{ payStatusLabel(o.paymentStatus) }}</div>
            </div>
            <div class="meta-row">
              <span>时间：{{ formatTime(o.createdAt) }}</span>
              <span>金额：¥ {{ o.amount ?? '--' }}</span>
              <span>状态：{{ o.status }}</span>
            </div>
            <div class="items">
              <div v-for="it in o.items" :key="it.dishId+'-'+it.portion" class="item">
                <span>{{ it.dishName }}（{{ it.portion === 'LARGE' ? '大份' : '小份' }}）</span>
                <span>x{{ it.quantity }}</span>
                <span>¥ {{ it.totalPrice }}</span>
              </div>
            </div>
            <div class="status-row">
              <label>操作</label>
              <button v-if="o.status !== 'COMPLETED' && o.status !== 'CANCELLED'" class="primary small-btn" @click="updateOrderStatus(o.id, 'COMPLETED')">完成</button>
              <button v-if="o.status !== 'COMPLETED' && o.status !== 'CANCELLED'" class="danger small-btn" @click="updateOrderStatus(o.id, 'CANCELLED')">取消</button>
              <span v-if="o.status === 'COMPLETED'" class="muted">已完成</span>
              <span v-if="o.status === 'CANCELLED'" class="muted">已取消</span>
            </div>
          </div>

          <h3 style="margin-top:16px;">为该桌点单</h3>
          <div class="order-dish-grid">
            <div v-for="d in store.adminDishes" :key="'od-'+d.id" class="order-dish-item">
              <span class="od-name">{{ d.name }}</span>
              <span class="od-price">¥{{ d.price }}</span>
              <button v-if="canOrderSmall(d)" @click="addToAdminCart(d.id, 'SMALL')">小份+</button>
              <button v-if="canOrderLarge(d)" @click="addToAdminCart(d.id, 'LARGE')">大份+</button>
            </div>
          </div>
          <div v-if="Object.keys(adminCart).length" class="admin-cart">
            <div class="cart-title">当前购物车</div>
            <div v-for="k in Object.keys(adminCart)" :key="k" class="cart-row">
              <span>{{ adminCartLabel(k) }}</span>
              <span>x{{ adminCart[k] }}</span>
              <button @click="removeFromAdminCart(k)">-</button>
            </div>
            <div class="row" style="margin-top:8px;">
              <label>人数</label>
              <input v-model.number="adminDiners" type="number" min="1" placeholder="1" style="width:80px;" />
            </div>
            <button class="primary" @click="submitAdminOrder" :disabled="adminOrdering" style="margin-top:8px;">
              {{ adminOrdering ? '提交中...' : '提交订单' }}
            </button>
          </div>
          <div v-if="adminOrderMsg" class="success">{{ adminOrderMsg }}</div>
          <div v-if="adminOrderError" class="error">{{ adminOrderError }}</div>
        </section>

        <!-- 右：二维码 -->
        <section class="panel">
          <h2>桌号 {{ focusTable.tableNo }} 二维码</h2>
          <div class="t-qr">
            <img v-if="focusTable.qrDataUrl" :src="focusTable.qrDataUrl" alt="qr" />
            <div v-else class="placeholder">生成中...</div>
          </div>
          <div class="t-link">{{ focusTable.url }}</div>
        </section>
      </div>

      <!-- 菜品管理 -->
      <section class="panel" style="margin-top:12px;">
        <div class="section-head">
          <h2>菜品管理</h2>
          <button class="primary" @click="showCreateModal = true">添加新菜品</button>
        </div>
        <div class="list">
          <div v-for="d in store.adminDishes" :key="d.id" class="dish-row">
            <div class="dish-head">
              <div class="dish-img-sm">
                <img v-if="d.imageUrl" :src="d.imageUrl" alt="" />
                <div v-else class="placeholder">无图</div>
              </div>
              <div class="dish-info">
                <div class="dish-title">{{ d.name }}</div>
                <div class="dish-meta">¥{{ d.price }}　{{ categoryLabel(d.category) }}　{{ d.available ? '可用' : '停用' }}</div>
              </div>
              <div class="dish-btns">
                <button @click="toggleEditDish(d.id)">{{ editingDishId === d.id ? '收起' : '编辑' }}</button>
                <button class="danger" @click="removeDish(d.id)">删除</button>
              </div>
            </div>
            <div v-if="editingDishId === d.id && getEdit(d)" class="dish-edit-area">
              <div class="row"><label>描述</label><input v-model="getEdit(d).description" /></div>
              <div class="row"><label>价格</label><input v-model.number="getEdit(d).price" type="number" step="0.01" /></div>
              <div class="row">
                <label>可用</label>
                <select v-model="getEdit(d).available"><option :value="true">是</option><option :value="false">否</option></select>
              </div>
              <div class="row">
                <label>分类</label>
                <select v-model="getEdit(d).category">
                  <option value="RECOMMEND">推荐</option><option value="HOT">热菜</option>
                  <option value="STAPLE">主食</option><option value="DESSERT">甜品</option>
                  <option value="DRINK">饮品</option>
                </select>
              </div>
              <div class="row">
                <label>规格</label>
                <select v-model="getEdit(d).portionOption">
                  <option value="BOTH">大份/小份</option>
                  <option value="SMALL_ONLY">仅小份</option>
                  <option value="LARGE_ONLY">仅大份</option>
                </select>
              </div>
              <div class="row">
                <label>上传图片</label>
                <input type="file" accept="image/*" @change="onPickDishFile(d.id, $event)" />
              </div>
              <div class="actions">
                <button class="primary" @click="uploadDishImage(d.id)">上传图片</button>
                <button class="primary" @click="saveDish(d.id)">保存</button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="error" class="error">{{ error }}</div>
      </section>
    </div>

    <!-- 新增菜品弹窗 -->
    <div v-if="showCreateModal" class="modal-mask" @click.self="showCreateModal = false">
      <div class="modal">
        <div class="modal-title">添加新菜品</div>
        <div class="row"><label>名称</label><input v-model="create.name" /></div>
        <div class="row"><label>描述</label><input v-model="create.description" /></div>
        <div class="row"><label>价格</label><input v-model.number="create.price" type="number" step="0.01" /></div>
        <div class="row">
          <label>规格</label>
          <select v-model="create.portionOption">
            <option value="BOTH">大份/小份</option>
            <option value="SMALL_ONLY">仅小份</option>
            <option value="LARGE_ONLY">仅大份</option>
          </select>
        </div>
        <div class="row">
          <label>可用</label>
          <select v-model="create.available"><option :value="true">是</option><option :value="false">否</option></select>
        </div>
        <div class="row">
          <label>分类</label>
          <select v-model="create.category">
            <option value="RECOMMEND">推荐</option><option value="HOT">热菜</option>
            <option value="STAPLE">主食</option><option value="DESSERT">甜品</option>
            <option value="DRINK">饮品</option>
          </select>
        </div>
        <button class="primary" @click="createDish" :disabled="creating" style="width:100%;">
          {{ creating ? '创建中...' : '确认创建' }}
        </button>
        <div v-if="createMsg" class="success">{{ createMsg }}</div>
        <div v-if="createError" class="error">{{ createError }}</div>
      </div>
    </div>

    <!-- 付款二维码弹窗 -->
    <div v-if="payQr.show" class="modal-mask" @click.self="payQr.show = false">
      <div class="modal" style="text-align:center;">
        <div class="modal-title">扫码付款</div>
        <div class="muted">订单 #{{ payQr.orderId }}　金额 ¥{{ payQr.amount }}</div>
        <div class="t-qr" style="margin:12px auto;">
          <img v-if="payQr.qrDataUrl" :src="payQr.qrDataUrl" alt="pay-qr" />
          <div v-else class="placeholder">生成中...</div>
        </div>
        <div class="muted small">顾客扫码后可在手机上完成支付</div>
        <button style="margin-top:12px;" @click="payQr.show = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useHaomeiStore } from '../store/haomei.js'
import { api } from '../services/api.js'
import logoUrl from '../assets/haomei-logo.svg'

const store = useHaomeiStore()
const login = reactive({ username: '', password: '' })
const error = ref('')
const adminTheme = ref(localStorage.getItem('haomei_admin_theme') || 'light')
const themeButtonText = computed(() => adminTheme.value === 'guochao' ? '切换清爽主题' : '切换国潮主题')

const adminToken = ref(localStorage.getItem('haomei_admin_token') || '')
const isAuthed = computed(() => !!adminToken.value)

const tables = ref([])
const focusTable = ref(null)
const tableOrders = ref([])

const dishFiles = reactive({})
const edit = reactive({})
const editingDishId = ref(null)

const showCreateModal = ref(false)
const create = reactive({
  name: '',
  description: '',
  price: 10.00,
  portionOption: 'BOTH',
  available: true,
  category: 'RECOMMEND'
})
const creating = ref(false)
const createMsg = ref('')
const createError = ref('')

const adminCart = reactive({})
const adminDiners = ref(1)
const adminOrdering = ref(false)
const adminOrderMsg = ref('')
const adminOrderError = ref('')

const payQr = reactive({ show: false, orderId: null, amount: null, qrDataUrl: '' })

function ensureEdit(d) {
  if (!edit[d.id]) {
    edit[d.id] = {
      description: d.description || '',
      price: d.price ?? 0,
      available: d.available ?? true,
      category: d.category || 'RECOMMEND',
      portionOption: d.portionOption || 'BOTH'
    }
  }
}

function getEdit(d) {
  ensureEdit(d)
  return edit[d.id]
}

function syncEditCache() {
  for (const d of store.adminDishes) ensureEdit(d)
}

async function doLogin() {
  error.value = ''
  try {
    const { data } = await api.post('/api/admin/login', login)
    localStorage.setItem('haomei_admin_token', data.token)
    adminToken.value = data.token
    await store.fetchAdminDishes()
    await store.fetchAdminOrders()
    await fetchTables()
    store.connectRealtime()
  } catch (e) {
    const status = e?.response?.status
    if (status === 401) error.value = '用户名或密码错误'
    else if (e?.response?.data?.message) error.value = e.response.data.message
    else if (e?.code === 'ERR_NETWORK' || !e?.response) error.value = '无法连接后端，请确认已启动（端口 8081）'
    else error.value = '登录失败'
  }
}

function logout() {
  localStorage.removeItem('haomei_admin_token')
  adminToken.value = ''
  store.adminDishes = []
  store.adminOrders = []
  tables.value = []
  focusTable.value = null
  tableOrders.value = []
  error.value = ''
}

function toggleTheme() {
  adminTheme.value = adminTheme.value === 'guochao' ? 'light' : 'guochao'
  localStorage.setItem('haomei_admin_theme', adminTheme.value)
}

function baseUrl() {
  return window.location.origin.replace(/\/$/, '')
}

async function fetchTables() {
  try {
    const { data } = await api.get('/api/admin/tables', { params: { baseUrl: baseUrl() } })
    const list = (data || []).map(t => ({
      tableNo: t.tableNo,
      token: t.token,
      url: t.url || `${baseUrl()}/?t=${t.token}`,
      qrDataUrl: t.qrDataUrl || '',
      inUse: !!t.inUse,
      activeOrderCount: t.activeOrderCount ?? 0,
      statusLabel: t.statusLabel || (t.inUse ? '正在用餐' : '空闲')
    }))
    tables.value = list

    if (focusTable.value) {
      const updated = tables.value.find(t => t.tableNo === focusTable.value.tableNo)
      if (updated) focusTable.value = updated
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载桌台失败'
  }
}

async function selectTable(t) {
  focusTable.value = t
  adminOrderMsg.value = ''
  adminOrderError.value = ''
  try {
    const { data } = await api.get(`/api/admin/orders/table/${t.tableNo}`)
    tableOrders.value = (data || []).filter(o => ['WAIT_PAY','PENDING','COOKING'].includes(o.status))
  } catch (e) {
    tableOrders.value = []
  }
}

function toggleEditDish(id) {
  editingDishId.value = editingDishId.value === id ? null : id
}

function categoryLabel(c) {
  const map = { RECOMMEND: '推荐', HOT: '热菜', STAPLE: '主食', DESSERT: '甜品', DRINK: '饮品' }
  return map[c] || c || '--'
}

function canOrderSmall(d) {
  return !d?.portionOption || d.portionOption === 'BOTH' || d.portionOption === 'SMALL_ONLY'
}

function canOrderLarge(d) {
  return !d?.portionOption || d.portionOption === 'BOTH' || d.portionOption === 'LARGE_ONLY'
}

async function resetTable(t) {
  try {
    const { data: orders } = await api.get(`/api/admin/orders/table/${t.tableNo}`)
    const active = (orders || []).filter(o => ['WAIT_PAY','PENDING','COOKING'].includes(o.status))
    for (const o of active) {
      await api.patch(`/api/admin/orders/${o.id}/status`, { status: 'COMPLETED' })
    }
    await fetchTables()
    if (focusTable.value && focusTable.value.tableNo === t.tableNo) {
      tableOrders.value = []
      focusTable.value = null
    }
  } catch (e) {
    error.value = e?.response?.data?.message || '重置失败'
  }
}

async function createDish() {
  createError.value = ''
  createMsg.value = ''
  if (!create.name.trim()) { createError.value = '名称不能为空'; return }
  if (!create.description.trim()) { createError.value = '描述不能为空'; return }
  if (!Number.isFinite(create.price) || create.price <= 0) { createError.value = '价格必须大于0'; return }
  if (store.adminDishes.some(d => d.name === create.name.trim())) {
    createError.value = `「${create.name.trim()}」已存在`; return
  }
  creating.value = true
  try {
    await api.post('/api/admin/dishes', {
      name: create.name.trim(), description: create.description.trim(),
      price: Number(create.price), available: !!create.available, category: create.category, portionOption: create.portionOption
    })
    await store.fetchAdminDishes()
    createMsg.value = `「${create.name.trim()}」创建成功`
    create.name = ''; create.description = ''; create.price = 10; create.portionOption = 'BOTH'; create.available = true; create.category = 'RECOMMEND'
    setTimeout(() => { showCreateModal.value = false; createMsg.value = '' }, 1200)
  } catch (e) {
    createError.value = e?.response?.data?.message || e?.message || '创建失败'
  } finally { creating.value = false }
}

async function saveDish(dishId) {
  error.value = ''
  const d = store.adminDishes.find(x => x.id === dishId)
  if (!d) return
  try {
    await api.put(`/api/admin/dishes/${dishId}`, {
      name: d.name, description: edit[dishId].description,
      price: Number(edit[dishId].price),
      available: !!edit[dishId].available,
      category: edit[dishId].category,
      portionOption: edit[dishId].portionOption || 'BOTH'
    })
    await store.fetchAdminDishes()
    editingDishId.value = null
  } catch (e) { error.value = e?.response?.data?.message || '更新失败' }
}

function onPickDishFile(dishId, ev) {
  const f = ev?.target?.files?.[0]
  if (f) dishFiles[dishId] = f
}

async function uploadDishImage(dishId) {
  error.value = ''
  const f = dishFiles[dishId]
  if (!f) { error.value = '请选择图片文件'; return }
  try {
    const fd = new FormData(); fd.append('file', f)
    await api.post(`/api/admin/dishes/${dishId}/image`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    dishFiles[dishId] = null
    await store.fetchAdminDishes()
  } catch (e) { error.value = e?.response?.data?.message || '上传失败' }
}

async function removeDish(dishId) {
  error.value = ''
  try {
    await api.delete(`/api/admin/dishes/${dishId}`)
    await store.fetchAdminDishes()
    delete edit[dishId]
  } catch (e) { error.value = e?.response?.data?.message || '删除失败' }
}

async function updateOrderStatus(orderId, status) {
  try {
    await api.patch(`/api/admin/orders/${orderId}/status`, { status })
    await fetchTables()
    if (focusTable.value) {
      const updated = tables.value.find(t => t.tableNo === focusTable.value.tableNo)
      if (updated && !updated.inUse) {
        tableOrders.value = []
        focusTable.value = null
      } else {
        await selectTable(focusTable.value)
      }
    }
  } catch (e) { error.value = e?.response?.data?.message || '更新订单失败' }
}

// 后台点单
function addToAdminCart(dishId, portion) {
  const k = `${dishId}:${portion}`
  adminCart[k] = (adminCart[k] || 0) + 1
}

function removeFromAdminCart(k) {
  if (adminCart[k] > 1) adminCart[k]--
  else delete adminCart[k]
}

function adminCartLabel(k) {
  const [dishId, portion] = k.split(':')
  const name = store.adminDishes.find(d => d.id === Number(dishId))?.name || `#${dishId}`
  return `${name}（${portion === 'LARGE' ? '大份' : '小份'}）`
}

async function submitAdminOrder() {
  adminOrderError.value = ''
  adminOrderMsg.value = ''
  if (!focusTable.value) return
  const items = Object.entries(adminCart).map(([k, qty]) => {
    const [dishId, portion] = k.split(':')
    return { dishId: Number(dishId), portion, quantity: qty }
  })
  if (!items.length) { adminOrderError.value = '请先选择菜品'; return }
  if (!adminDiners.value || adminDiners.value < 1) { adminOrderError.value = '请填写人数'; return }

  adminOrdering.value = true
  try {
    const { data: order } = await api.post('/api/orders', {
      tableToken: focusTable.value.token,
      diners: adminDiners.value,
      customerName: '后台点单',
      remark: '',
      items
    })
    Object.keys(adminCart).forEach(k => delete adminCart[k])
    adminOrderMsg.value = `订单 #${order.id} 已创建（¥${order.amount}）`

    // 生成付款二维码
    const payUrl = `${baseUrl()}/receipt/${order.id}?t=${focusTable.value.token}`
    try {
      const QRCode = (await import('qrcode')).default
      payQr.qrDataUrl = await QRCode.toDataURL(payUrl, { width: 260, margin: 1 })
    } catch { payQr.qrDataUrl = '' }
    payQr.orderId = order.id
    payQr.amount = order.amount
    payQr.show = true

    await fetchTables()
    await selectTable(focusTable.value)
    setTimeout(() => { adminOrderMsg.value = '' }, 3000)
  } catch (e) {
    adminOrderError.value = e?.response?.data?.message || e?.message || '点单失败'
  } finally { adminOrdering.value = false }
}

function payStatusLabel(s) {
  const map = { UNPAID: '未支付', PAYING: '支付中', PAID: '已支付', FAILED: '支付失败', EXPIRED: '支付超时' }
  return map[s] || s || '--'
}

function formatTime(t) {
  try { return new Date(t).toLocaleString() } catch { return t || '--' }
}

onMounted(async () => {
  try {
    store.connectRealtime()
    if (isAuthed.value) {
      await store.fetchAdminDishes()
      await store.fetchAdminOrders()
      await fetchTables()
    }
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载失败'
  }
})

watch(() => store.adminDishes, () => syncEditCache(), { deep: true, immediate: true })

let orderRefreshDebounce = null
watch(
  () => store.adminOrders,
  () => {
    if (!isAuthed.value) return
    if (orderRefreshDebounce) clearTimeout(orderRefreshDebounce)
    orderRefreshDebounce = setTimeout(async () => {
      await fetchTables()
      if (focusTable.value) {
        const updated = tables.value.find(t => t.tableNo === focusTable.value.tableNo)
        if (updated && !updated.inUse) {
          tableOrders.value = []
          focusTable.value = null
        } else if (updated) {
          await selectTable(updated)
        }
      }
    }, 300)
  },
  { deep: true }
)

// 定时刷新桌台状态
let tableRefreshTimer = null
onMounted(() => {
  tableRefreshTimer = setInterval(() => {
    if (isAuthed.value) fetchTables().catch(() => {})
  }, 5000)
})
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (tableRefreshTimer) clearInterval(tableRefreshTimer)
  if (orderRefreshDebounce) clearTimeout(orderRefreshDebounce)
})
</script>

<style scoped>
.page {
  padding: 16px;
  background: radial-gradient(circle at top, #f8fafc 0%, #eef2ff 42%, #e2e8f0 100%);
  border-radius: 14px;
}

.login-wrap {
  min-height: calc(100vh - 120px);
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  background-image: url('../assets/haomei-admin-guochao-bg.svg');
  background-size: cover; background-position: center; border-radius: 14px;
}
.login-wrap::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(125deg, rgba(8,8,8,0.62) 0%, rgba(20,10,12,0.38) 44%, rgba(8,8,8,0.62) 100%);
}
.login-panel {
  width: min(520px, 100%); position: relative; z-index: 1;
  background: rgba(23,13,15,0.76); border: 1px solid rgba(224,188,116,0.5);
  box-shadow: 0 18px 45px rgba(0,0,0,0.45), inset 0 1px 0 rgba(248,216,148,0.24);
  backdrop-filter: blur(8px);
}
.login-panel h2, .login-panel label, .login-panel .hint { color: #f3dfb0; }
.login-brand { display: flex; align-items: center; justify-content: center; gap: 10px; margin-bottom: 8px; }
.login-brand-logo { width: 40px; height: 40px; border-radius: 10px; box-shadow: 0 6px 18px rgba(0,0,0,0.35); }
.login-brand-text { color: #f3dfb0; font-weight: 700; letter-spacing: 2px; }
.login-panel input { background: rgba(255,255,255,0.9); }
.login-panel button.primary {
  background: linear-gradient(180deg, #d1a14f 0%, #b88432 100%);
  border-color: #b88432; color: #1c1208; font-weight: 700;
}

.panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(4px);
}
.grid { display: grid; grid-template-columns: 1fr 300px; gap: 14px; align-items: start; }
.admin-head { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-bottom: 10px; }
.theme-toggle {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
  border-color: #d97706;
  color: #fff;
  font-weight: 700;
}
.switch-link {
  color: #1d4ed8; text-decoration: none; font-size: 14px; font-weight: 700;
  padding: 9px 15px; border: 1px solid #dbeafe; border-radius: 10px; background: #eff6ff;
}
.switch-link:hover { background: #dbeafe; }

.status-overview { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; margin-top: 10px; }
.status-card {
  border: 1px solid #e2e8f0; border-radius: 12px; padding: 10px 12px;
  cursor: pointer; transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}
.status-card:hover { border-color: #2563eb; transform: translateY(-1px); }
.status-card.selected { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.18); }
.s-no { font-weight: 800; font-size: 13px; color: #0f172a; }
.badge { display: inline-block; margin-top: 4px; padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.badge.busy { color: #b91c1c; background: #fee2e2; }
.badge.idle { color: #065f46; background: #d1fae5; }
.small { font-size: 12px; margin-top: 4px; }

.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }

.order {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  margin-top: 10px;
  background: #fff;
}
.order-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; font-weight: 700; }
.meta-row { display: flex; gap: 14px; color: #64748b; font-size: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.status-row { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.items { display: grid; gap: 4px; }
.item { display: flex; justify-content: space-between; font-size: 13px; color: #374151; padding: 2px 0; }

.order-dish-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; margin-top: 8px; }
.order-dish-item {
  display: flex; align-items: center; gap: 6px; font-size: 13px;
  border: 1px solid #e2e8f0; border-radius: 10px; padding: 8px 10px; background: #fff;
}
.od-name { flex: 1; font-weight: 600; }
.od-price { color: #6b7280; font-size: 12px; }
.admin-cart { border: 1px solid #c7d2fe; border-radius: 12px; padding: 10px; margin-top: 10px; background: #f8faff; }
.cart-title { font-weight: 700; margin-bottom: 6px; }
.cart-row { display: flex; justify-content: space-between; align-items: center; font-size: 13px; padding: 3px 0; }

.t-qr {
  width: 220px; height: 220px; background: #f3f4f6; border-radius: 10px;
  overflow: hidden; display: flex; align-items: center; justify-content: center;
}
.t-qr img { width: 100%; height: 100%; object-fit: cover; }
.t-link { margin-top: 8px; color: #6b7280; font-size: 12px; word-break: break-all; }

.row { display: flex; align-items: center; gap: 10px; margin: 10px 0; }
label { min-width: 60px; color: #334155; font-size: 13px; font-weight: 600; }
input, select {
  width: 100%;
  padding: 9px 11px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-size: 14px;
  transition: border-color .15s, box-shadow .15s;
  background: #fff;
}
input:focus, select:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
}
button {
  cursor: pointer;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 8px 10px;
  background: #fff;
  font-size: 13px;
  transition: all .15s ease;
}
button:hover { transform: translateY(-1px); }
button.primary {
  background: linear-gradient(180deg, #4f46e5 0%, #3730a3 100%);
  color: #fff;
  border-color: #3730a3;
}
button.danger { border-color: #ef4444; color: #ef4444; }
.error { margin-top: 10px; color: #dc2626; font-weight: 700; }
.success { margin-top: 10px; color: #059669; font-weight: 700; }
.hint { margin-top: 10px; color: #6b7280; font-size: 12px; }
.muted { color: #6b7280; }
.right { color: #6b7280; font-size: 12px; }
.placeholder { font-size: 12px; color: #6b7280; }

.list { display: grid; gap: 8px; }
.dish-row { border: 1px solid #f3f4f6; border-radius: 10px; padding: 10px; }
.dish-head { display: flex; align-items: center; gap: 10px; }
.dish-img-sm {
  width: 48px; height: 48px; overflow: hidden; border-radius: 8px; flex: none;
  background: #f3f4f6; display: flex; align-items: center; justify-content: center;
}
.dish-img-sm img { width: 100%; height: 100%; object-fit: cover; }
.dish-info { flex: 1; }
.dish-title { font-weight: 700; font-size: 14px; }
.dish-meta { color: #6b7280; font-size: 12px; margin-top: 2px; }
.dish-btns { display: flex; gap: 6px; flex: none; }
.dish-edit-area { border-top: 1px solid #f3f4f6; margin-top: 10px; padding-top: 10px; }
.actions { display: flex; gap: 10px; }
.actions button.primary { flex: 1; }
.actions button.danger { flex: 1; }
.reset-btn {
  margin-top: 4px; padding: 2px 8px; font-size: 11px;
  border: 1px solid #f87171; color: #ef4444; border-radius: 6px; background: #fff; cursor: pointer;
}
.reset-btn:hover { background: #fef2f2; }
.small-btn { padding: 4px 10px; font-size: 12px; }

.modal-mask {
  position: fixed; inset: 0; background: rgba(17,24,39,0.55);
  display: flex; align-items: center; justify-content: center; z-index: 50;
}
.modal {
  width: min(520px, calc(100vw - 24px)); background: #fff; border-radius: 16px;
  padding: 18px; border: 1px solid #e2e8f0; box-shadow: 0 24px 50px rgba(15, 23, 42, 0.26);
}
.modal-title { font-weight: 900; font-size: 19px; margin-bottom: 10px; color: #0f172a; }

/* 国潮深色主题（登录后可切换） */
.page.theme-guochao {
  background:
    radial-gradient(circle at top, #2a1113 0%, #14090b 42%, #090609 100%),
    repeating-linear-gradient(
      45deg,
      rgba(193, 146, 83, 0.05) 0px,
      rgba(193, 146, 83, 0.05) 2px,
      transparent 2px,
      transparent 12px
    );
}
.page.theme-guochao .panel {
  background: rgba(31, 16, 19, 0.9);
  border-color: rgba(176, 124, 72, 0.45);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.35);
}
.page.theme-guochao .panel h2,
.page.theme-guochao .panel h3 {
  position: relative;
  padding-bottom: 8px;
  margin-bottom: 10px;
}
.page.theme-guochao .panel h2::after,
.page.theme-guochao .panel h3::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 56px;
  height: 2px;
  background: linear-gradient(90deg, #f6d28d 0%, rgba(246, 210, 141, 0.1) 100%);
}
.page.theme-guochao h2,
.page.theme-guochao h3,
.page.theme-guochao .s-no,
.page.theme-guochao .dish-title,
.page.theme-guochao .cart-title {
  color: #f6e2b4;
}
.page.theme-guochao .muted,
.page.theme-guochao .right,
.page.theme-guochao .meta-row,
.page.theme-guochao .dish-meta,
.page.theme-guochao .t-link {
  color: #d8bf95;
}
.page.theme-guochao .order,
.page.theme-guochao .order-dish-item,
.page.theme-guochao .dish-row,
.page.theme-guochao .admin-cart {
  background: rgba(28, 15, 17, 0.88);
  border-color: rgba(176, 124, 72, 0.35);
}
.page.theme-guochao input,
.page.theme-guochao select {
  background: rgba(250, 240, 225, 0.98);
  border-color: rgba(176, 124, 72, 0.55);
}
.page.theme-guochao .switch-link {
  color: #f6e2b4;
  background: rgba(90, 45, 35, 0.42);
  border-color: rgba(176, 124, 72, 0.55);
}
.page.theme-guochao .switch-link:hover {
  background: rgba(120, 66, 50, 0.5);
}
.page.theme-guochao .theme-toggle {
  background: linear-gradient(180deg, #c58d3a 0%, #9a6421 100%);
  border-color: #9a6421;
}
.page.theme-guochao button.primary {
  background: linear-gradient(180deg, #d19a42 0%, #9e6623 100%);
  border-color: #9e6623;
  color: #fff7e6;
}
.page.theme-guochao button.danger {
  color: #fca5a5;
  border-color: #ef4444;
  background: rgba(127, 29, 29, 0.2);
}
.page.theme-guochao .status-card {
  background: linear-gradient(180deg, rgba(47, 24, 24, 0.95) 0%, rgba(30, 16, 18, 0.95) 100%);
  border-color: rgba(176, 124, 72, 0.45);
}
.page.theme-guochao .status-card.selected {
  box-shadow: 0 0 0 2px rgba(246, 210, 141, 0.25), 0 8px 18px rgba(0, 0, 0, 0.25);
}
.page.theme-guochao .badge.busy {
  color: #fecaca;
  background: rgba(127, 29, 29, 0.45);
}
.page.theme-guochao .badge.idle {
  color: #bbf7d0;
  background: rgba(20, 83, 45, 0.45);
}
.page.theme-guochao .modal {
  background: rgba(34, 18, 20, 0.96);
  border-color: rgba(176, 124, 72, 0.55);
}
.page.theme-guochao .modal-title {
  color: #f6e2b4;
}

@media (max-width: 1100px) {
  .grid { grid-template-columns: 1fr; }
  .status-overview { grid-template-columns: repeat(2, 1fr); }
  .order-dish-grid { grid-template-columns: 1fr; }
}
</style>
