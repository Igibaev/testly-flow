import { useEffect, useState } from 'react';
import { listAdminTests, getMetrics } from '../../api/admin';

export default function AdminMetricsPage() {
  const [tests, setTests] = useState([]);
  const [testId, setTestId] = useState('');
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    listAdminTests().then(setTests).catch(() => {});
  }, []);

  useEffect(() => {
    getMetrics(testId || undefined)
      .then(setMetrics)
      .catch((e) => setError(e.message));
  }, [testId]);

  const maxBucket = metrics
    ? Math.max(1, ...Object.values(metrics.scoreDistribution))
    : 1;
  const maxTeam = metrics && metrics.teamActivity.length
    ? Math.max(1, ...metrics.teamActivity.map((t) => t.attempts))
    : 1;

  return (
    <div>
      <h1>Метрики</h1>

      <div className="filters-row">
        <select value={testId} onChange={(e) => setTestId(e.target.value)}>
          <option value="">Все тесты</option>
          {tests.map((t) => (
            <option key={t.id} value={t.id}>
              {t.title}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="error-box">{error}</div>}
      {!metrics && !error && <p className="muted">Загрузка…</p>}

      {metrics && (
        <>
          <div className="metrics-grid">
            <div className="metric-tile">
              <div className="value">{metrics.startsCount}</div>
              <div className="label">Стартов теста</div>
            </div>
            <div className="metric-tile">
              <div className="value">{metrics.completedCount}</div>
              <div className="label">Завершено</div>
            </div>
            <div className="metric-tile">
              <div className="value">{metrics.abandonedCount}</div>
              <div className="label">Не завершено</div>
            </div>
            <div className="metric-tile">
              <div className="value">{formatDuration(metrics.averageDurationSeconds)}</div>
              <div className="label">Среднее время</div>
            </div>
          </div>

          <div className="card">
            <h2>Распределение баллов</h2>
            {Object.entries(metrics.scoreDistribution).map(([bucket, count]) => (
              <div className="bar-row" key={bucket}>
                <div className="bar-label">{bucket}%</div>
                <div className="bar-track">
                  <div className="bar-fill" style={{ width: `${(count / maxBucket) * 100}%` }} />
                </div>
                <div className="bar-value">{count}</div>
              </div>
            ))}
          </div>

          <div className="card">
            <h2>Активность по командам</h2>
            {metrics.teamActivity.length === 0 && <p className="muted">Нет данных.</p>}
            {metrics.teamActivity.map((team) => (
              <div className="bar-row" key={team.team}>
                <div className="bar-label">{team.team}</div>
                <div className="bar-track">
                  <div className="bar-fill" style={{ width: `${(team.attempts / maxTeam) * 100}%` }} />
                </div>
                <div className="bar-value">{team.attempts}</div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function formatDuration(seconds) {
  if (!seconds) return '0 сек';
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return minutes > 0 ? `${minutes} мин ${secs} с` : `${secs} с`;
}
