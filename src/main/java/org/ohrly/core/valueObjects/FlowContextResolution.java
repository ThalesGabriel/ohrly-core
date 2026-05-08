package org.ohrly.core.valueObjects;

import java.util.List;

public record FlowContextResolution(
        FlowContext selectedContext,
        int sampleSize,
        double confidence,
        List<String> usedDimensions
) {}
