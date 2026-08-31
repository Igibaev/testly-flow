import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getEmployeeCard } from '../../api/admin';

export default function AdminEmployeeCardPage() {
  const { firstName, lastName, team } = useParams();
  const [card, setCard] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setCard(null);
    setError(null);
    getEmployeeCard(firstName, lastName, team)
      .then(setCard)
      .catch((e) => setError(e.message));
  }, [firstName, lastName, team]);

  if (error) {
    return <div className="error-box">{error}</div>;
  }

  if (!card) {
    return <p className="muted">Загрузка…</p>;
  }

  return (
    <div>
      <Link to="/admin/employees" className="muted">
        ← К списку сотрудников
      </Link>
      <h1>
        {card.lastName} {card.firstName} — {card.team}
      </h1>

      <div className="metrics-grid">
        <div className="metric-tile">
          <div className="value">{card.attempts.length}</div>
          <div className="label">Попыток</div>
        </div>
        <div className="metric-tile">
          <div className="value">{card.attempts.filter((a) => a.status === 'COMPLETED').length}</div>
          <div className="label">Завершено</div>
        </div>
        <div className="metric-tile">
          <div className="value">{card.avgTimePerQuestionSeconds != null ? `${card.avgTimePerQuestionSeconds} с` : '—'}</div>
          <div className="label">Ср. время на вопрос</div>
        </div>
      </div>

      <div className="card">
        <h2>Время по вопросам</h2>
        <p className="muted">
          Сравнение со средним временем всех сотрудников на этот же вопрос (без порога минимума наблюдений — это
          справочная база для сравнения, а не рейтинг вопросов).
        </p>
        {card.questionTimings.length === 0 && (
          <p className="muted">У этого сотрудника пока нет завершённых ответов с таймингом.</p>
        )}
        {card.questionTimings.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Вопрос</th>
                <th>Категория</th>
                <th>Время сотрудника</th>
                <th>В среднем у всех</th>
                <th>Разница</th>
                <th>% верных у сотрудника</th>
              </tr>
            </thead>
            <tbody>
              {card.questionTimings.map((q) => (
                <tr key={q.questionId}>
                  <td title={q.text}>№{q.number}</td>
                  <td>{q.categoryName}</td>
                  <td>
                    {q.employeeAvgSeconds} с {q.employeeSamples > 1 ? `(×${q.employeeSamples})` : ''}
                  </td>
                  <td>
                    {q.globalAvgSeconds != null ? `${q.globalAvgSeconds} с (n=${q.globalSamples})` : '—'}
                  </td>
                  <td>{renderDelta(q)}</td>
                  <td>{q.employeeCorrectRate}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h2>Попытки</h2>
        <table>
          <thead>
            <tr>
              <th>Начало</th>
              <th>Статус</th>
              <th>Балл</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {card.attempts.map((a) => (
              <tr key={a.id} className="clickable-row">
                <td>{new Date(a.startedAt).toLocaleString()}</td>
                <td>{a.status === 'COMPLETED' ? 'Завершена' : 'В процессе'}</td>
                <td>{a.scorePercent != null ? `${a.scorePercent}%` : '—'}</td>
                <td>
                  <Link to={`/admin/attempts/${a.id}`}>Детали</Link>
                  {a.timingSuspicious && <span title="Тайминги вызывают сомнение"> ⚠</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function renderDelta(q) {
  if (q.globalAvgSeconds == null || q.globalAvgSeconds === 0) return '—';
  const diffPercent = Math.round(((q.employeeAvgSeconds - q.globalAvgSeconds) / q.globalAvgSeconds) * 100);
  if (Math.abs(diffPercent) < 5) return 'как у всех';
  return diffPercent < 0 ? `быстрее на ${Math.abs(diffPercent)}%` : `медленнее на ${diffPercent}%`;
}
