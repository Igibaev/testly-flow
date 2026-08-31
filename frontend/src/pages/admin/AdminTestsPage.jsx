import { useEffect, useState } from 'react';
import { listAdminTests, uploadTest } from '../../api/admin';

const emptyLink = () => ({ title: '', url: '' });

export default function AdminTestsPage() {
  const [tests, setTests] = useState(null);
  const [error, setError] = useState(null);
  const [file, setFile] = useState(null);
  const [title, setTitle] = useState('');
  const [links, setLinks] = useState([emptyLink()]);
  const [uploading, setUploading] = useState(false);
  const [warnings, setWarnings] = useState([]);

  const reload = () => {
    listAdminTests()
      .then(setTests)
      .catch((e) => setError(e.message));
  };

  useEffect(reload, []);

  const updateLink = (index, field, value) => {
    const next = [...links];
    next[index] = { ...next[index], [field]: value };
    setLinks(next);
  };

  const addLink = () => setLinks([...links, emptyLink()]);
  const removeLink = (index) => setLinks(links.filter((_, i) => i !== index));

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Выберите MD-файл теста');
      return;
    }
    setError(null);
    setWarnings([]);
    setUploading(true);

    const formData = new FormData();
    formData.append('file', file);
    if (title.trim()) {
      formData.append('title', title.trim());
    }
    links
      .filter((l) => l.title.trim() && l.url.trim())
      .forEach((l) => {
        formData.append('prepLinkTitles', l.title.trim());
        formData.append('prepLinkUrls', l.url.trim());
      });

    try {
      const response = await uploadTest(formData);
      setWarnings(response.warnings || []);
      setFile(null);
      setTitle('');
      setLinks([emptyLink()]);
      e.target.reset();
      reload();
    } catch (err) {
      setError(err.message + (err.details?.length ? ': ' + err.details.join('; ') : ''));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <div className="card">
        <h2>Загрузить новый тест</h2>
        {error && <div className="error-box">{error}</div>}
        {warnings.length > 0 && (
          <div className="error-box" style={{ background: '#fff8e1', color: '#8a6d00', borderColor: '#f0dca0' }}>
            Тест загружен с предупреждениями:
            <ul>
              {warnings.map((w, i) => (
                <li key={i}>{w}</li>
              ))}
            </ul>
          </div>
        )}
        <form onSubmit={handleUpload}>
          <div className="form-field">
            <label htmlFor="test-file">MD-файл теста (вопросы + ключ ответов)</label>
            <input
              id="test-file"
              type="file"
              accept=".md"
              onChange={(e) => setFile(e.target.files[0])}
            />
          </div>
          <div className="form-field">
            <label htmlFor="test-title">Название теста (необязательно, иначе берётся из файла)</label>
            <input id="test-title" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>

          <div className="form-field">
            <label>Подготовительные ссылки</label>
            {links.map((link, index) => (
              <div key={index} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
                <input
                  placeholder="Заголовок"
                  value={link.title}
                  onChange={(e) => updateLink(index, 'title', e.target.value)}
                />
                <input
                  placeholder="URL"
                  value={link.url}
                  onChange={(e) => updateLink(index, 'url', e.target.value)}
                />
                <button type="button" className="btn-secondary btn" onClick={() => removeLink(index)}>
                  ✕
                </button>
              </div>
            ))}
            <button type="button" className="btn-secondary btn" onClick={addLink}>
              + Добавить ссылку
            </button>
          </div>

          <button type="submit" className="btn" disabled={uploading}>
            {uploading ? 'Загрузка…' : 'Загрузить тест'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2>Загруженные тесты</h2>
        {!tests && <p className="muted">Загрузка…</p>}
        {tests && tests.length === 0 && <p className="muted">Тесты ещё не загружены.</p>}
        {tests && tests.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Название</th>
                <th>Вопросов</th>
                <th>Попыток</th>
                <th>Создан</th>
              </tr>
            </thead>
            <tbody>
              {tests.map((t) => (
                <tr key={t.id}>
                  <td>{t.title}</td>
                  <td>{t.questionCount}</td>
                  <td>{t.attemptsCount}</td>
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
