<template>
  <div class="wrap">
    <div class="toolbar no-print">
      <button class="primary" @click="doPrint">打印小票</button>
      <RouterLink :to="backUrl">返回点餐</RouterLink>
      <div v-if="error" class="error">{{ error }}</div>
    </div>

    <div class="ticket" v-if="order">
      <div class="title">好美味 · 订单小票</div>
      <div class="line">订单号：#{{ order.id }}</div>
      <div class="line">桌号：{{ order.tableNo }}　人数：{{ order.diners }}</div>
      <div class="line">时间：{{ fmt(order.createdAt) }}</div>
      <div class="line">支付：{{ order.paymentMethod }} / {{ order.paymentStatus }}</div>
      <div class="line" v-if="order.remark">备注：{{ order.remark }}</div>

      <div class="hr"></div>
      <div class="items">
        <div class="it head">
          <span>菜品</span><span>数量</span><span>金额</span>
        </div>
        <div v-for="it in order.items" :key="it.dishId + '-' + it.portion" class="it">
          <span>{{ it.dishName }}（{{ it.portion === 'LARGE' ? '大份' : '小份' }}）</span>
          <span>x{{ it.quantity }}</span>
          <span>¥ {{ it.totalPrice }}</span>
        </div>
      </div>
      <div class="hr"></div>
      <div class="total">合计：¥ {{ order.amount }}</div>
      <div class="footer">谢谢惠顾，欢迎再来！</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../services/api.js'

const route = useRoute()
const order = ref(null)
const error = ref('')

const backUrl = computed(() => {
  const t = route.query.t
  return t ? `/?t=${t}` : '/'
})

function fmt(t) {
  try { return new Date(t).toLocaleString() } catch { return t || '--' }
}

function doPrint() {
  window.print()
}

onMounted(async () => {
  error.value = ''
  try {
    const t = route.query.t
    if (!t) throw new Error('缺少桌台token，无法查看小票')
    const { data } = await api.get(`/api/orders/${route.params.orderId}/receipt`, { params: { tableToken: t } })
    order.value = data
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || '加载小票失败'
  }
})
</script>

<style scoped>
.wrap { padding: 16px; }
.toolbar { display:flex; gap:12px; align-items:center; margin-bottom:14px; }
.ticket {
  width: min(420px, 100%);
  border: 1px dashed #111827;
  padding: 14px;
  background: #fff;
}
.title { font-weight: 900; font-size: 18px; text-align:center; margin-bottom:10px; }
.line { font-size: 13px; margin: 4px 0; }
.hr { border-top: 1px dashed #9ca3af; margin: 12px 0; }
.it { display:flex; justify-content:space-between; font-size: 13px; padding: 3px 0; }
.it.head { font-weight: 800; }
.total { font-weight: 900; text-align:right; margin-top: 6px; }
.footer { text-align:center; color:#6b7280; margin-top: 12px; font-size:12px; }
.error { color:#dc2626; font-weight:800; }
button { cursor:pointer; border-radius:8px; border:1px solid #e5e7eb; padding:9px 10px; background:#fff; }
button.primary { background:#2563eb; color:#fff; border-color:#2563eb; }

@media print {
  .no-print { display:none !important; }
  .wrap { padding:0; }
  .ticket { border:none; width: 80mm; }
}
</style>

