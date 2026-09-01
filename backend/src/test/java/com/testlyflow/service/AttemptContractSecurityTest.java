package com.testlyflow.service;

import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.dto.AttemptStateDto;
import com.testlyflow.dto.PublicAnswerDetailDto;
import com.testlyflow.dto.SavedAnswerDto;
import com.testlyflow.dto.StartAttemptResponse;
import com.testlyflow.dto.SubmitAttemptResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards two related contracts: (1) neither the composition of an in-progress attempt
 * (GET /api/attempts/{id}, POST /api/attempts/start) nor a saved answer echo the correct
 * option or a correctness flag back to the client while the attempt is still running, and
 * (2) even after submit, the employee-facing response never reveals which option was the
 * correct one for a question they got wrong -- only that they got it wrong. The correct
 * option stays admin-only (see AnswerDetailDto, used by AdminAttemptDetailDto).
 */
class AttemptContractSecurityTest {

    private static final Set<String> FORBIDDEN_FIELD_NAME_FRAGMENTS = Set.of("correct", "iscorrect");
    private static final Set<String> FORBIDDEN_ANSWER_KEY_FRAGMENTS = Set.of("correctoption", "answerkey");

    @Test
    void startAttemptResponseQuestionsNeverCarryTheAnswerKey() {
        assertNoForbiddenFields(AttemptQuestionDto.class, FORBIDDEN_FIELD_NAME_FRAGMENTS);
    }

    @Test
    void attemptStateResponseNeverCarriesTheAnswerKey() {
        assertNoForbiddenFields(AttemptStateDto.class, FORBIDDEN_FIELD_NAME_FRAGMENTS);
        assertNoForbiddenFields(AttemptQuestionDto.class, FORBIDDEN_FIELD_NAME_FRAGMENTS);
        assertNoForbiddenFields(SavedAnswerDto.class, FORBIDDEN_FIELD_NAME_FRAGMENTS);
    }

    @Test
    void startAttemptResponseTopLevelHasNoAnswerKeyField() {
        assertNoForbiddenFields(StartAttemptResponse.class, FORBIDDEN_FIELD_NAME_FRAGMENTS);
    }

    @Test
    void submitResponseNeverRevealsTheCorrectOptionForAWrongAnswer() {
        // isCorrect (whether the taker's own pick was right) is fine post-submission --
        // only the specific correct letter must never reach the employee.
        assertNoForbiddenFields(PublicAnswerDetailDto.class, FORBIDDEN_ANSWER_KEY_FRAGMENTS);
        assertNoForbiddenFields(SubmitAttemptResponse.class, FORBIDDEN_ANSWER_KEY_FRAGMENTS);
    }

    private void assertNoForbiddenFields(Class<?> record, Set<String> forbiddenFragments) {
        for (RecordComponent component : record.getRecordComponents()) {
            String lower = component.getName().toLowerCase();
            boolean forbidden = forbiddenFragments.stream().anyMatch(lower::contains);
            assertFalse(forbidden, record.getSimpleName() + "." + component.getName()
                    + " looks like it could leak the correct answer to the employee taking the test");
        }
    }
}
