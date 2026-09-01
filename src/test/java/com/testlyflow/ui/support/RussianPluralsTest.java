package com.testlyflow.ui.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RussianPluralsTest {

    @Test
    void questionsDeclension() {
        assertEquals("вопрос", RussianPlurals.questions(1));
        assertEquals("вопроса", RussianPlurals.questions(2));
        assertEquals("вопросов", RussianPlurals.questions(5));
        assertEquals("вопросов", RussianPlurals.questions(11));
        assertEquals("вопрос", RussianPlurals.questions(21));
        assertEquals("вопросов", RussianPlurals.questions(111));
    }

    @Test
    void mistakesDeclension() {
        assertEquals("ошибка", RussianPlurals.mistakes(1));
        assertEquals("ошибки", RussianPlurals.mistakes(3));
        assertEquals("ошибок", RussianPlurals.mistakes(5));
        assertEquals("ошибок", RussianPlurals.mistakes(12));
    }
}
