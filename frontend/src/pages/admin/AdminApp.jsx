import { useState } from 'react';
import { NavLink, Route, Routes, Navigate } from 'react-router-dom';
import { checkAdminPassword } from '../../api/admin';
import AdminCategoriesPage from './AdminCategoriesPage.jsx';
import AdminTestsPage from './AdminTestsPage.jsx';
import AdminAttemptsPage from './AdminAttemptsPage.jsx';
import AdminAttemptDetailPage from './AdminAttemptDetailPage.jsx';
import AdminMetricsPage from './AdminMetricsPage.jsx';
import AdminEmployeesPage from './AdminEmployeesPage.jsx';
import AdminEmployeeCardPage from './AdminEmployeeCardPage.jsx';

export default function AdminApp() {
  const [authorized, setAuthorized] = useState(!!sessionStorage.getItem('adminPassword'));
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [checking, setChecking] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError(null);
    setChecking(true);
    sessionStorage.setItem('adminPassword', password);
    try {
      await checkAdminPassword();
      setAuthorized(true);
    } catch (err) {
      sessionStorage.removeItem('adminPassword');
      setError('Неверный пароль администратора');
    } finally {
      setChecking(false);
    }
  };

  if (!authorized) {
    return (
      <div className="card password-gate">
        <h2>Вход в админ-панель</h2>
        {error && <div className="error-box">{error}</div>}
        <form onSubmit={handleLogin}>
          <div className="form-field">
            <label htmlFor="admin-password">Пароль</label>
            <input
              id="admin-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn" disabled={checking}>
            {checking ? 'Проверка…' : 'Войти'}
          </button>
        </form>
      </div>
    );
  }

  return (
    <div>
      <nav className="admin-nav">
        <NavLink to="/admin/categories" className={({ isActive }) => (isActive ? 'active' : '')}>
          Категории
        </NavLink>
        <NavLink to="/admin/tests" className={({ isActive }) => (isActive ? 'active' : '')}>
          Загрузка вопросов
        </NavLink>
        <NavLink to="/admin/attempts" className={({ isActive }) => (isActive ? 'active' : '')}>
          Попытки
        </NavLink>
        <NavLink to="/admin/employees" className={({ isActive }) => (isActive ? 'active' : '')}>
          Сотрудники
        </NavLink>
        <NavLink to="/admin/metrics" className={({ isActive }) => (isActive ? 'active' : '')}>
          Метрики
        </NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<Navigate to="categories" replace />} />
        <Route path="categories" element={<AdminCategoriesPage />} />
        <Route path="tests" element={<AdminTestsPage />} />
        <Route path="attempts" element={<AdminAttemptsPage />} />
        <Route path="attempts/:attemptId" element={<AdminAttemptDetailPage />} />
        <Route path="employees" element={<AdminEmployeesPage />} />
        <Route path="employees/:firstName/:lastName/:team" element={<AdminEmployeeCardPage />} />
        <Route path="metrics" element={<AdminMetricsPage />} />
      </Routes>
    </div>
  );
}
