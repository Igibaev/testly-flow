import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { searchAttempts } from '../../api/admin';

export default function AdminAttemptsPage() {
  const [team, setTeam] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    searchAttempts({ team: team || undefined, page })
      .then(setData)
      .catch((e) => setError(e.message));
  }, [team, page]);

  const handleFilterChange = (setter) => (e) => {
    setter(e.target.value);
    setPage(0);
  };

  return (
    <div>
      <h1>Попытки прохождения</h1>

      <div className="filters-row">
        <input placeholder="Команда" value={team} onChange={handleFilterChange(setTeam)} />
      </div>

      {error && <div className="error-box">{error}</div>}

      {!data && !error && <p className="muted">Загрузка…</p>}

      {data && (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Участник</th>
                <th>Команда</th>
                <th>Статус</th>
                <th>Балл</th>
                <th>Начало</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((a) => (
                <tr key={a.id} className="clickable-row">
                  <td>
                    <Link to={`/admin/attempts/${a.id}`}>
                      {a.lastName} {a.firstName}
                    </Link>
                  </td>
                  <td>{a.team}</td>
                  <td>{a.status === 'COMPLETED' ? 'Завершена' : 'В процессе'}</td>
                  <td>{a.scorePercent != null ? `${a.scorePercent}%` : '—'}</td>
                  <td>{new Date(a.startedAt).toLocaleString()}</td>
                  <td>{a.timingSuspicious && <span title="Тайминги вызывают сомнение">⚠</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {data.content.length === 0 && <p className="muted">Попыток не найдено.</p>}

          <div className="pagination">
            <button className="btn-secondary btn" disabled={page === 0} onClick={() => setPage(page - 1)}>
              ← Назад
            </button>
            <span className="muted">
              Стр. {page + 1} из {Math.max(data.totalPages, 1)}
            </span>
            <button
              className="btn-secondary btn"
              disabled={page + 1 >= data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Вперёд →
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
