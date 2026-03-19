import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router/index.js'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia())
app.use(router)

app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue Error]', info, err)
  const el = document.getElementById('app')
  if (el && !el.querySelector('.app-error')) {
    el.innerHTML = `<div class="app-error" style="padding:20px;color:#dc2626;font-weight:700;">页面出错：${err?.message || String(err)}</div>`
  }
}

app.mount('#app')

