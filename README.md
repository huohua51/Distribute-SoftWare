# 高并发读实验项目

本项目用于完成"高并发读"作业，包含以下六部分：

1. **容器环境**：Dockerfile + docker-compose 启动所有服务。
2. **负载均衡**：Nginx 反向代理两个后端实例，支持多种算法。
3. **动静分离**：静态页面由 Nginx 直接提供，接口请求转发给后端。
4. **分布式缓存**：Redis 缓存商品详情，处理穿透、击穿、雪崩。
5. **读写分离**：MySQL 主从复制，写入走 Master，读取走 Slave。
6. **商品搜索**：ElasticSearch 全文搜索商品。

## 一、项目结构

```text
high-concurrency-demo
├─ backend
│  ├─ Dockerfile
│  ├─ package.json
│  └─ server.js
├─ mysql
│  ├─ master/my.cnf
│  ├─ slave/my.cnf
│  ├─ init/01-schema.sql
│  └─ slave-init/01-start-replication.sh
├─ nginx
│  ├─ nginx.conf
│  └─ html
│     ├─ index.html
│     ├─ style.css
│     └─ app.js
├─ docker-compose.yml
└─ README.md
```

## 二、服务说明

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| Redis | hc-redis | 6379 | 缓存 |
| MySQL Master | hc-mysql-master | 3306 | 主库（写） |
| MySQL Slave | hc-mysql-slave | 3307 | 从库（读） |
| ElasticSearch | hc-elasticsearch | 9200 | 全文搜索 |
| 后端 1 | hc-backend-1 | 8081 | 业务服务 |
| 后端 2 | hc-backend-2 | 8082 | 业务服务 |
| Nginx | hc-nginx | 80 | 反向代理入口 |

## 三、启动方式

```bash
docker compose up --build
```

启动后需要手动配置 MySQL 主从复制（首次）：

```bash
# 进入从库容器
docker exec -it hc-mysql-slave bash

# 在从库中执行
mysql -u root -prootpass -e "
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-master',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl_pass',
  SOURCE_AUTO_POSITION=1;
START REPLICA;
"

# 验证复制状态
mysql -u root -prootpass -e "SHOW REPLICA STATUS\G"
```

看到 `Replica_IO_Running: Yes` 和 `Replica_SQL_Running: Yes` 即表示主从复制成功。

访问：

- 前端页面：[http://localhost](http://localhost)
- 健康检查：[http://localhost/api/health](http://localhost/api/health)

停止服务：

```bash
docker compose down
```

清除数据卷（重置数据库）：

```bash
docker compose down -v
```

## 四、功能验证

### 1. 负载均衡

多次访问 `/api/products/1`，观察返回的 `instance` 在 `backend-1` 和 `backend-2` 之间交替。

### 2. 动静分离

- 浏览器访问 `http://localhost/` → Nginx 返回静态页面
- 访问 `http://localhost/api/products/1` → Nginx 转发到后端

### 3. 分布式缓存

- 第一次请求：`source` 为 `mysql-slave-rebuild-cache`（从 MySQL 从库读取后写入 Redis）
- 第二次请求：`source` 为 `redis-cache`（直接命中缓存）
- 请求不存在的商品：返回 404，写入空值缓存防止穿透

### 4. MySQL 读写分离

页面上点击「执行读写分离测试」按钮，或直接访问：

```bash
curl http://localhost/api/rw-test
```

返回结果会显示：
- 写入目标：`mysql-master`
- Master 读取：找到数据
- Slave 读取：找到数据（如果主从复制已同步）

也可以用「写入新商品 (Master)」按钮向主库写入数据，然后查询验证数据已同步到从库。

### 5. ElasticSearch 搜索

在页面搜索框输入关键词（如 `cache`、`nginx`、`mysql`），点击搜索按钮。

或直接访问：

```bash
curl "http://localhost/api/search?q=cache"
```

## 五、API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查（含各组件状态） |
| GET | /api/products | 获取所有商品（从 Slave 读） |
| GET | /api/products/:id | 获取单个商品（Redis 缓存 + Slave 读） |
| POST | /api/products | 新增商品（写入 Master） |
| PUT | /api/products/:id | 更新商品（写入 Master） |
| GET | /api/search?q=xxx | ElasticSearch 全文搜索 |
| GET | /api/rw-test | 读写分离自动化测试 |

## 六、切换负载均衡算法

编辑 `nginx/nginx.conf` 中的 `upstream backend_pool`，可选：

- 默认轮询（Round Robin）
- `least_conn`（最少连接）
- `ip_hash`（IP 哈希）

修改后重启 Nginx：

```bash
docker compose restart nginx
```

## 七、JMeter 测试建议

创建线程组分别测试：

1. 静态资源：`GET http://localhost/style.css`
2. 动态接口：`GET http://localhost/api/products/1`
3. 搜索接口：`GET http://localhost/api/search?q=cache`

推荐参数：线程数 50~100，Ramp-Up 1~5 秒，循环 20 次。

查看日志：

```bash
docker compose logs -f backend1 backend2
```

## 八、实验结论

- 通过 Docker Compose 将 Redis、MySQL 主从、ElasticSearch、后端服务和 Nginx 统一容器化部署。
- Nginx 对两个后端实例进行反向代理和负载均衡，分散请求压力。
- 静态资源由 Nginx 直接处理，动态接口转发给后端，实现动静分离。
- Redis 缓存商品详情，通过空值缓存、分布式锁、随机过期时间缓解穿透、击穿和雪崩。
- MySQL 主从复制实现读写分离：写操作走 Master，读操作走 Slave，降低主库压力。
- ElasticSearch 提供全文搜索功能，支持按商品名称和描述模糊匹配。
