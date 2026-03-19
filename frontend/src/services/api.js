import axios from 'axios'

// 开发时留空，请求发往当前域名(5173)，由 Vite 代理到 8081，避免跨域与连不上后端
const apiBase = import.meta.env.VITE_API_BASE ?? ''

export const api = axios.create({
  baseURL: apiBase
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('haomei_admin_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

