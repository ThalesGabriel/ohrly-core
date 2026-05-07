package org.ohrly.core.valueObjects;

public record Baseline(
        Context context,
        double average,
        double p95
) { }