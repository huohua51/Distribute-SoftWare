package com.example.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRuleConfig {

    @Value("${app.limit.internal-qps:100}")
    private double internalQps;

    @PostConstruct
    public void initRules() {
        FlowRule findRule = new FlowRule();
        findRule.setResource("orderFindByOrderId");
        findRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        findRule.setCount(internalQps);

        FlowRule existsRule = new FlowRule();
        existsRule.setResource("orderExists");
        existsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        existsRule.setCount(internalQps);

        FlowRuleManager.loadRules(List.of(findRule, existsRule));
    }
}
