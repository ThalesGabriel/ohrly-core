package org.ohrly.core.application.valueObject;

import org.ohrly.core.domain.valueObject.FlowContext;

import java.util.List;

public record FlowContextResolution(
        FlowContext selectedContext,
        int sampleSize,
        double confidence,
        List<String> usedDimensions
) {}
