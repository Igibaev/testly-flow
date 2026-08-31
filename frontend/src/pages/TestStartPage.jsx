import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTest, startAttempt } from '../api/tests';

export default function TestStartPage() {
  const { testId } = useParams();
  const navigate = useNavigate();

  const [test, setTest] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ firstName: '', lastName: '', team: '' });

  useEffect(() => {
    getTest(testId)
      .then(setTest)
      .catch((e) => setError(e.message));
  }, [testId]);

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const response = await startAttempt(testId, form);
      navigate(`/test/${testId}/run`, { state: { attemptId: response.attemptId, test } });
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  if (error && !test) {
    return <div className="error-box">{error}</div>;
  }

  if (!test) {
    return <p className="muted">Загрузка…</p>;
  }

  return (
    <div>
      <h1>{test.title}</h1>
      {test.description && <p className="muted">{test.description}</p>}

      {test.prepLinks.length > 0 && (
        <div className="card prep-links">
          <h2>Материалы для подготовки</h2>
          <ul>
            {test.prepLinks.map((link) => (
              <li key={link.id}>
                <a href={link.url} target="_blank" rel="noreferrer">
                  {link.title}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="card">
        <h2>Начать тест</h2>
        {error && <div className="error-box">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="firstName">Имя</label>
            <input id="firstName" required value={form.firstName} onChange={handleChange('firstName')} />
          </div>
          <div className="form-field">
            <label htmlFor="lastName">Фамилия</label>
            <input id="lastName" required value={form.lastName} onChange={handleChange('lastName')} />
          </div>
          <div className="form-field">
            <label htmlFor="team">Команда</label>
            <input id="team" required value={form.team} onChange={handleChange('team')} />
          </div>
          <button type="submit" className="btn" disabled={submitting}>
            {submitting ? 'Начинаем…' : 'Начать тест'}
          </button>
        </form>
      </div>
    </div>
  );
}
