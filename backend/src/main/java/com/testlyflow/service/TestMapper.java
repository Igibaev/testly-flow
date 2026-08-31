package com.testlyflow.service;

import com.testlyflow.dto.*;
import com.testlyflow.entity.PrepLink;
import com.testlyflow.entity.Question;
import com.testlyflow.entity.Test;

import java.util.List;

public final class TestMapper {

    private TestMapper() {
    }

    public static TestSummaryDto toSummary(Test test) {
        return new TestSummaryDto(test.getId(), test.getTitle(), test.getDescription(), test.getQuestions().size());
    }

    public static TestDetailDto toDetail(Test test) {
        List<PrepLinkDto> prepLinks = test.getPrepLinks().stream()
                .map(TestMapper::toPrepLinkDto)
                .toList();

        List<QuestionPublicDto> questions = test.getQuestions().stream()
                .map(TestMapper::toQuestionPublicDto)
                .toList();

        return new TestDetailDto(test.getId(), test.getTitle(), test.getDescription(), prepLinks, questions);
    }

    public static PrepLinkDto toPrepLinkDto(PrepLink link) {
        return new PrepLinkDto(link.getId(), link.getTitle(), link.getUrl());
    }

    public static QuestionPublicDto toQuestionPublicDto(Question question) {
        List<QuestionOptionDto> options = question.getOptions().stream()
                .map(o -> new QuestionOptionDto(o.getOptionLetter(), o.getText()))
                .toList();
        return new QuestionPublicDto(question.getId(), question.getNumber(), question.getText(), options);
    }

}
