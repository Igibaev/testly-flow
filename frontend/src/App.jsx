import { Routes, Route, Link } from 'react-router-dom';
import TestListPage from './pages/TestListPage.jsx';
import TestStartPage from './pages/TestStartPage.jsx';
import TestRunPage from './pages/TestRunPage.jsx';
import TestResultPage from './pages/TestResultPage.jsx';
import AdminApp from './pages/admin/AdminApp.jsx';

export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="app-title">
          Платформа тестирования знаний
        </Link>
        <Link to="/admin" className="app-admin-link">
          Админ-панель
        </Link>
      </header>
      <main className="app-content">
        <Routes>
          <Route path="/" element={<TestListPage />} />
          <Route path="/test/:testId" element={<TestStartPage />} />
          <Route path="/test/:testId/run" element={<TestRunPage />} />
          <Route path="/test/:testId/result" element={<TestResultPage />} />
          <Route path="/admin/*" element={<AdminApp />} />
        </Routes>
      </main>
    </div>
  );
}
