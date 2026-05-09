package org.ohrly.core.domain.valueObject;

import java.util.Map;

public record FlowContext(
        String flowId,
        Map<String, Object> dimensions
) {}
