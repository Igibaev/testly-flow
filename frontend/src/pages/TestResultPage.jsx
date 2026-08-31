import { useEffect } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';

export default function TestResultPage() {
  const { testId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const { result, test } = location.state || {};

  useEffect(() => {
    if (!result) {
      navigate(`/test/${testId}`, { replace: true });
    }
  }, [result, testId, navigate]);

  if (!result) {
    return null;
  }

  return (
    <div>
      <div className="card result-summary">
        <h1>{test?.title}</h1>
        <div className="result-score">
          {result.correctCount} / {result.totalQuestions}
        </div>
        <p className="muted">Результат: {result.scorePercent}%</p>
        <Link to="/" className="btn" style={{ marginTop: 16 }}>
          К списку тестов
        </Link>
      </div>

      <div className="card">
        <h2>Детализация ответов</h2>
        {result.details.map((detail) => (
          <div className="answer-row" key={detail.questionId}>
            <div>
              <div className="question-number">Вопрос {detail.number}</div>
              <div>{detail.questionText}</div>
              <div className="muted">
                Ваш ответ: {detail.selectedOption || '—'} · Правильный: {detail.correctOption}
              </div>
            </div>
            <span className={`badge ${detail.isCorrect ? 'badge-correct' : 'badge-incorrect'}`}>
              {detail.isCorrect ? 'Верно' : 'Неверно'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
