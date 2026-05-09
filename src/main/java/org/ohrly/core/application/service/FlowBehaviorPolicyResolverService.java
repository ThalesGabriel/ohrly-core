package org.ohrly.core.application.service;

import org.ohrly.core.application.valueObject.FlowBaseline;
import org.ohrly.core.application.valueObject.FlowBehaviorPolicy;
import org.springframework.stereotype.Service;

@Service
public class FlowBehaviorPolicyResolverService {

    public FlowBehaviorPolicy resolve(
            FlowBehaviorPolicy policy,
            FlowBaseline baseline
    ) {
        if (policy != null) {
            return policy;
        }

        return FlowBehaviorPolicy.defaultFor(
                baseline.context().flowId(),
                baseline.context().flowId()
        );
    }
}
