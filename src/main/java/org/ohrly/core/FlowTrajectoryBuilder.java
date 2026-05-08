package org.ohrly.core;

import org.ohrly.core.valueObjects.BehavioralPrimitive;
import org.ohrly.core.valueObjects.FlowTrajectory;

import java.util.List;

public interface FlowTrajectoryBuilder {
    FlowTrajectory build(String flowId, String sessionId, List<BehavioralPrimitive> primitives);
}
