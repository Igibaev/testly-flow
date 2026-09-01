package com.testlyflow.ui.support;

public final class RussianPlurals {

    private RussianPlurals() {
    }

    public static String form(int n, String one, String few, String many) {
        int abs = Math.abs(n) % 100;
        int d = abs % 10;
        if (abs > 10 && abs < 20) {
            return many;
        }
        if (d == 1) {
            return one;
        }
        if (d >= 2 && d <= 4) {
            return few;
        }
        return many;
    }

    public static String questions(int n) {
        return form(n, "вопрос", "вопроса", "вопросов");
    }

    public static String blocksGenitive(int n) {
        // Matches the original home-page copy: «из N блока/блоков».
        return form(n, "блока", "блоков", "блоков");
    }

    public static String mistakes(int n) {
        return form(n, "ошибка", "ошибки", "ошибок");
    }
}
