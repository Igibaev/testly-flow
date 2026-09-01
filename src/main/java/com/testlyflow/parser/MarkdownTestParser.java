package com.testlyflow.parser;

import com.testlyflow.exception.TestParsingException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a single Markdown test file containing both the question section
 * and, after a "---" divider, a "## Ключ ответов" answer-key table.
 */
@Component
public class MarkdownTestParser {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s+(.+?)\\s*$");
    private static final Pattern KEY_HEADER_PATTERN = Pattern.compile("^##\\s*Ключ\\s+ответов\\s*$");
    private static final Pattern QUESTION_PATTERN = Pattern.compile("^\\*\\*(\\d+)\\.\\s*(.+?)\\*\\*\\s*$");
    private static final Pattern OPTION_PATTERN = Pattern.compile("^[-*]\\s*([А-Яа-яA-Za-z])\\)\\s*(.+?)\\s*$");
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\|?[\\s:|-]+\\|?$");

    public ParsedTestResult parse(String content) {
        if (content == null || content.isBlank()) {
            throw new TestParsingException("Файл теста пуст", List.of());
        }

        String[] rawLines = content.replace("\r\n", "\n").split("\n", -1);
        List<String> lines = new ArrayList<>(List.of(rawLines));

        int keyHeaderIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (KEY_HEADER_PATTERN.matcher(lines.get(i).trim()).matches()) {
                keyHeaderIndex = i;
                break;
            }
        }
        if (keyHeaderIndex == -1) {
            throw new TestParsingException(
                    "Не найдена секция \"## Ключ ответов\" с таблицей правильных ответов", List.of());
        }

        List<String> questionLines = lines.subList(0, keyHeaderIndex);
        List<String> keyLines = lines.subList(keyHeaderIndex + 1, lines.size());

        String title = extractTitle(questionLines);
        List<ParsedQuestion> questions = parseQuestions(questionLines);
        if (questions.isEmpty()) {
            throw new TestParsingException("В файле не найдено ни одного вопроса", List.of());
        }

        Map<Integer, String> answerKey = parseAnswerKey(keyLines);
        if (answerKey.isEmpty()) {
            throw new TestParsingException(
                    "Таблица \"## Ключ ответов\" не содержит ни одной пары \"№ | Ответ\"", List.of());
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ParsedQuestion question : questions) {
            String correct = answerKey.get(question.getNumber());
            if (correct == null) {
                errors.add("Для вопроса №" + question.getNumber() + " не найден ответ в ключе ответов");
                continue;
            }
            if (!question.getOptions().containsKey(correct)) {
                errors.add("Ключ ответов указывает вариант \"" + correct + "\" для вопроса №"
                        + question.getNumber() + ", но такого варианта нет среди его вариантов ответа");
                continue;
            }
            question.setCorrectOption(correct);
        }

        java.util.Set<Integer> questionNumbers = new java.util.HashSet<>();
        for (ParsedQuestion q : questions) {
            questionNumbers.add(q.getNumber());
        }
        for (Integer keyNumber : answerKey.keySet()) {
            if (!questionNumbers.contains(keyNumber)) {
                warnings.add("В ключе ответов есть номер " + keyNumber
                        + ", для которого нет вопроса в основной секции — проигнорирован");
            }
        }

        if (!errors.isEmpty()) {
            throw new TestParsingException("Не удалось связать вопросы с ключом ответов", errors);
        }

        return new ParsedTestResult(title, questions, warnings);
    }

    private String extractTitle(List<String> questionLines) {
        for (String line : questionLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher matcher = TITLE_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                return matcher.group(1).trim();
            }
            if (QUESTION_PATTERN.matcher(trimmed).matches()) {
                break;
            }
        }
        return null;
    }

    private List<ParsedQuestion> parseQuestions(List<String> questionLines) {
        List<ParsedQuestion> questions = new ArrayList<>();
        ParsedQuestion current = null;

        for (String rawLine : questionLines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher questionMatcher = QUESTION_PATTERN.matcher(line);
            if (questionMatcher.matches()) {
                current = new ParsedQuestion(
                        Integer.parseInt(questionMatcher.group(1)),
                        questionMatcher.group(2).trim());
                questions.add(current);
                continue;
            }

            Matcher optionMatcher = OPTION_PATTERN.matcher(line);
            if (optionMatcher.matches() && current != null) {
                String letter = optionMatcher.group(1).toUpperCase();
                current.addOption(letter, optionMatcher.group(2).trim());
            }
        }

        return questions;
    }

    private Map<Integer, String> parseAnswerKey(List<String> keyLines) {
        Map<Integer, String> result = new LinkedHashMap<>();

        List<String> tableRows = new ArrayList<>();
        for (String rawLine : keyLines) {
            String line = rawLine.trim();
            if (line.startsWith("|")) {
                tableRows.add(line);
            }
        }

        boolean headerSkipped = false;
        for (String row : tableRows) {
            if (TABLE_SEPARATOR_PATTERN.matcher(row).matches()) {
                continue;
            }
            if (!headerSkipped) {
                // first non-separator row is the "№ | Ответ | № | Ответ ..." header — skip it
                headerSkipped = true;
                continue;
            }

            List<String> cells = splitTableRow(row);
            for (int i = 0; i + 1 < cells.size(); i += 2) {
                String numberCell = cells.get(i).trim();
                String answerCell = cells.get(i + 1).trim();
                if (numberCell.isEmpty() || answerCell.isEmpty()) {
                    continue;
                }
                Integer number = tryParseInt(numberCell);
                if (number == null) {
                    continue;
                }
                result.put(number, answerCell.toUpperCase());
            }
        }

        return result;
    }

    private List<String> splitTableRow(String row) {
        String trimmed = row.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] parts = trimmed.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    private Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
