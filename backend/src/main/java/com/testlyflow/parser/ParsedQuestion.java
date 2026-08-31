package com.testlyflow.parser;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParsedQuestion {

    private final int number;
    private final String text;
    private final Map<String, String> options = new LinkedHashMap<>();
    private String correctOption;

    public ParsedQuestion(int number, String text) {
        this.number = number;
        this.text = text;
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void addOption(String letter, String optionText) {
        options.put(letter, optionText);
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }
}
