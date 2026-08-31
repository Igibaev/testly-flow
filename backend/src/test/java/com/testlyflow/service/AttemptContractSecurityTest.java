package com.testlyflow.service;

import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.dto.AttemptStateDto;
import com.testlyflow.dto.SavedAnswerDto;
import com.testlyflow.dto.StartAttemptResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards the contract from ticket #6: neither the composition of an in-progress attempt
 * (GET /api/attempts/{id}, POST /api/attempts/start) nor a saved answer echo the correct
 * option or a correctness flag back to the client while the attempt is still running.
 */
class AttemptContractSecurityTest {

    private static final Set<String> FORBIDDEN_FIELD_NAME_FRAGMENTS = Set.of("correct", "iscorrect");

    @Test
    void startAttemptResponseQuestionsNeverCarryTheAnswerKey() {
        assertNoForbiddenFields(AttemptQuestionDto.class);
    }

    @Test
    void attemptStateResponseNeverCarriesTheAnswerKey() {
        assertNoForbiddenFields(AttemptStateDto.class);
        assertNoForbiddenFields(AttemptQuestionDto.class);
        assertNoForbiddenFields(SavedAnswerDto.class);
    }

    @Test
    void startAttemptResponseTopLevelHasNoAnswerKeyField() {
        assertNoForbiddenFields(StartAttemptResponse.class);
    }

    private void assertNoForbiddenFields(Class<?> record) {
        for (RecordComponent component : record.getRecordComponents()) {
            String lower = component.getName().toLowerCase();
            boolean forbidden = FORBIDDEN_FIELD_NAME_FRAGMENTS.stream().anyMatch(lower::contains);
            assertFalse(forbidden, record.getSimpleName() + "." + component.getName()
                    + " looks like it could leak answer correctness before the attempt is submitted");
        }
    }
}
