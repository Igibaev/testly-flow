import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { submitAttempt } from '../api/tests';

export default function TestRunPage() {
  const { testId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const { attemptId, test } = location.state || {};

  const [answers, setAnswers] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!attemptId || !test) {
      navigate(`/test/${testId}`, { replace: true });
    }
  }, [attemptId, test, testId, navigate]);

  if (!attemptId || !test) {
    return null;
  }

  const selectOption = (questionId, letter) => {
    setAnswers({ ...answers, [questionId]: letter });
  };

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const payload = {
        answers: test.questions.map((q) => ({
          questionId: q.id,
          selectedOption: answers[q.id] || null,
        })),
      };
      const result = await submitAttempt(attemptId, payload);
      navigate(`/test/${testId}/result`, { state: { result, test } });
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  const answeredCount = Object.keys(answers).length;

  return (
    <div>
      <h1>{test.title}</h1>
      <p className="muted">
        Отвечено {answeredCount} из {test.questions.length}
      </p>

      {error && <div className="error-box">{error}</div>}

      <div className="card">
        {test.questions.map((question) => (
          <div className="question-block" key={question.id}>
            <div className="question-number">Вопрос {question.number}</div>
            <strong>{question.text}</strong>
            <div style={{ marginTop: 10 }}>
              {question.options.map((option) => (
                <label className="option-row" key={option.letter}>
                  <input
                    type="radio"
                    name={`question-${question.id}`}
                    checked={answers[question.id] === option.letter}
                    onChange={() => selectOption(question.id, option.letter)}
                  />
                  <span>
                    {option.letter}) {option.text}
                  </span>
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>

      <button className="btn" onClick={handleSubmit} disabled={submitting}>
        {submitting ? 'Отправка…' : 'Завершить тест'}
      </button>
    </div>
  );
}
