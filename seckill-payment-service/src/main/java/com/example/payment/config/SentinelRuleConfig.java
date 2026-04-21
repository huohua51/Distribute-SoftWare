package com.example.payment.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRuleConfig {

    @Value("${app.limit.internal-qps:50}")
    private double internalQps;

    @PostConstruct
    public void initRules() {
        FlowRule payRule = new FlowRule();
        payRule.setResource("paymentPay");
        payRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        payRule.setCount(internalQps);

        FlowRuleManager.loadRules(List.of(payRule));
    }
}
