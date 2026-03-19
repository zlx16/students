import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client = null

export function connectHaomeiWs({ onOrder, onMenu } = {}) {
  if (client && client.active) return client

  client = new Client({
    // Vite 会把 /ws 代理到后端的 WebSocket 端点
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    debug: false,
    onConnect: () => {
      if (onOrder) {
        client.subscribe('/topic/orders', (message) => {
          try {
            onOrder(JSON.parse(message.body))
          } catch (e) {}
        })
      }
      if (onMenu) {
        client.subscribe('/topic/menu', (message) => {
          try {
            onMenu(JSON.parse(message.body))
          } catch (e) {}
        })
      }
    }
  })

  client.activate()
  return client
}

