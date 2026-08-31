import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listTests } from '../api/tests';

export default function TestListPage() {
  const [tests, setTests] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    listTests()
      .then(setTests)
      .catch((e) => setError(e.message));
  }, []);

  if (error) {
    return <div className="error-box">{error}</div>;
  }

  if (!tests) {
    return <p className="muted">Загрузка тестов…</p>;
  }

  if (tests.length === 0) {
    return <p className="muted">Пока нет доступных тестов.</p>;
  }

  return (
    <div>
      <h1>Доступные тесты</h1>
      {tests.map((test) => (
        <Link to={`/test/${test.id}`} key={test.id} className="card-link">
          <div className="card">
            <h2>{test.title}</h2>
            {test.description && <p className="muted">{test.description}</p>}
            <p className="muted">Вопросов: {test.questionCount}</p>
          </div>
        </Link>
      ))}
    </div>
  );
}
