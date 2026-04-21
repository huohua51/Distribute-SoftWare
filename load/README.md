# JMeter 压测说明

本目录用于验证接入 `Gateway + Sentinel` 前后的秒杀接口差异。

## 前提

1. 启动基础设施与服务。
2. 确保网关地址可访问：`http://localhost:8084`
3. 先通过网关初始化商品：

```bash
curl -X POST "http://localhost:8084/api/seckill/products" ^
  -H "Content-Type: application/json" ^
  -d "{\"productId\":1001,\"productName\":\"iPhone 16\",\"stock\":200}"
```

## 压测对象

- 秒杀接口：`POST /api/seckill/orders`
- 入口地址：`gateway-service`

## 执行方式

设置 `JMETER_HOME` 后运行：

```powershell
.\load\run_jmeter.ps1
```

## 观察点

- 未调低 `app.limit.gateway-route-qps` 与 `app.limit.seckill-qps` 时，成功数更高、限流较少
- 调低 Nacos 配置中的 QPS 后，再次执行压测，可在返回体中观察到限流提示
- 对比 `load/results/result.jtl` 与 HTML 报表中的吞吐量、错误率、RT
# load

压测目录，预留给 JMeter、wrk、ab 等压测脚本。

建议后续补充：

- `jmeter_test_plan.jmx`
- `run_jmeter.ps1`
- 秒杀接口压测参数说明
