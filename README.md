# 好美味餐厅点餐系统（Spring Boot + Vue）

面向餐厅扫码点餐场景，包含用户端（H5）与后台管理端（Web）。

## 当前功能

- 用户端扫码进入（桌台 `token`），先填写人数再点餐。
- 菜品支持分类、搜索、购物车、大小份下单。
- 支付流程：下单后进入支付弹窗，扫码付款，支持模拟成功/失败/取消。
- 支付成功后用户端弹出“支付成功”提示。
- 用户端订单只显示本桌进行中订单（历史已完成/已取消不显示）。
- 后台管理支持：
  - 管理员登录/退出
  - 10桌状态总览（空闲/正在用餐）
  - 按桌查看订单、改状态、重置桌台
  - 菜品新增/编辑/删除/图片上传
  - 菜品规格选择：`仅小份 / 仅大份 / 大小份`
- 前后端通过 WebSocket 实时同步菜单与订单。

## 技术栈

- 后端：Spring Boot 3、Spring Security、JPA、SQLite、WebSocket(STOMP)
- 前端：Vue 3、Vite、Pinia、Vue Router、Axios、SockJS + StompJS

## 目录说明

- `backend`：后端项目（Java）
- `frontend`：前端项目（Vue）
- `backend/data/haomei.db`：SQLite 数据文件（运行后生成）
- `backend/uploads/dish-images`：菜品图片上传目录（运行后生成）

## 开发启动

### 1) 启动后端（默认 8081）

进入 `backend` 后执行（Windows）：

```powershell
.\run-backend.ps1
```

如果 `JAVA_HOME` 无效，优先用：

```powershell
.\start-backend.cmd clean spring-boot:run
```

### 2) 启动前端（默认 5173）

进入 `frontend` 后执行：

```powershell
npm install
npm run dev -- --host
```

前端通过 Vite 代理访问后端：
- `/api` -> `http://localhost:8081`
- `/ws` -> `http://localhost:8081`

## 访问地址

- 用户端：`http://localhost:5173/`
- 后台端：`http://localhost:5173/admin`

局域网访问示例：
- `http://<你的局域网IP>:5173/`
- `http://<你的局域网IP>:5173/admin`

## 微信扫码（H5）

在 `backend/src/main/resources/application.yml` 配置二维码基础地址：

```yaml
app:
  site:
    baseUrl: "https://你的域名或穿透地址"
```

说明：
- 配置后，后台桌台二维码会使用该地址。
- 本地测试可填局域网地址，如 `http://192.168.x.x:5173`。
- 跨网络访问可使用内网穿透地址（如 cloudflared/ngrok 提供的 https 域名）。

## 管理员账号

配置文件：`backend/src/main/resources/application.yml`

- 用户名：`app.admin.username`（默认 `admin`）
- 密码：`app.admin.password`（默认 `{noop}admin123`）

## 支付流程说明（当前实现）

1. 用户点击“去付款”后创建订单（`WAIT_PAY`）。
2. 在支付弹窗中进行支付确认（模拟流程）：
   - `POST /api/orders/{id}/pay/start`
   - `POST /api/orders/{id}/pay/confirm`
3. 成功后订单转为已支付并完成，桌台无进行中订单时自动恢复空闲。
4. 订单变化实时推送到用户端与后台端。

## 关键接口（排查用）

- 解析桌台：`GET /api/tables/resolve?token=...`
- 用户菜品：`GET /api/dishes`
- 创建订单：`POST /api/orders`
- 开始支付：`POST /api/orders/{id}/pay/start`
- 确认支付：`POST /api/orders/{id}/pay/confirm`
- 取消支付：`POST /api/orders/{id}/pay/cancel`
- 本桌订单：`GET /api/orders?tableNo=...`
- 后台菜品列表：`GET /api/admin/dishes`
- 后台菜品图片上传：`POST /api/admin/dishes/{id}/image`
- 后台桌台列表：`GET /api/admin/tables`

## 常见问题

- 前端报 `host is not allowed`：
  - 检查 `frontend/vite.config.js` 中 `server.allowedHosts` 配置。
  - 修改后需重启前端 dev 服务。

- 后台登录失败：
  - 先确认后端 8081 正常启动。
  - 再检查账号密码是否与 `application.yml` 一致。

- 手机扫码打不开：
  - 局域网模式：手机与电脑需同一 Wi-Fi。
  - 跨网模式：使用公网可达的穿透/部署地址。

## 备注

本 README 以当前项目代码实现为准，若后续新增业务流程，请同步更新本文档。
