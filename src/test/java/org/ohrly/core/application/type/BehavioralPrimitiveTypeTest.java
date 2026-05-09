package org.ohrly.core.application.type;

import org.junit.jupiter.api.Test;
import org.ohrly.core.application.type.BehavioralPrimitiveCategory;
import org.ohrly.core.application.type.BehavioralPrimitiveType;

import static org.assertj.core.api.Assertions.assertThat;

class BehavioralPrimitiveTypeTest {

    @Test
    void shouldClassifySessionPrimitives() {
        assertThat(BehavioralPrimitiveType.START.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.SESSION);

        assertThat(BehavioralPrimitiveType.COMPLETE.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.SESSION);

        assertThat(BehavioralPrimitiveType.ABANDON.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.SESSION);

        assertThat(BehavioralPrimitiveType.TIMEOUT.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.SESSION);
    }

    @Test
    void shouldClassifyStepPrimitives() {
        assertThat(BehavioralPrimitiveType.STEP_REACHED.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.STEP);

        assertThat(BehavioralPrimitiveType.STEP_COMPLETED.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.STEP);

        assertThat(BehavioralPrimitiveType.STEP_FAILED.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.STEP);
    }

    @Test
    void shouldClassifyFrictionPrimitives() {
        assertThat(BehavioralPrimitiveType.WAIT.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.FRICTION);

        assertThat(BehavioralPrimitiveType.RETRY.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.FRICTION);

        assertThat(BehavioralPrimitiveType.LOOP.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.FRICTION);
    }

    @Test
    void shouldClassifyEscalationPrimitives() {
        assertThat(BehavioralPrimitiveType.FALLBACK.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.ESCALATION);

        assertThat(BehavioralPrimitiveType.HUMAN_HANDOFF.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.ESCALATION);

        assertThat(BehavioralPrimitiveType.TRANSFER.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.ESCALATION);
    }

    @Test
    void shouldClassifyTemporalIntegrityPrimitives() {
        assertThat(BehavioralPrimitiveType.LATE_EVENT.getCategory())
                .isEqualTo(BehavioralPrimitiveCategory.TEMPORAL_INTEGRITY);
    }

    @Test
    void shouldHaveDescriptionsForAllPrimitiveTypes() {
        for (BehavioralPrimitiveType type : BehavioralPrimitiveType.values()) {
            assertThat(type.getDescription())
                    .isNotBlank();
        }
    }

    @Test
    void shouldHaveDescriptionsForAllPrimitiveCategories() {
        for (BehavioralPrimitiveCategory category : BehavioralPrimitiveCategory.values()) {
            assertThat(category.getDescription())
                    .isNotBlank();
        }
    }
}