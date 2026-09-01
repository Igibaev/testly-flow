package com.testlyflow.ui.view;

import com.testlyflow.dto.AnswerSubmission;
import com.testlyflow.dto.AnswerUpdateRequest;
import com.testlyflow.dto.AttemptQuestionDto;
import com.testlyflow.dto.AttemptStateDto;
import com.testlyflow.dto.SavedAnswerDto;
import com.testlyflow.dto.SubmitAttemptRequest;
import com.testlyflow.dto.SubmitAttemptResponse;
import com.testlyflow.dto.TimingSubmission;
import com.testlyflow.exception.ConflictException;
import com.testlyflow.service.AttemptService;
import com.testlyflow.ui.MainLayout;
import com.testlyflow.ui.component.FinishDialog;
import com.testlyflow.ui.component.QuestionNavigator;
import com.testlyflow.ui.component.QuestionPanel;
import com.testlyflow.ui.support.NativeUi;
import com.testlyflow.ui.support.QuestionTimer;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Route(value = "attempt/:attemptId", layout = MainLayout.class)
@PageTitle("Прохождение теста")
public class AttemptView extends Div implements BeforeEnterObserver {

    public static final String RESULT_SESSION_PREFIX = "result:";

    private static final List<String> OPTION_KEYS = List.of(
            "1", "2", "3", "4", "5", "6",
            "А", "Б", "В", "Г", "Д", "Е",
            "A", "B", "C", "D", "E", "F");

    private final AttemptService attemptService;
    private final QuestionTimer timer = new QuestionTimer();

    private Long attemptId;
    private List<AttemptQuestionDto> questions = List.of();
    private final Map<Long, String> answers = new HashMap<>();
    private final Set<Integer> visited = new HashSet<>();
    private int currentIndex;
    private boolean navigatorOpen;
    private boolean confirmOpen;
    private boolean submitting;
    private String submitError;
    private String loadError;
    private boolean completedAttempt;

    public AttemptView(AttemptService attemptService) {
        this.attemptService = attemptService;
        addClassName("attempt-page");
        setWidthFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        load(event.getRouteParameters().get("attemptId").orElse(null));
    }

