package com.testlyflow.parser;

import com.testlyflow.exception.TestParsingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownTestParserTest {

    private final MarkdownTestParser parser = new MarkdownTestParser();

    private String readSample() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/sample-test.md")) {
            assertNotNull(in, "sample-test.md must be present in test resources");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesAllQuestionsFromSampleFile() throws IOException {
        ParsedTestResult result = parser.parse(readSample());

        assertEquals("Тест по продукту АРМК", result.getTitleFromFile());
        assertEquals(5, result.getQuestions().size());

        ParsedQuestion first = result.getQuestions().get(0);
        assertEquals(1, first.getNumber());
        assertTrue(first.getText().contains("удаляются"));
        assertEquals(4, first.getOptions().size());
        assertEquals("PATCH-запросом с isDeleted = true", first.getOptions().get("В"));
    }

    @Test
    void handlesMultiplePairsPerRowAndShortLastRow() throws IOException {
        ParsedTestResult result = parser.parse(readSample());

        assertEquals("В", findQuestion(result, 1).getCorrectOption());
        assertEquals("Б", findQuestion(result, 2).getCorrectOption());
        assertEquals("В", findQuestion(result, 3).getCorrectOption());
        assertEquals("А", findQuestion(result, 4).getCorrectOption());
        assertEquals("Г", findQuestion(result, 5).getCorrectOption());
    }

    @Test
    void parsesRowsWithMoreThanTwoPairs() {
        String md = """
                **1. Q1?**
                - А) a
                - Б) b

                **2. Q2?**
                - А) a
                - Б) b

                **3. Q3?**
                - А) a
                - Б) b

                **4. Q4?**
                - А) a
                - Б) b

                ---

                ## Ключ ответов

                | № | Ответ | № | Ответ | № | Ответ | № | Ответ |
                |---|---|---|---|---|---|---|---|
                | 1 | А | 2 | Б | 3 | А | 4 | Б |
                """;

        ParsedTestResult result = parser.parse(md);

        assertEquals(4, result.getQuestions().size());
        assertEquals("А", findQuestion(result, 1).getCorrectOption());
        assertEquals("Б", findQuestion(result, 2).getCorrectOption());
        assertEquals("А", findQuestion(result, 3).getCorrectOption());
        assertEquals("Б", findQuestion(result, 4).getCorrectOption());
    }

    @Test
    void throwsWhenQuestionHasNoAnswerInKey() {
        String md = """
                **1. Q1?**
                - А) a
                - Б) b

                **2. Q2?**
                - А) a
                - Б) b

                ---

                ## Ключ ответов

                | № | Ответ |
                |---|---|
                | 1 | А |
                """;

        TestParsingException ex = assertThrows(TestParsingException.class, () -> parser.parse(md));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("2")));
    }

    @Test
    void warnsWhenKeyHasExtraNumberWithoutQuestion() {
        String md = """
                **1. Q1?**
                - А) a
                - Б) b

                ---

                ## Ключ ответов

                | № | Ответ |
                |---|---|
                | 1 | А |
                | 2 | Б |
                """;

        ParsedTestResult result = parser.parse(md);

        assertEquals(1, result.getQuestions().size());
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    void throwsWhenNoQuestionsFound() {
        String md = """
                ---

                ## Ключ ответов

                | № | Ответ |
                |---|---|
                | 1 | А |
                """;

        assertThrows(TestParsingException.class, () -> parser.parse(md));
    }

    @Test
    void throwsWhenAnswerKeySectionMissing() {
        String md = """
                **1. Q1?**
                - А) a
                - Б) b
                """;

        assertThrows(TestParsingException.class, () -> parser.parse(md));
    }

    @Test
    void bindsAnswerKeyWhenHeaderRowIsOmitted() {
        String md = """
                **1. Q1?**
                - А) a
                - Б) b

                **2. Q2?**
                - А) a
                - Б) b

                ---

                ## Ключ ответов

                | 1 | А |
                | 2 | Б |
                """;

        ParsedTestResult result = parser.parse(md);
        assertEquals("А", findQuestion(result, 1).getCorrectOption());
        assertEquals("Б", findQuestion(result, 2).getCorrectOption());
    }

    @Test
    void bindsLatinLookalikeLettersInTheAnswerKey() {
        String md = """
                **1. Q1?**
                - А) a
                - Б) b
                - В) c

                ---

                ## Ключ ответов

                | № | Ответ |
                |---|---|
                | 1 | A |
                """;

        ParsedTestResult result = parser.parse(md);
        assertEquals("А", findQuestion(result, 1).getCorrectOption());
    }

    @Test
    void bindsAnswerLettersWithTrailingParen() {
        String md = """
                **1. Q1?**
                - А) a
                - В) c

                ---

                ## Ключ ответов

                | № | Ответ |
                |---|---|
                | 1 | В) |
                """;

        ParsedTestResult result = parser.parse(md);
        assertEquals("В", findQuestion(result, 1).getCorrectOption());
    }

    @Test
    void parsesUtf8BomAndWindows1251Bytes() {
        String md = """
                # Title
                **1. Q1?**
                - А) a
                - Б) b

                ---

                ## Ключ ответов

                | № | Ответ |
                |---|---|
                | 1 | А |
                """;

        byte[] bom = new byte[3 + md.getBytes(StandardCharsets.UTF_8).length];
        bom[0] = (byte) 0xEF;
        bom[1] = (byte) 0xBB;
        bom[2] = (byte) 0xBF;
        System.arraycopy(md.getBytes(StandardCharsets.UTF_8), 0, bom, 3, md.getBytes(StandardCharsets.UTF_8).length);
        assertEquals("А", parser.parse(bom).getQuestions().get(0).getCorrectOption());

        byte[] windows1251 = md.getBytes(java.nio.charset.Charset.forName("windows-1251"));
        assertEquals("А", parser.parse(windows1251).getQuestions().get(0).getCorrectOption());
    }

    private ParsedQuestion findQuestion(ParsedTestResult result, int number) {
        return result.getQuestions().stream()
                .filter(q -> q.getNumber() == number)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Question " + number + " not found"));
    }
}
