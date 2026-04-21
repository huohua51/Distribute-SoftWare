package com.example.inventory.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRuleConfig {

    @Value("${app.limit.internal-qps:80}")
    private double internalQps;

    @PostConstruct
    public void initRules() {
        FlowRule reserveRule = new FlowRule();
        reserveRule.setResource("inventoryReserve");
        reserveRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        reserveRule.setCount(internalQps);

        FlowRule queryRule = new FlowRule();
        queryRule.setResource("inventoryQuery");
        queryRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        queryRule.setCount(internalQps);

        FlowRuleManager.loadRules(List.of(reserveRule, queryRule));
    }
}
