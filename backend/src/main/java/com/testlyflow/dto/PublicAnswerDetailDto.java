package com.testlyflow.dto;

/**
 * Employee-facing answer detail for the result page. Deliberately has no
 * {@code correctOption} field -- the taker must never learn the right answer for a question
 * they got wrong, only that they got it wrong (see AttemptContractSecurityTest). Admin views
 * use {@link AnswerDetailDto} instead, which does carry the correct option.
 */
public record PublicAnswerDetailDto(
        Long questionId,
        int number,
        String questionText,
        Long categoryId,
        String categoryName,
        String selectedOption,
        boolean isCorrect,
        long timeSpentMs
) {
    public static PublicAnswerDetailDto from(AnswerDetailDto detail) {
        return new PublicAnswerDetailDto(
                detail.questionId(),
                detail.number(),
                detail.questionText(),
                detail.categoryId(),
                detail.categoryName(),
                detail.selectedOption(),
                detail.isCorrect(),
                detail.timeSpentMs());
    }
}
