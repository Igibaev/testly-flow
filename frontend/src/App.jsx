import { Routes, Route, Link, useLocation } from 'react-router-dom';
import HomePage from './pages/HomePage.jsx';
import AttemptPage from './pages/AttemptPage.jsx';
import ResultPage from './pages/ResultPage.jsx';
import AdminApp from './pages/admin/AdminApp.jsx';

export default function App() {
  const location = useLocation();
  const isAttempt = /^\/attempt\/[^/]+$/.test(location.pathname);

  return (
    <div className="app-shell">
      <header className={`app-header${isAttempt ? ' app-header-compact' : ''}`}>
        <Link to="/" className="app-title">
          Платформа тестирования знаний
        </Link>
        {!isAttempt && (
          <Link to="/admin" className="app-admin-link">
            Админ-панель
          </Link>
        )}
      </header>
      <main className="app-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/attempt/:attemptId" element={<AttemptPage />} />
          <Route path="/attempt/:attemptId/result" element={<ResultPage />} />
          <Route path="/admin/*" element={<AdminApp />} />
        </Routes>
      </main>
    </div>
  );
}
