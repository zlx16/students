import { createRouter, createWebHistory } from 'vue-router'
import UserMenuPage from '../pages/UserMenuPage.vue'
import AdminPage from '../pages/AdminPage.vue'
import ReceiptPage from '../pages/ReceiptPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'user', component: UserMenuPage, meta: { title: '欢迎来到好美味' } },
    { path: '/admin', name: 'admin', component: AdminPage, meta: { title: '好美味后台管理系统' } },
    { path: '/receipt/:orderId', name: 'receipt', component: ReceiptPage, meta: { title: '好美味 · 订单小票' } }
  ]
})

router.afterEach((to) => {
  document.title = to.meta.title || '欢迎来到好美味'
})

export default router

