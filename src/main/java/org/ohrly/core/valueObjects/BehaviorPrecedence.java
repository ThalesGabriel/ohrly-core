package org.ohrly.core.valueObjects;

public record BehaviorPrecedence(
        Context context,
        boolean matchesHistoricalPattern,
        double similarityScore,
        String message
) {
    public static BehaviorPrecedence empty(Context context, String message) {
        return new BehaviorPrecedence(context, false, 0, message);
    }
}
