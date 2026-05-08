package org.ohrly.core.valueObjects;

import java.util.Map;

public record FlowContext(
        String flowId,
        Map<String, Object> dimensions
) {}
