import { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

const TIER_LABELS = {
  GROWTH: 'Точка роста',
  SOLID: 'Уверенная база',
  STRONG: 'Сильный результат',
  EXEMPLARY: 'Отличный результат',
};

export default function ResultPage() {
  useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const result = location.state?.result;
  const [showAll, setShowAll] = useState(false);

  if (!result) {
    return (
      <div className="state-empty">
        <p>Результат недоступен — возможно, страница была перезагружена.</p>
        <button type="button" className="btn btn-primary" onClick={() => navigate('/')}>
          На главную
        </button>
      </div>
    );
  }

  const wrongAnswers = result.details.filter((d) => !d.isCorrect);
  const visibleAnswers = showAll ? result.details : wrongAnswers;

  return (
    <div className="result-page">
      <section className="result-hero">
        <p className="result-tier-label">{TIER_LABELS[result.resultTier] ?? result.resultTier}</p>
        <h1>{result.headline}</h1>
        <p className="result-score">
          {result.correctCount} <span>из {result.totalQuestions}</span>
        </p>
        <p className="result-message">{result.message}</p>
      </section>

      {result.focusAreas?.length > 0 && (
        <section className="result-focus">
          <h2>На что посмотреть</h2>
          <div className="focus-area-grid">
            {result.focusAreas.map((area) => (
              <article className="focus-area-card" key={area.categoryId}>
                <h3>{area.categoryName}</h3>
                <p>
                  {area.correctRate}% верных · {area.wrongCount} {pluralizeMistake(area.wrongCount)}
                </p>
                {area.prepLinks?.length > 0 && (
                  <ul>
                    {area.prepLinks.map((link) => (
                      <li key={link.id}>
                        <a href={link.url} target="_blank" rel="noreferrer">
                          {link.title}
                        </a>
                      </li>
                    ))}
                  </ul>
                )}
              </article>
            ))}
          </div>
        </section>
      )}

      <section className="result-details">
        <div className="result-details-header">
          <h2>{showAll ? 'Все ответы' : 'Разбор ошибок'}</h2>
          {wrongAnswers.length > 0 && wrongAnswers.length < result.details.length && (
            <button type="button" className="btn btn-ghost" onClick={() => setShowAll((v) => !v)}>
              {showAll ? 'Показать только ошибки' : `Показать все ответы (${result.details.length})`}
            </button>
          )}
        </div>

        {wrongAnswers.length === 0 && !showAll && (
          <p className="result-no-mistakes">Ошибок нет — можно посмотреть все ответы целиком.</p>
        )}

        <ul className="answer-review-list">
          {visibleAnswers.map((d) => (
            <li key={d.questionId} className={`answer-review-item${d.isCorrect ? '' : ' answer-review-item-wrong'}`}>
              <p className="answer-review-category">{d.categoryName}</p>
              <p className="answer-review-question">{d.questionText}</p>
              {!d.isCorrect && (
                <div className="answer-review-comparison">
                  <p>Твой ответ: {d.selectedOption ?? '— не выбран'}</p>
                  <p>Верный ответ: {d.correctOption}</p>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>

      <div className="result-actions">
        <button type="button" className="btn btn-primary btn-large" onClick={() => navigate('/')}>
          Пройти ещё раз
        </button>
      </div>
    </div>
  );
}

function pluralizeMistake(n) {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'ошибка';
  if ([2, 3, 4].includes(mod10) && ![12, 13, 14].includes(mod100)) return 'ошибки';
  return 'ошибок';
}
