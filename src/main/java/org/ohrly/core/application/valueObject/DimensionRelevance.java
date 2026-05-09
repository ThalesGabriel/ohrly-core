package org.ohrly.core.application.valueObject;

public record DimensionRelevance(
        String dimensionKey,
        double explainedVarianceRatio,
        int distinctValues,
        int minimumGroupSize
) {}