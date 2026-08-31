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

  const { summary, answers, slowestQuestion, categoryBreakdown } = data;

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
        {summary.timingSuspicious && (
          <p className="error-box" style={{ background: '#fff8e1', color: '#8a6d00', borderColor: '#f0dca0' }}>
            ⚠ Сумма таймингов по вопросам заметно превышает длительность попытки — тайминги этой попытки исключены из метрик времени.
          </p>
        )}
        <p>
          <strong>IP-адрес:</strong> {summary.ipAddress || '—'}
        </p>
        <p>
          <strong>User-Agent:</strong> {summary.userAgent || '—'}
        </p>
      </div>

      {slowestQuestion && (
        <div className="card">
          <h2>Самый долгий вопрос в попытке</h2>
          <p>
            №{slowestQuestion.number} · {slowestQuestion.categoryName} · {formatMs(slowestQuestion.timeSpentMs)}
          </p>
          <p className="muted">{slowestQuestion.questionText}</p>
        </div>
      )}

      {categoryBreakdown?.length > 0 && (
        <div className="card">
          <h2>Время по блокам</h2>
          {categoryBreakdown.map((c) => (
            <div className="bar-row" key={c.categoryId}>
              <div className="bar-label">{c.categoryName}</div>
              <div className="bar-value">
                {formatMs(c.totalTimeSpentMs)} · {c.questionCount} вопр.
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="card">
        <h2>Детализация ответов</h2>
        {answers.map((a) => (
          <div className="answer-row" key={a.questionId}>
            <div>
              <div className="question-number">
                Вопрос {a.number} · {a.categoryName} · {formatMs(a.timeSpentMs)}
              </div>
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

function formatMs(ms) {
  if (!ms) return '0 с';
  const totalSeconds = Math.round(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes} мин ${seconds} с` : `${seconds} с`;
}
