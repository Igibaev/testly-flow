import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listCategories } from '../api/categories';
import { startAttempt } from '../api/attempts';

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

  const totalQuestions = categories?.reduce((sum, c) => sum + c.questionCount, 0) ?? 0;

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
                <p className="category-card-count">{c.questionCount} вопросов в пуле</p>
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

      {categories && categories.length > 0 && (
        <section className="home-start">
          <form className="start-form" onSubmit={handleStart}>
            <h2>Начать</h2>
            <p className="start-form-hint">Тест соберёт примерно {totalQuestions} вопросов из {categories.length} блоков.</p>
            <div className="field-row">
              <label className="field">
                <span>Имя</span>
                <input
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  autoComplete="given-name"
                />
              </label>
              <label className="field">
                <span>Фамилия</span>
                <input
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  autoComplete="family-name"
                />
              </label>
            </div>
            <label className="field">
              <span>Команда</span>
              <input value={form.team} onChange={(e) => setForm({ ...form, team: e.target.value })} />
            </label>
            {startError && <p className="field-error">{startError}</p>}
            <button type="submit" className="btn btn-primary btn-large" disabled={starting}>
              {starting ? 'Собираем тест…' : 'Пройти тест'}
            </button>
          </form>
        </section>
      )}
    </div>
  );
}
