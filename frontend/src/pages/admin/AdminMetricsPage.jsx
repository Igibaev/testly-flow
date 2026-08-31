import { useEffect, useState } from 'react';
import { listAdminCategories, getMetrics } from '../../api/admin';

export default function AdminMetricsPage() {
  const [categories, setCategories] = useState([]);
  const [categoryId, setCategoryId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    listAdminCategories().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    getMetrics({
      categoryId: categoryId || undefined,
      from: from ? new Date(from).toISOString() : undefined,
      to: to ? new Date(to).toISOString() : undefined,
    })
      .then(setMetrics)
      .catch((e) => setError(e.message));
  }, [categoryId, from, to]);

  const maxBucket = metrics ? Math.max(1, ...Object.values(metrics.scoreDistribution)) : 1;
  const maxTeam = metrics && metrics.teamActivity.length ? Math.max(1, ...metrics.teamActivity.map((t) => t.attempts)) : 1;

  return (
    <div>
      <h1>Метрики</h1>

      <div className="filters-row">
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">Все категории</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <label className="muted" style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
          с <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label className="muted" style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
          по <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
      </div>

      {error && <div className="error-box">{error}</div>}
      {!metrics && !error && <p className="muted">Загрузка…</p>}

      {metrics && (
        <>
          <div className="metrics-grid">
            <MetricTile value={metrics.startsCount} label="Стартов теста" />
            <MetricTile value={metrics.completedCount} label="Завершено" />
            <MetricTile value={metrics.abandonedCount} label="Не завершено" />
            <MetricTile value={formatDuration(metrics.averageDurationSeconds)} label="Среднее время прохождения" />
          </div>

          {metrics.excludedSuspiciousAttempts > 0 && (
            <p className="muted">
              {metrics.excludedSuspiciousAttempts} попытк(и/а) с подозрительными таймингами исключены из расчёта времени (баллы учтены).
            </p>
          )}

          <div className="card">
            <h2>Распределение баллов</h2>
            <p className="muted">Сколько завершённых попыток попало в каждый диапазон итогового балла.</p>
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
            <p className="muted">Число завершённых попыток на команду.</p>
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

          <div className="card">
            <h2>Время на вопрос</h2>
            <p className="muted">
              Считается только по завершённым попыткам без подозрительных таймингов; вопрос попадает в рейтинг только
              при {metrics.minSamplesForTiming}+ ответах.
            </p>
            {metrics.questionTiming.slowestQuestions.length === 0 ? (
              <p className="muted">Недостаточно данных: нужно минимум {metrics.minSamplesForTiming} ответов на вопрос.</p>
            ) : (
              <div className="timing-tables">
                <TimingTable title="Самые долгие" items={metrics.questionTiming.slowestQuestions} />
                <TimingTable title="Самые быстрые" items={metrics.questionTiming.fastestQuestions} />
              </div>
            )}
          </div>

          <div className="card">
            <h2>По блокам</h2>
            <p className="muted">Отсортировано по проблемности: дольше и с меньшей долей верных — выше.</p>
            {metrics.categoryMetrics.length === 0 && <p className="muted">Недостаточно данных.</p>}
            {[...metrics.categoryMetrics]
              .sort((a, b) => b.avgTimePerQuestionSeconds - a.avgTimePerQuestionSeconds || a.correctRate - b.correctRate)
              .map((c) => (
                <div className="category-metric-card" key={c.categoryId} style={{ '--cat-accent': c.color || '#6d5dfc' }}>
                  <h3>{c.categoryName}</h3>
                  <div className="bar-row">
                    <div className="bar-label">Время / вопрос</div>
                    <div className="bar-track">
                      <div
                        className="bar-fill"
                        style={{ width: `${Math.min(100, (c.avgTimePerQuestionSeconds / (metrics.questionTiming.averageTimePerQuestionSeconds * 2 || 1)) * 100)}%` }}
                      />
                    </div>
                    <div className="bar-value">{c.avgTimePerQuestionSeconds} с (медиана {c.medianTimePerQuestionSeconds} с)</div>
                  </div>
                  <div className="bar-row">
                    <div className="bar-label">% верных</div>
                    <div className="bar-track">
                      <div className="bar-fill bar-fill-accent" style={{ width: `${c.correctRate}%` }} />
                    </div>
                    <div className="bar-value">{c.correctRate}%</div>
                  </div>
                  <p className="muted">
                    Вопросов отвечено: {c.questionsServed} · Попыток охвачено: {c.attemptsCovered} · В среднем на блок:{' '}
                    {c.avgTimePerAttemptSeconds} с
                  </p>
                </div>
              ))}
          </div>
        </>
      )}
    </div>
  );
}

function MetricTile({ value, label }) {
  return (
    <div className="metric-tile">
      <div className="value">{value}</div>
      <div className="label">{label}</div>
    </div>
  );
}

function TimingTable({ title, items }) {
  return (
    <div>
      <h3>{title}</h3>
      <table>
        <thead>
          <tr>
            <th>Вопрос</th>
            <th>Категория</th>
            <th>Сред. / медиана</th>
            <th>% верных</th>
            <th>Наблюдений</th>
          </tr>
        </thead>
        <tbody>
          {items.map((q) => (
            <tr key={q.questionId}>
              <td title={q.text}>№{q.number}</td>
              <td>{q.categoryName}</td>
              <td>
                {q.avgTimeSeconds} с / {q.medianTimeSeconds} с
              </td>
              <td>{q.correctRate}%</td>
              <td>{q.samplesCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatDuration(seconds) {
  if (!seconds) return '0 сек';
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return minutes > 0 ? `${minutes} мин ${secs} с` : `${secs} с`;
}
