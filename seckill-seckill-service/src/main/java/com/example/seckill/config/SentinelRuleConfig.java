package com.example.seckill.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRuleConfig {

    @Value("${app.limit.seckill-qps:20}")
    private double seckillQps;

    @Value("${app.limit.query-qps:60}")
    private double queryQps;

    @PostConstruct
    public void initRules() {
        FlowRule submitOrderRule = new FlowRule();
        submitOrderRule.setResource("submitSeckillOrder");
        submitOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        submitOrderRule.setCount(seckillQps);

        FlowRule queryOrderRule = new FlowRule();
        queryOrderRule.setResource("queryOrderByOrderId");
        queryOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        queryOrderRule.setCount(queryQps);

        DegradeRule paymentRule = new DegradeRule();
        paymentRule.setResource("payOrder");
        paymentRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        paymentRule.setCount(1500);
        paymentRule.setTimeWindow(10);
        paymentRule.setMinRequestAmount(5);
        paymentRule.setStatIntervalMs(60000);

        FlowRuleManager.loadRules(List.of(submitOrderRule, queryOrderRule));
        DegradeRuleManager.loadRules(List.of(paymentRule));
    }
}
