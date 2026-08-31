import { useEffect, useState } from 'react';
import {
  createCategory,
  deleteCategory,
  listAdminCategories,
  updateCategory,
  updateCategoryPrepLinks,
} from '../../api/admin';

const emptyForm = () => ({ name: '', description: '', color: '#6d5dfc', questionsMin: '', questionsMax: '' });
const emptyLink = () => ({ title: '', url: '' });

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState(null);
  const [error, setError] = useState(null);
  const [form, setForm] = useState(emptyForm());
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [linkEditorId, setLinkEditorId] = useState(null);
  const [links, setLinks] = useState([emptyLink()]);

  const reload = () => {
    listAdminCategories()
      .then(setCategories)
      .catch((e) => setError(e.message));
  };

  useEffect(reload, []);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      color: form.color || null,
      questionsMin: form.questionsMin ? Number(form.questionsMin) : null,
      questionsMax: form.questionsMax ? Number(form.questionsMax) : null,
    };
    try {
      if (editingId) {
        await updateCategory(editingId, payload);
      } else {
        await createCategory(payload);
      }
      setForm(emptyForm());
      setEditingId(null);
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function startEdit(c) {
    setEditingId(c.id);
    setForm({
      name: c.name,
      description: c.description || '',
      color: c.color || '#6d5dfc',
      questionsMin: c.questionsMin ?? '',
      questionsMax: c.questionsMax ?? '',
    });
  }

  async function handleDelete(c) {
    if (!window.confirm(`Удалить категорию «${c.name}»?`)) return;
    try {
      await deleteCategory(c.id);
      reload();
    } catch (err) {
      setError(err.message);
    }
  }

  function openLinkEditor(c) {
    setLinkEditorId(c.id);
    setLinks(c.prepLinks?.length ? c.prepLinks.map((l) => ({ title: l.title, url: l.url })) : [emptyLink()]);
  }

  async function saveLinks(categoryId) {
    const filtered = links.filter((l) => l.title.trim() && l.url.trim());
    try {
      await updateCategoryPrepLinks(categoryId, filtered);
      setLinkEditorId(null);
      reload();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <h1>Категории вопросов</h1>

      <div className="card">
        <h2>{editingId ? 'Изменить категорию' : 'Новая категория'}</h2>
        {error && <div className="error-box">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label>Название</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div className="form-field">
            <label>Описание</label>
            <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="form-field">
            <label>Цвет-акцент</label>
            <input type="color" value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} />
          </div>
          <div className="form-field">
            <label>Вопросов в попытке: мин / макс (необязательно, иначе глобальный дефолт)</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="number"
                min="1"
                placeholder="мин"
                value={form.questionsMin}
                onChange={(e) => setForm({ ...form, questionsMin: e.target.value })}
              />
              <input
                type="number"
                min="1"
                placeholder="макс"
                value={form.questionsMax}
                onChange={(e) => setForm({ ...form, questionsMax: e.target.value })}
              />
            </div>
          </div>
          <button type="submit" className="btn" disabled={saving}>
            {saving ? 'Сохраняем…' : editingId ? 'Сохранить' : 'Создать'}
          </button>
          {editingId && (
            <button
              type="button"
              className="btn-secondary btn"
              onClick={() => {
                setEditingId(null);
                setForm(emptyForm());
              }}
            >
              Отмена
            </button>
          )}
        </form>
      </div>

      <div className="card">
        <h2>Все категории</h2>
        {!categories && <p className="muted">Загрузка…</p>}
        {categories && categories.length === 0 && <p className="muted">Категорий ещё нет.</p>}
        {categories && categories.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Название</th>
                <th>Вопросов</th>
                <th>Загрузок</th>
                <th>Диапазон выборки</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {categories.map((c) => (
                <tr key={c.id}>
                  <td>
                    <span className="color-dot" style={{ background: c.color || '#94a3b8' }} /> {c.name}
                  </td>
                  <td>{c.questionCount}</td>
                  <td>{c.testCount}</td>
                  <td>
                    {c.questionsMin ?? '—'}–{c.questionsMax ?? '—'}
                  </td>
                  <td style={{ display: 'flex', gap: 8 }}>
                    <button type="button" className="btn-secondary btn" onClick={() => startEdit(c)}>
                      Изменить
                    </button>
                    <button type="button" className="btn-secondary btn" onClick={() => openLinkEditor(c)}>
                      Ссылки ({c.prepLinks?.length ?? 0})
                    </button>
                    <button
                      type="button"
                      className="btn-secondary btn"
                      disabled={c.questionCount > 0}
                      title={c.questionCount > 0 ? 'Нельзя удалить: в категории есть вопросы' : undefined}
                      onClick={() => handleDelete(c)}
                    >
                      Удалить
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {linkEditorId && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal">
            <h3>Подготовительные ссылки</h3>
            {links.map((link, index) => (
              <div key={index} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
                <input
                  placeholder="Заголовок"
                  value={link.title}
                  onChange={(e) => {
                    const next = [...links];
                    next[index] = { ...next[index], title: e.target.value };
                    setLinks(next);
                  }}
                />
                <input
                  placeholder="URL"
                  value={link.url}
                  onChange={(e) => {
                    const next = [...links];
                    next[index] = { ...next[index], url: e.target.value };
                    setLinks(next);
                  }}
                />
                <button type="button" className="btn-secondary btn" onClick={() => setLinks(links.filter((_, i) => i !== index))}>
                  ✕
                </button>
              </div>
            ))}
            <button type="button" className="btn-secondary btn" onClick={() => setLinks([...links, emptyLink()])}>
              + Добавить ссылку
            </button>
            <div className="modal-actions">
              <button type="button" className="btn-secondary btn" onClick={() => setLinkEditorId(null)}>
                Отмена
              </button>
              <button type="button" className="btn" onClick={() => saveLinks(linkEditorId)}>
                Сохранить
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
