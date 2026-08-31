import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getAttemptDetail } from '../../api/admin';

export default function AdminAttemptDetailPage() {
  const { attemptId } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    getAttemptDetail(attemptId)
      .then(setData)
      .catch((e) => setError(e.message));
  }, [attemptId]);

  if (error) {
    return <div className="error-box">{error}</div>;
  }

  if (!data) {
    return <p className="muted">Загрузка…</p>;
  }

  const { summary, answers } = data;

  return (
    <div>
      <Link to="/admin/attempts" className="muted">
        ← К списку попыток
      </Link>
      <h1>
        {summary.lastName} {summary.firstName} — {summary.team}
      </h1>

      <div className="card">
        <p>
          <strong>Тест:</strong> {summary.testTitle}
        </p>
        <p>
          <strong>Статус:</strong> {summary.status === 'COMPLETED' ? 'Завершена' : 'В процессе'}
        </p>
        <p>
          <strong>Результат:</strong>{' '}
          {summary.correctCount != null ? `${summary.correctCount} / ${summary.totalQuestions} (${summary.scorePercent}%)` : '—'}
        </p>
        <p>
          <strong>Начало:</strong> {new Date(summary.startedAt).toLocaleString()}
        </p>
        <p>
          <strong>Завершение:</strong> {summary.finishedAt ? new Date(summary.finishedAt).toLocaleString() : '—'}
        </p>
        <p>
          <strong>IP-адрес:</strong> {summary.ipAddress || '—'}
        </p>
        <p>
          <strong>User-Agent:</strong> {summary.userAgent || '—'}
        </p>
      </div>

      <div className="card">
        <h2>Детализация ответов</h2>
        {answers.map((a) => (
          <div className="answer-row" key={a.questionId}>
            <div>
              <div className="question-number">Вопрос {a.number}</div>
              <div>{a.questionText}</div>
              <div className="muted">
                Ответ: {a.selectedOption || '—'} · Правильный: {a.correctOption}
              </div>
            </div>
            <span className={`badge ${a.isCorrect ? 'badge-correct' : 'badge-incorrect'}`}>
              {a.isCorrect ? 'Верно' : 'Неверно'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