    public void load(String rawId) {
        try {
            attemptId = Long.valueOf(rawId);
        } catch (RuntimeException e) {
            loadError = "Не удалось загрузить попытку: некорректный идентификатор";
            rebuild();
            return;
        }
        try {
            AttemptStateDto state = attemptService.getAttemptState(attemptId);
            questions = state.questions() == null ? List.of() : state.questions();
            answers.clear();
            for (AttemptQuestionDto q : questions) {
                answers.put(q.questionId(), null);
            }
            if (state.answers() != null) {
                for (SavedAnswerDto saved : state.answers()) {
                    answers.put(saved.questionId(), saved.selectedOption());
                    timer.seed(saved.questionId(), saved.timeSpentMs());
                }
            }
            currentIndex = 0;
            visited.clear();
            visited.add(0);
            loadError = null;
            completedAttempt = false;
        } catch (ConflictException e) {
            completedAttempt = true;
            loadError = "Эта попытка уже завершена.";
        } catch (RuntimeException e) {
            loadError = "Не удалось загрузить попытку: " + e.getMessage();
        }
        rebuild();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.addHeartbeatListener(event -> timer.noteActivity());
        // The only JS in the project: pause the question timer when the tab is hidden.
        // Server-side code cannot observe document.visibilityState on its own.
        getElement().executeJs(
                "const el=this;document.addEventListener('visibilitychange',()=>{"
                        + "el.$server.visibilityChanged(document.visibilityState==='hidden');});");
        registerShortcuts();
        if (currentQuestion() != null) {
            timer.setActiveQuestion(currentQuestion().questionId());
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        timer.freezeAtLastActivity();
        AttemptQuestionDto current = currentQuestion();
        if (attemptId != null && current != null) {
            try {
                attemptService.updateAnswer(attemptId, current.questionId(),
                        new AnswerUpdateRequest(answers.get(current.questionId()),
                                timer.accumulatedMs(current.questionId())));
            } catch (RuntimeException ignored) {
                // best-effort flush of the last known interval
            }
        }
        super.onDetach(detachEvent);
    }

    @ClientCallable
    public void visibilityChanged(boolean hidden) {
        if (hidden) {
            timer.pause();
            flushCurrent();
        } else {
            timer.resume();
        }
    }

    private void registerShortcuts() {
        Shortcuts.addShortcutListener(this, this::goNext, Key.ARROW_RIGHT).listenOn(this);
        Shortcuts.addShortcutListener(this, this::goNext, Key.ENTER).listenOn(this);
        Shortcuts.addShortcutListener(this, this::goPrev, Key.ARROW_LEFT).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("1"), Key.DIGIT_1).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("2"), Key.DIGIT_2).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("3"), Key.DIGIT_3).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("4"), Key.DIGIT_4).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("5"), Key.DIGIT_5).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("6"), Key.DIGIT_6).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("A"), Key.KEY_A).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("B"), Key.KEY_B).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("C"), Key.KEY_C).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("D"), Key.KEY_D).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("E"), Key.KEY_E).listenOn(this);
        Shortcuts.addShortcutListener(this, () -> selectByKey("F"), Key.KEY_F).listenOn(this);
    }

    private void selectByKey(String key) {
        if (shortcutsBlocked()) {
            return;
        }
        int idx = OPTION_KEYS.indexOf(key);
        if (idx < 0) {
            idx = OPTION_KEYS.indexOf(key.toUpperCase());
        }
        if (idx < 0) {
            return;
        }
        AttemptQuestionDto current = currentQuestion();
        if (current == null) {
            return;
        }
        int optionIndex = idx % 6;
        if (optionIndex < current.options().size()) {
            selectOption(current.options().get(optionIndex).letter());
        }
    }

    private boolean shortcutsBlocked() {
        return confirmOpen;
    }

    private AttemptQuestionDto currentQuestion() {
        if (currentIndex < 0 || currentIndex >= questions.size()) {
            return null;
        }
        return questions.get(currentIndex);
    }

    private void selectOption(String letter) {
        AttemptQuestionDto current = currentQuestion();
        if (current == null) {
            return;
        }
        String existing = answers.get(current.questionId());
        String next = letter.equals(existing) ? null : letter;
        answers.put(current.questionId(), next);
        flushCurrent();
        rebuild();
    }

    private void goTo(int index) {
        if (index < 0 || index >= questions.size() || index == currentIndex) {
            return;
        }
        flushCurrent();
        currentIndex = index;
        visited.add(index);
        AttemptQuestionDto current = currentQuestion();
        if (current != null) {
            timer.setActiveQuestion(current.questionId());
        }
        rebuild();
    }

    private void goNext() {
        if (shortcutsBlocked()) {
            return;
        }
        goTo(currentIndex + 1);
    }

    private void goPrev() {
        if (shortcutsBlocked()) {
            return;
        }
        goTo(currentIndex - 1);
    }

    private void flushCurrent() {
        AttemptQuestionDto current = currentQuestion();
        if (current == null || attemptId == null) {
            return;
        }
        timer.noteActivity();
        long ms = timer.accumulatedMs(current.questionId());
        try {
            attemptService.updateAnswer(attemptId, current.questionId(),
                    new AnswerUpdateRequest(answers.get(current.questionId()), ms));
        } catch (RuntimeException ignored) {
            // autosave failures are surfaced softly — the final sync on submit re-sends everything
        }
    }

    private void openConfirm() {
        flushCurrent();
        confirmOpen = true;
        submitError = null;
        rebuild();
    }

    private void doSubmit() {
        if (submitting) {
            return;
        }
        submitting = true;
        submitError = null;
        flushCurrent();
        AttemptQuestionDto current = currentQuestion();
        List<AnswerSubmission> finalAnswers = new ArrayList<>();
        List<TimingSubmission> finalTimings = new ArrayList<>();
        for (AttemptQuestionDto q : questions) {
            finalAnswers.add(new AnswerSubmission(q.questionId(), answers.get(q.questionId())));
            long ms = (current != null && q.questionId().equals(current.questionId()))
                    ? timer.accumulatedMs(q.questionId())
                    : timer.accumulatedMs(q.questionId());
            finalTimings.add(new TimingSubmission(q.questionId(), ms));
        }
        try {
            SubmitAttemptResponse result = attemptService.submitAttempt(
                    attemptId, new SubmitAttemptRequest(finalAnswers, finalTimings));
            VaadinSession.getCurrent().setAttribute(RESULT_SESSION_PREFIX + attemptId, result);
            getUI().ifPresent(ui -> ui.navigate(ResultView.class,
                    new RouteParameters("attemptId", String.valueOf(attemptId))));
        } catch (RuntimeException e) {
            submitError = e.getMessage();
            submitting = false;
            confirmOpen = false;
            rebuild();
        }
    }

    private int answeredCount() {
        int count = 0;
        for (AttemptQuestionDto q : questions) {
            String selected = answers.get(q.questionId());
            if (selected != null && !selected.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private int firstUnansweredIndex() {
        for (int i = 0; i < questions.size(); i++) {
            String selected = answers.get(questions.get(i).questionId());
            if (selected == null || selected.isBlank()) {
                return i;
            }
        }
        return -1;
    }

    public void rebuild() {
        removeAll();
        if (loadError != null) {
            Div box = new Div();
            box.addClassName("state-error");
            box.addClassName("attempt-load-error");
            box.add(new Paragraph(loadError));
            box.add(NativeUi.button("На главную", e -> getUI().ifPresent(ui -> ui.navigate(HomeView.class)),
                    "btn", "btn-secondary"));
            add(box);
            return;
        }
        AttemptQuestionDto current = currentQuestion();
        if (current == null) {
            Div loading = new Div();
            loading.addClassName("state-loading");
            loading.getElement().setAttribute("aria-live", "polite");
            loading.setText("Загружаем вопросы…");
            add(loading);
            return;
        }

        Span live = new Span("Вопрос " + (currentIndex + 1) + " из " + questions.size());
        live.addClassName("visually-hidden");
        live.getElement().setAttribute("aria-live", "polite");
        add(live);

        Div header = new Div();
        header.addClassName("attempt-header");
        Div left = new Div();
        Paragraph count = new Paragraph("Вопрос " + (currentIndex + 1) + " из " + questions.size());
        count.addClassName("attempt-question-count");
        Span pill = new Span(current.categoryName());
        pill.addClassName("category-pill");
        List<QuestionNavigator.CategoryGroup> groups = QuestionNavigator.groupByCategory(questions);
        String color = groups.stream()
                .filter(g -> g.categoryId.equals(current.categoryId()))
                .map(g -> g.color)
                .findFirst()
                .orElse("var(--color-accent)");
        pill.getStyle().set("--cat-accent", color);
        left.add(count, pill);
        String toggleLabel = navigatorOpen ? "Скрыть список вопросов" : "Все вопросы";
        header.add(left, NativeUi.button(toggleLabel, e -> {
            navigatorOpen = !navigatorOpen;
            rebuild();
        }, "btn", "btn-ghost", "navigator-toggle"));
        add(header);

        int answered = answeredCount();
        Div progress = new Div();
        progress.addClassName("progress-bar");
        progress.getElement().setAttribute("role", "progressbar");
        progress.getElement().setAttribute("aria-valuenow", String.valueOf(answered));
        progress.getElement().setAttribute("aria-valuemin", "0");
        progress.getElement().setAttribute("aria-valuemax", String.valueOf(questions.size()));
        Div fill = new Div();
        fill.addClassName("progress-bar-fill");
        double pct = questions.isEmpty() ? 0 : (answered * 100.0 / questions.size());
        fill.getStyle().set("width", pct + "%");
        progress.add(fill);
        Paragraph label = new Paragraph("Отвечено " + answered + " из " + questions.size());
        label.addClassName("progress-label");
        add(progress, label);

        Div body = new Div();
        body.addClassName("attempt-body");
        body.add(new QuestionPanel(
                current,
                answers.get(current.questionId()),
                currentIndex == 0,
                currentIndex == questions.size() - 1,
                this::selectOption,
                this::goPrev,
                this::goNext,
                this::openConfirm));
        body.add(new QuestionNavigator(questions, answers, visited, currentIndex, navigatorOpen, this::goTo));
        add(body);

        if (confirmOpen) {
            add(new FinishDialog(questions, answers, submitting, submitError,
                    () -> {
                        confirmOpen = false;
                        int idx = firstUnansweredIndex();
                        if (idx >= 0) {
                            goTo(idx);
                        } else {
                            rebuild();
                        }
                    },
                    () -> {
                        confirmOpen = false;
                        rebuild();
                    },
                    this::doSubmit));
        }
    }
}
