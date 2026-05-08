package org.ohrly.core.valueObjects;

public record DimensionRelevance(
        String dimensionKey,
        double explainedVarianceRatio,
        int distinctValues,
        int minimumGroupSize
) {}