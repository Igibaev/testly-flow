import { useEffect, useState } from 'react';
import { listAdminCategories, listAdminTests, uploadTest } from '../../api/admin';

export default function AdminTestsPage() {
  const [tests, setTests] = useState(null);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState(null);
  const [file, setFile] = useState(null);
  const [title, setTitle] = useState('');
  const [mode, setMode] = useState('existing'); // 'existing' | 'new'
  const [categoryId, setCategoryId] = useState('');
  const [newCategoryName, setNewCategoryName] = useState('');
  const [newCategoryDescription, setNewCategoryDescription] = useState('');
  const [newCategoryColor, setNewCategoryColor] = useState('#6d5dfc');
  const [uploading, setUploading] = useState(false);
  const [warnings, setWarnings] = useState([]);
  const [lastUpload, setLastUpload] = useState(null);

  const reload = () => {
    listAdminTests()
      .then(setTests)
      .catch((e) => setError(e.message));
    listAdminCategories()
      .then(setCategories)
      .catch(() => {});
  };

  useEffect(reload, []);

  async function handleUpload(e) {
    e.preventDefault();
    if (!file) {
      setError('Выберите MD-файл с вопросами');
      return;
    }
    if (mode === 'existing' && !categoryId) {
      setError('Выберите категорию, либо переключитесь на «Создать новую»');
      return;
    }
    if (mode === 'new' && !newCategoryName.trim()) {
      setError('Укажите название новой категории');
      return;
    }
    setError(null);
    setWarnings([]);
    setLastUpload(null);
    setUploading(true);

    const formData = new FormData();
    formData.append('file', file);
    if (title.trim()) formData.append('title', title.trim());
    if (mode === 'existing') {
      formData.append('categoryId', categoryId);
    } else {
      formData.append('newCategoryName', newCategoryName.trim());
      if (newCategoryDescription.trim()) formData.append('newCategoryDescription', newCategoryDescription.trim());
      if (newCategoryColor) formData.append('newCategoryColor', newCategoryColor);
    }

    try {
      const response = await uploadTest(formData);
      setWarnings(response.warnings || []);
      setLastUpload(response);
      setFile(null);
      setTitle('');
      setNewCategoryName('');
      setNewCategoryDescription('');
      e.target.reset();
      reload();
    } catch (err) {
      setError(err.message + (err.details?.length ? ': ' + err.details.join('; ') : ''));
    } finally {
      setUploading(false);
    }
  }

  return (
    <div>
      <div className="card">
        <h2>Загрузить вопросы из MD-файла</h2>
        {error && <div className="error-box">{error}</div>}
        {lastUpload && (
          <div className="error-box" style={{ background: '#eefbf3', color: '#166534', borderColor: '#bbf0d0' }}>
            Добавлено {lastUpload.questionsAdded} вопрос(ов) в категорию «{lastUpload.categoryName}»
            {lastUpload.categoryCreated ? ' (создана новая категория)' : ''}.
          </div>
        )}
        {warnings.length > 0 && (
          <div className="error-box" style={{ background: '#fff8e1', color: '#8a6d00', borderColor: '#f0dca0' }}>
            Загружено с предупреждениями:
            <ul>
              {warnings.map((w, i) => (
                <li key={i}>{w}</li>
              ))}
            </ul>
          </div>
        )}
        <form onSubmit={handleUpload}>
          <div className="form-field">
            <label htmlFor="test-file">MD-файл (вопросы + ключ ответов)</label>
            <input id="test-file" type="file" accept=".md" onChange={(e) => setFile(e.target.files[0])} />
          </div>
          <div className="form-field">
            <label htmlFor="test-title">Название загрузки (необязательно, иначе берётся из файла)</label>
            <input id="test-title" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>

          <div className="form-field">
            <label>Категория</label>
            <div style={{ display: 'flex', gap: 16, marginBottom: 8 }}>
              <label style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                <input type="radio" checked={mode === 'existing'} onChange={() => setMode('existing')} />
                Выбрать существующую
              </label>
              <label style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                <input type="radio" checked={mode === 'new'} onChange={() => setMode('new')} />
                Создать новую
              </label>
            </div>

            {mode === 'existing' ? (
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                <option value="">— выберите —</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <input
                  placeholder="Название новой категории"
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                />
                <input
                  placeholder="Описание (необязательно)"
                  value={newCategoryDescription}
                  onChange={(e) => setNewCategoryDescription(e.target.value)}
                />
                <input type="color" value={newCategoryColor} onChange={(e) => setNewCategoryColor(e.target.value)} />
              </div>
            )}
          </div>

          <button type="submit" className="btn" disabled={uploading}>
            {uploading ? 'Загрузка…' : 'Загрузить'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2>Загруженные файлы</h2>
        {!tests && <p className="muted">Загрузка…</p>}
        {tests && tests.length === 0 && <p className="muted">Пока ничего не загружено.</p>}
        {tests && tests.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Название</th>
                <th>Категория</th>
                <th>Вопросов</th>
                <th>Загружен</th>
              </tr>
            </thead>
            <tbody>
              {tests.map((t) => (
                <tr key={t.id}>
                  <td>{t.title}</td>
                  <td>{t.categoryName}</td>
                  <td>{t.questionCount}</td>
                  <td>{new Date(t.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
