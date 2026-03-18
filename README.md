# 高并发读实验项目

本项目用于完成“高并发读”作业，包含以下四部分：

1. 容器环境：使用 `Dockerfile` 和 `docker-compose.yml` 启动 Redis、两个后端实例和 Nginx。
2. 负载均衡：Nginx 反向代理两个后端实例，默认使用轮询算法。
3. 动静分离：静态页面由 Nginx 直接提供，接口请求转发给后端。
4. 分布式缓存：使用 Redis 缓存商品详情，并处理缓存穿透、击穿、雪崩问题。

## 一、项目结构

```text
high-concurrency-demo
├─ backend
│  ├─ Dockerfile
│  ├─ package.json
│  └─ server.js
├─ nginx
│  ├─ nginx.conf
│  └─ html
│     ├─ index.html
│     ├─ style.css
│     └─ app.js
├─ docker-compose.yml
└─ README.md
```

## 二、功能说明

### 1. 容器环境

- `redis`：缓存服务。
- `backend1`：第一个后端实例，对外映射到 `8081`。
- `backend2`：第二个后端实例，对外映射到 `8082`。
- `nginx`：统一入口，对外映射到 `80`。

### 2. 负载均衡

Nginx 将 `/api/*` 请求转发到两个后端实例：

- 默认：轮询（Round Robin）
- 可选：`least_conn`
- 可选：`ip_hash`

修改位置：`nginx/nginx.conf`

### 3. 动静分离

- `/`、`/index.html`、`/style.css`、`/app.js` 等静态资源由 Nginx 直接处理。
- `/api/*` 动态接口由 Nginx 转发到后端服务。

### 4. 分布式缓存

接口：`GET /api/products/:id`

缓存策略：

- 缓存命中：直接从 Redis 返回。
- 缓存穿透：空商品写入短期空值缓存。
- 缓存击穿：使用 Redis 分布式锁控制热点 Key 重建。
- 缓存雪崩：给缓存 TTL 增加随机值，避免同一时刻大量失效。

## 三、启动方式

先确保已安装：

- Docker Desktop
- Docker Compose（Docker Desktop 一般已内置）

在项目根目录执行：

```bash
docker compose up --build
```

启动后访问：

- 前端页面：[http://localhost](http://localhost)
- 后端 1：[http://localhost:8081/api/health](http://localhost:8081/api/health)
- 后端 2：[http://localhost:8082/api/health](http://localhost:8082/api/health)
- Nginx 统一入口：[http://localhost/api/health](http://localhost/api/health)

停止服务：

```bash
docker compose down
```

## 四、验证方法

### 1. 验证容器环境

执行：

```bash
docker compose ps
```

预期结果：看到 `redis`、`backend1`、`backend2`、`nginx` 四个服务正常运行。

### 2. 验证负载均衡

多次访问：

```bash
curl http://localhost/api/health
```

或：

```bash
curl http://localhost/api/products/1
```

观察返回中的 `instance` 字段是否在 `backend-1` 和 `backend-2` 之间切换。

### 3. 验证动静分离

浏览器访问：

- `http://localhost/`
- `http://localhost/style.css`
- `http://localhost/app.js`

说明静态资源由 Nginx 直接返回。

再访问：

- `http://localhost/api/products/1`

说明动态请求被代理到后端。

### 4. 验证分布式缓存

第一次请求商品详情：

```bash
curl http://localhost/api/products/1
```

预期：`source` 可能是 `database-rebuild-cache`。

再次请求相同商品：

```bash
curl http://localhost/api/products/1
```

预期：`source` 变成 `redis-cache` 或 `redis-cache-after-wait`。

请求不存在的商品：

```bash
curl http://localhost/api/products/999
```

预期：返回 404，并触发空值缓存，避免缓存穿透。

## 五、JMeter 测试建议

可以创建两个线程组，分别测试：

1. 静态资源：`GET http://localhost/style.css`
2. 动态接口：`GET http://localhost/api/products/1`

推荐参数：

- 线程数：50 或 100
- Ramp-Up：1~5 秒
- 循环次数：20

观察指标：

- 吞吐量
- 平均响应时间
- 95% 响应时间
- 错误率

并结合下面命令查看日志：

```bash
docker compose logs -f backend1 backend2 nginx
```

## 六、切换负载均衡算法

编辑 `nginx/nginx.conf` 中的 `upstream backend_pool`：

默认轮询：

```nginx
upstream backend_pool {
  server backend1:3000;
  server backend2:3000;
}
```

最少连接：

```nginx
upstream backend_pool {
  least_conn;
  server backend1:3000;
  server backend2:3000;
}
```

IP Hash：

```nginx
upstream backend_pool {
  ip_hash;
  server backend1:3000;
  server backend2:3000;
}
```

修改后重启：

```bash
docker compose restart nginx
```

## 七、可直接写进实验报告的结论

- 通过 Docker Compose 将 Redis、后端服务和 Nginx 统一容器化部署，简化了实验环境搭建。
- 使用 Nginx 对两个后端实例进行反向代理和负载均衡，能够分散请求压力。
- 静态资源由 Nginx 直接处理，动态接口再转发给后端，实现了动静分离。
- 利用 Redis 缓存商品详情，并通过空值缓存、分布式锁、随机过期时间分别缓解缓存穿透、击穿和雪崩问题。
