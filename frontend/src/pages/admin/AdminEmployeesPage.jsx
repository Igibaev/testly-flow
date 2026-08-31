import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listEmployees } from '../../api/admin';

export default function AdminEmployeesPage() {
  const [employees, setEmployees] = useState(null);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');

  useEffect(() => {
    listEmployees()
      .then(setEmployees)
      .catch((e) => setError(e.message));
  }, []);

  const filtered = employees
    ? employees.filter((e) => {
        const haystack = `${e.firstName} ${e.lastName} ${e.team}`.toLowerCase();
        return haystack.includes(search.toLowerCase());
      })
    : null;

  return (
    <div>
      <h1>Сотрудники</h1>
      <p className="muted">
        Отсортировано по среднему времени на вопрос — быстрее всех наверху. Учитываются только завершённые попытки
        без подозрительных таймингов.
      </p>

      <div className="filters-row">
        <input placeholder="Поиск по имени или команде" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {error && <div className="error-box">{error}</div>}
      {!employees && !error && <p className="muted">Загрузка…</p>}

      {filtered && (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Сотрудник</th>
                <th>Команда</th>
                <th>Попыток</th>
                <th>Завершено</th>
                <th>Ср. балл</th>
                <th>Ср. время / вопрос</th>
                <th>Последняя попытка</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((e) => (
                <tr key={`${e.firstName}|${e.lastName}|${e.team}`} className="clickable-row">
                  <td>
                    <Link to={`/admin/employees/${encodeURIComponent(e.firstName)}/${encodeURIComponent(e.lastName)}/${encodeURIComponent(e.team)}`}>
                      {e.lastName} {e.firstName}
                    </Link>
                  </td>
                  <td>{e.team}</td>
                  <td>{e.attemptsCount}</td>
                  <td>{e.completedCount}</td>
                  <td>{e.avgScorePercent != null ? `${e.avgScorePercent}%` : '—'}</td>
                  <td>{e.avgTimePerQuestionSeconds != null ? `${e.avgTimePerQuestionSeconds} с` : '—'}</td>
                  <td>{e.lastAttemptAt ? new Date(e.lastAttemptAt).toLocaleString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {filtered.length === 0 && <p className="muted">Никого не найдено.</p>}
        </div>
      )}
    </div>
  );
}
