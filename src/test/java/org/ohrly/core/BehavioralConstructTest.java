package org.ohrly.core;

import org.junit.jupiter.api.Test;
import org.ohrly.core.enums.BehavioralConstructType;
import org.ohrly.core.enums.BehavioralPrimitiveType;
import org.ohrly.core.valueObjects.BehavioralConstruct;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BehavioralConstructTest {

    @Test
    void shouldCreateFrictionConstructFromFrictionPrimitives() {
        var construct = new BehavioralConstruct(
                BehavioralConstructType.FRICTION,
                List.of(
                        BehavioralPrimitiveType.WAIT,
                        BehavioralPrimitiveType.RETRY,
                        BehavioralPrimitiveType.LOOP
                ),
                7.5,
                "Session required additional effort to preserve continuity."
        );

        assertThat(construct.type())
                .isEqualTo(BehavioralConstructType.FRICTION);

        assertThat(construct.primitives())
                .containsExactly(
                        BehavioralPrimitiveType.WAIT,
                        BehavioralPrimitiveType.RETRY,
                        BehavioralPrimitiveType.LOOP
                );

        assertThat(construct.score())
                .isEqualTo(7.5);

        assertThat(construct.description())
                .isNotBlank();
    }

    @Test
    void shouldCreateRuptureConstructFromFailureAndAbandon() {
        var construct = new BehavioralConstruct(
                BehavioralConstructType.RUPTURE,
                List.of(
                        BehavioralPrimitiveType.STEP_FAILED,
                        BehavioralPrimitiveType.ABANDON
                ),
                9.0,
                "Session broke functional continuity after a failed step."
        );

        assertThat(construct.type())
                .isEqualTo(BehavioralConstructType.RUPTURE);

        assertThat(construct.primitives())
                .contains(
                        BehavioralPrimitiveType.STEP_FAILED,
                        BehavioralPrimitiveType.ABANDON
                );
    }

    @Test
    void shouldCreateRecoveryConstructFromFailureRetryAndComplete() {
        var construct = new BehavioralConstruct(
                BehavioralConstructType.RECOVERY,
                List.of(
                        BehavioralPrimitiveType.STEP_FAILED,
                        BehavioralPrimitiveType.RETRY,
                        BehavioralPrimitiveType.COMPLETE
                ),
                6.0,
                "Session restored continuity after failure."
        );

        assertThat(construct.type())
                .isEqualTo(BehavioralConstructType.RECOVERY);

        assertThat(construct.primitives())
                .containsExactly(
                        BehavioralPrimitiveType.STEP_FAILED,
                        BehavioralPrimitiveType.RETRY,
                        BehavioralPrimitiveType.COMPLETE
                );
    }

    @Test
    void shouldHaveDescriptionsForAllConstructTypes() {
        for (BehavioralConstructType type : BehavioralConstructType.values()) {
            assertThat(type.getDescription())
                    .isNotBlank();
        }
    }
}