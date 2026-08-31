import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { getAttemptState, submitAttempt, updateAnswer } from '../api/attempts';
import { putAnswerKeepalive } from '../api/client';
import { useQuestionTimer } from '../hooks/useQuestionTimer';

const OPTION_KEYS = ['1', '2', '3', '4', '5', '6', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'A', 'B', 'C', 'D', 'E', 'F'];

export default function AttemptPage() {
  const { attemptId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const timer = useQuestionTimer();

  const [attempt, setAttempt] = useState(location.state?.startData ?? null);
  const [loadError, setLoadError] = useState(null);
  const [answers, setAnswers] = useState(() => initialAnswers(location.state?.startData));
  const [visited, setVisited] = useState(() => new Set([0]));
  const [currentIndex, setCurrentIndex] = useState(0);
  const [navigatorOpen, setNavigatorOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const liveRegionRef = useRef(null);

  useEffect(() => {
    if (attempt) {
      return;
    }
    getAttemptState(attemptId)
      .then((data) => {
        setAttempt(data);
        setAnswers(initialAnswers(data));
      })
      .catch((e) => setLoadError(e));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attemptId]);

  const questions = attempt?.questions ?? [];
  const currentQuestion = questions[currentIndex];

  useEffect(() => {
    if (!currentQuestion) return;
    timer.setActiveQuestion(currentQuestion.questionId);
    setVisited((prev) => new Set(prev).add(currentIndex));
    if (liveRegionRef.current) {
      liveRegionRef.current.textContent = `Вопрос ${currentIndex + 1} из ${questions.length}`;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentQuestion?.questionId]);

  // Flush timing right before the tab actually closes.
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (!currentQuestion) return;
      const ms = timer.getAccumulatedMs(currentQuestion.questionId);
      const current = answers[currentQuestion.questionId];
      putAnswerKeepalive(attemptId, currentQuestion.questionId, {
        selectedOption: current?.selectedOption ?? null,
        timeSpentMs: ms,
      });
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attemptId, currentQuestion, answers]);

  const answeredCount = useMemo(
    () => Object.values(answers).filter((a) => a.selectedOption).length,
    [answers]
  );

  const flushCurrent = useCallback(
    (selectedOverride) => {
      if (!currentQuestion) return;
      const ms = timer.getAccumulatedMs(currentQuestion.questionId);
      const selectedOption = selectedOverride !== undefined ? selectedOverride : answers[currentQuestion.questionId]?.selectedOption ?? null;
      setAnswers((prev) => ({
        ...prev,
        [currentQuestion.questionId]: { selectedOption, timeSpentMs: ms },
      }));
      return updateAnswer(attemptId, currentQuestion.questionId, { selectedOption, timeSpentMs: ms }).catch(() => {
        // autosave failures are surfaced softly -- the final sync on submit re-sends everything
      });
    },
    [attemptId, answers, currentQuestion, timer]
  );

  function selectOption(letter) {
    if (!currentQuestion) return;
    const isSame = answers[currentQuestion.questionId]?.selectedOption === letter;
    flushCurrent(isSame ? null : letter);
  }

  function goTo(index) {
    if (index < 0 || index >= questions.length || index === currentIndex) return;
    flushCurrent();
    setCurrentIndex(index);
  }

  function goNext() {
    goTo(currentIndex + 1);
  }

  function goPrev() {
    goTo(currentIndex - 1);
  }

  const firstUnansweredIndex = useMemo(
    () => questions.findIndex((q) => !answers[q.questionId]?.selectedOption),
    [questions, answers]
  );

  function openConfirm() {
    flushCurrent();
    setConfirmOpen(true);
  }

  async function doSubmit() {
    setSubmitting(true);
    setSubmitError(null);
    flushCurrent();
    const finalAnswers = questions.map((q) => ({
      questionId: q.questionId,
      selectedOption: answers[q.questionId]?.selectedOption ?? null,
    }));
    const finalTimings = questions.map((q) => ({
      questionId: q.questionId,
      timeSpentMs: q.questionId === currentQuestion?.questionId
        ? timer.getAccumulatedMs(q.questionId)
        : answers[q.questionId]?.timeSpentMs ?? 0,
    }));
    try {
      const result = await submitAttempt(attemptId, { answers: finalAnswers, timings: finalTimings });
      navigate(`/attempt/${attemptId}/result`, { state: { result } });
    } catch (e) {
      setSubmitError(e.message);
      setSubmitting(false);
      setConfirmOpen(false);
    }
  }

  useEffect(() => {
    function handleKeyDown(e) {
      if (confirmOpen) return;
      const tag = document.activeElement?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA') return;

      if (e.key === 'ArrowRight') {
        goNext();
      } else if (e.key === 'ArrowLeft') {
        goPrev();
      } else if (e.key === 'Enter') {
        goNext();
      } else {
        const idx = OPTION_KEYS.indexOf(e.key);
        if (idx >= 0 && currentQuestion) {
          const optionIndex = idx % 6;
          const opt = currentQuestion.options[optionIndex];
          if (opt) selectOption(opt.letter);
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentIndex, currentQuestion, answers, confirmOpen]);

  if (loadError) {
    return (
      <div className="state-error attempt-load-error">
        <p>{loadError.status === 409 ? 'Эта попытка уже завершена.' : `Не удалось загрузить попытку: ${loadError.message}`}</p>
        <button type="button" className="btn btn-secondary" onClick={() => navigate('/')}>
          На главную
        </button>
      </div>
    );
  }

  if (!attempt || !currentQuestion) {
    return <div className="state-loading" aria-live="polite">Загружаем вопросы…</div>;
  }

  const categoryGroups = groupByCategory(questions);

  return (
    <div className="attempt-page">
      <div className="visually-hidden" aria-live="polite" ref={liveRegionRef} />

      <div className="attempt-header">
        <div>
          <p className="attempt-question-count">
            Вопрос {currentIndex + 1} из {questions.length}
          </p>
          <span className="category-pill" style={{ '--cat-accent': categoryColor(categoryGroups, currentQuestion.categoryId) }}>
            {currentQuestion.categoryName}
          </span>
        </div>
        <button type="button" className="btn btn-ghost navigator-toggle" onClick={() => setNavigatorOpen((v) => !v)}>
          {navigatorOpen ? 'Скрыть список вопросов' : 'Все вопросы'}
        </button>
      </div>

      <div className="progress-bar" role="progressbar" aria-valuenow={answeredCount} aria-valuemin={0} aria-valuemax={questions.length}>
        <div className="progress-bar-fill" style={{ width: `${(answeredCount / questions.length) * 100}%` }} />
      </div>
      <p className="progress-label">
        Отвечено {answeredCount} из {questions.length}
      </p>

      <div className="attempt-body">
        <section className="question-panel" aria-labelledby="question-text">
          <fieldset>
            <legend id="question-text" className="question-text">
              {currentQuestion.text}
            </legend>
            <div className="option-list">
              {currentQuestion.options.map((opt) => {
                const selected = answers[currentQuestion.questionId]?.selectedOption === opt.letter;
                return (
                  <button
                    key={opt.letter}
                    type="button"
                    className={`option-card${selected ? ' option-card-selected' : ''}`}
                    onClick={() => selectOption(opt.letter)}
                    aria-pressed={selected}
                  >
                    <span className="option-letter">{opt.letter}</span>
                    <span className="option-text">{opt.text}</span>
                  </button>
                );
              })}
            </div>
          </fieldset>

          <div className="attempt-nav-buttons">
            <button type="button" className="btn btn-secondary" onClick={goPrev} disabled={currentIndex === 0}>
              ← Назад
            </button>
            <button type="button" className="btn btn-secondary" onClick={goNext} disabled={currentIndex === questions.length - 1}>
              Далее →
            </button>
            <button type="button" className="btn btn-finish" onClick={openConfirm}>
              Завершить тест
            </button>
          </div>
        </section>

        <aside className={`question-navigator${navigatorOpen ? ' question-navigator-open' : ''}`} aria-label="Список вопросов">
          {categoryGroups.map((group) => (
            <div className="navigator-group" key={group.categoryId}>
              <h4 style={{ '--cat-accent': group.color }}>{group.categoryName}</h4>
              <div className="navigator-grid">
                {group.items.map((item) => {
                  const isAnswered = !!answers[item.questionId]?.selectedOption;
                  const isVisited = visited.has(item.index);
                  const isCurrent = item.index === currentIndex;
                  const status = isCurrent ? 'current' : isAnswered ? 'answered' : isVisited ? 'skipped' : 'unvisited';
                  return (
                    <button
                      key={item.questionId}
                      type="button"
                      className={`nav-cell nav-cell-${status}`}
                      onClick={() => {
                        goTo(item.index);
                        setNavigatorOpen(false);
                      }}
                      aria-current={isCurrent ? 'true' : undefined}
                      title={statusLabel(status)}
                    >
                      {item.displayNumber}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
          <ul className="navigator-legend">
            <li className="nav-cell-answered">Отвечен</li>
            <li className="nav-cell-skipped">Пропущен, посещён</li>
            <li className="nav-cell-unvisited">Не посещён</li>
            <li className="nav-cell-current">Текущий</li>
          </ul>
        </aside>
      </div>

      {confirmOpen && (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
          <div className="modal">
            <h3 id="confirm-title">Завершить тест?</h3>
            {questions.length - answeredCount > 0 ? (
              <>
                <p>
                  Без ответа осталось {questions.length - answeredCount}{' '}
                  {pluralize(questions.length - answeredCount)}:{' '}
                  {questions
                    .filter((q) => !answers[q.questionId]?.selectedOption)
                    .map((q, i) => (i === 0 ? '' : ', ') + q.displayNumber)
                    .join('')}
                  .
                </p>
                <div className="modal-actions">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => {
                      setConfirmOpen(false);
                      if (firstUnansweredIndex >= 0) goTo(firstUnansweredIndex);
                    }}
                  >
                    Вернуться к первому пропущенному
                  </button>
                  <button type="button" className="btn btn-finish" onClick={doSubmit} disabled={submitting}>
                    {submitting ? 'Завершаем…' : 'Всё равно завершить'}
                  </button>
                </div>
              </>
            ) : (
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setConfirmOpen(false)}>
                  Продолжить
                </button>
                <button type="button" className="btn btn-finish" onClick={doSubmit} disabled={submitting}>
                  {submitting ? 'Завершаем…' : 'Завершить'}
                </button>
              </div>
            )}
            {submitError && <p className="field-error">{submitError}</p>}
          </div>
        </div>
      )}
    </div>
  );
}

function initialAnswers(data) {
  const map = {};
  if (!data) return map;
  for (const q of data.questions) {
    map[q.questionId] = { selectedOption: null, timeSpentMs: 0 };
  }
  for (const a of data.answers ?? []) {
    map[a.questionId] = { selectedOption: a.selectedOption, timeSpentMs: a.timeSpentMs };
  }
  return map;
}

function groupByCategory(questions) {
  const byId = new Map();
  questions.forEach((q, index) => {
    if (!byId.has(q.categoryId)) {
      byId.set(q.categoryId, { categoryId: q.categoryId, categoryName: q.categoryName, color: categoryColorFor(q.categoryId), items: [] });
    }
    byId.get(q.categoryId).items.push({ ...q, index });
  });
  return Array.from(byId.values());
}

const COLOR_PALETTE = ['#6d5dfc', '#0f9d8c', '#c2410c', '#0369a1', '#a21caf', '#65760a'];
function categoryColorFor(categoryId) {
  const idx = Number(categoryId) % COLOR_PALETTE.length;
  return COLOR_PALETTE[idx];
}
function categoryColor(groups, categoryId) {
  return groups.find((g) => g.categoryId === categoryId)?.color ?? 'var(--color-accent)';
}

function statusLabel(status) {
  return { answered: 'Отвечен', skipped: 'Пропущен, посещён', unvisited: 'Не посещён', current: 'Текущий вопрос' }[status];
}

function pluralize(n) {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'вопрос';
  if ([2, 3, 4].includes(mod10) && ![12, 13, 14].includes(mod100)) return 'вопроса';
  return 'вопросов';
}
