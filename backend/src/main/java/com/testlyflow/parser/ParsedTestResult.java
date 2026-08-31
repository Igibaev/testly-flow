package com.testlyflow.parser;

import java.util.List;

public class ParsedTestResult {

    private final String titleFromFile;
    private final List<ParsedQuestion> questions;
    private final List<String> warnings;

    public ParsedTestResult(String titleFromFile, List<ParsedQuestion> questions, List<String> warnings) {
        this.titleFromFile = titleFromFile;
        this.questions = questions;
        this.warnings = warnings;
    }

    public String getTitleFromFile() {
        return titleFromFile;
    }

    public List<ParsedQuestion> getQuestions() {
        return questions;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
