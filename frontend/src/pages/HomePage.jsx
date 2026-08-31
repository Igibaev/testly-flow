import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listCategories } from '../api/categories';
import { startAttempt } from '../api/attempts';

const SAMPLE_MIN = 10;
const SAMPLE_MAX = 15;

function plural(n, one, few, many) {
  const abs = Math.abs(n) % 100;
  const d = abs % 10;
  if (abs > 10 && abs < 20) return many;
  if (d === 1) return one;
  if (d >= 2 && d <= 4) return few;
  return many;
}

function sampledRange(categories) {
  return categories.reduce(
    (acc, c) => {
      if (c.questionCount <= 0) return acc;
      acc.min += Math.min(c.questionCount, SAMPLE_MIN);
      acc.max += Math.min(c.questionCount, SAMPLE_MAX);
      return acc;
    },
    { min: 0, max: 0 }
  );
}

export default function HomePage() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', team: '' });
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoadError(null);
    setCategories(null);
    listCategories()
      .then(setCategories)
      .catch((e) => setLoadError(e.message));
  }

  const filledCategories = categories?.filter((c) => c.questionCount > 0) ?? [];
  const { min: estimateMin, max: estimateMax } = sampledRange(filledCategories);
  const blockCount = filledCategories.length;
  const estimateText =
    estimateMin === estimateMax
      ? `Тест соберёт ${estimateMin} ${plural(estimateMin, 'вопрос', 'вопроса', 'вопросов')} из ${blockCount} ${plural(blockCount, 'блока', 'блоков', 'блоков')}.`
      : `Тест соберёт ${estimateMin}–${estimateMax} ${plural(estimateMax, 'вопрос', 'вопроса', 'вопросов')} из ${blockCount} ${plural(blockCount, 'блока', 'блоков', 'блоков')}.`;

  async function handleStart(e) {
    e.preventDefault();
    if (!form.firstName.trim() || !form.lastName.trim() || !form.team.trim()) {
      setStartError('Заполни имя, фамилию и команду');
      return;
    }
    setStarting(true);
    setStartError(null);
    try {
      const data = await startAttempt(form);
      navigate(`/attempt/${data.attemptId}`, { state: { startData: data } });
    } catch (e) {
      setStartError(e.message);
      setStarting(false);
    }
  }

  return (
    <div className="home-page">
      <section className="home-intro">
        <h1>Проверь себя</h1>
        <p className="home-lede">
          Тест собирается из нескольких блоков вопросов. Из каждого блока попадёт по 10–15
          случайных вопросов — состав каждый раз немного разный. Можно свободно
          возвращаться к вопросам и менять ответы, пока не нажмёшь «Завершить».
        </p>
      </section>

      <section className="home-categories" aria-live="polite">
        <header className="home-section-header">
          <h2>Категории вопросов</h2>
          <p className="home-section-sub">Добавленные блоки, из которых собирается тест</p>
        </header>
        {loadError && (
          <div className="state-error">
            <p>Не удалось загрузить блоки вопросов: {loadError}</p>
            <button type="button" className="btn btn-secondary" onClick={load}>
              Повторить
            </button>
          </div>
        )}
        {!loadError && categories === null && (
          <div className="category-grid">
            {[1, 2, 3].map((i) => (
              <div className="category-card skeleton" key={i} aria-hidden="true" />
            ))}
          </div>
        )}
        {!loadError && categories && categories.length === 0 && (
          <div className="state-empty">
            <p>Пока нет ни одного блока с вопросами. Загрузите вопросы в админ-панели.</p>
          </div>
        )}
        {!loadError && categories && categories.length > 0 && (
          <div className="category-grid">
            {categories.map((c) => (
              <article className="category-card" key={c.id} style={{ '--cat-accent': c.color || 'var(--color-accent)' }}>
                <h3>{c.name}</h3>
                {c.description && <p className="category-card-desc">{c.description}</p>}
                <p className="category-card-count">
                  {c.questionCount} {plural(c.questionCount, 'вопрос', 'вопроса', 'вопросов')} в пуле
                </p>
                {c.prepLinks?.length > 0 && (
                  <ul className="category-card-links">
                    {c.prepLinks.map((link) => (
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
        )}
      </section>

      {filledCategories.length > 0 && (
        <section className="home-start">
          <form className="start-form" onSubmit={handleStart}>
            <header className="home-section-header">
              <h2>Начать тест</h2>
              <p className="start-form-hint">{estimateText}</p>
            </header>
            <div className="field-row">
              <label className="field" htmlFor="firstName">
                <span>Имя</span>
                <input
                  id="firstName"
                  name="firstName"
                  required
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  autoComplete="given-name"
                />
              </label>
              <label className="field" htmlFor="lastName">
                <span>Фамилия</span>
                <input
                  id="lastName"
                  name="lastName"
                  required
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  autoComplete="family-name"
                />
              </label>
            </div>
            <label className="field" htmlFor="team">
              <span>Команда</span>
              <input
                id="team"
                name="team"
                required
                value={form.team}
                onChange={(e) => setForm({ ...form, team: e.target.value })}
                autoComplete="organization"
              />
            </label>
            {startError && <p className="field-error">{startError}</p>}
            <button type="submit" className="btn btn-primary btn-large start-form-submit" disabled={starting}>
              {starting ? 'Собираем тест…' : 'Пройти тест'}
            </button>
          </form>
        </section>
      )}
    </div>
  );
}
