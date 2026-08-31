# Платформа для тестирования знаний

Веб-приложение для проведения тестов сотрудников по вопросам с выбором ответа. Прохождение — без авторизации (ФИО + команда), с автоматической фиксацией IP и User-Agent. Встроенные метрики без сторонних систем мониторинга.

## Стек

- **Backend**: Java 17, Spring Boot 3.2 (Web, Data JPA, Validation), Flyway, PostgreSQL driver.
- **Frontend**: React 18 + Vite + React Router, без UI-библиотек.
- **БД**: PostgreSQL 16.
- **Контейнеризация**: Docker + docker-compose.

## Быстрый старт

```bash
cp .env.example .env
docker-compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Админ-панель: http://localhost:3000/admin (пароль по умолчанию — `admin`, задаётся переменной `ADMIN_PASSWORD`)

Данные PostgreSQL сохраняются в именованном volume `pgdata` и переживают `docker-compose down` / `up`.

## Формат MD-файла теста

Один файл содержит и вопросы, и ключ ответов. Пример — [`sample-test.md`](./sample-test.md).

**Секция вопросов:**

```markdown
**1. Текст вопроса?**
- А) вариант 1
- Б) вариант 2
- В) вариант 3
- Г) вариант 4
```

**Секция ключа ответов** — после разделителя `---`, под заголовком `## Ключ ответов`, в виде markdown-таблицы. В одной строке может быть несколько пар колонок «№ | Ответ» — парсер обрабатывает их все:

```markdown
---

## Ключ ответов

| № | Ответ | № | Ответ |
|---|---|---|---|
| 1 | В | 3 | В |
| 2 | Б | 4 | А |
```

Название теста берётся из параметра `title` при загрузке либо из первого заголовка `# ...` в начале файла.

## REST API

Все ответы — JSON. Ошибки — `{ "error": "...", "details": [...] }`.

### Публичные эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/tests` | Список тестов |
| GET | `/api/tests/{id}` | Тест: описание, подготовительные ссылки, вопросы (без правильных ответов) |
| POST | `/api/tests/{id}/attempts/start` | Начать попытку: `{firstName, lastName, team}` |
| POST | `/api/attempts/{attemptId}/submit` | Завершить попытку: `{answers: [{questionId, selectedOption}]}` |

### Админские эндпоинты (заголовок `X-Admin-Password`)

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/admin/tests` | Список тестов со статистикой |
| POST | `/api/admin/tests` | Загрузка теста (multipart: `file`, `title`, `prepLinkTitles[]`, `prepLinkUrls[]`) |
| PUT | `/api/admin/tests/{id}/prep-links` | Обновить подготовительные ссылки: `{links: [{title, url}]}` |
| GET | `/api/admin/attempts?testId=&team=&page=&size=` | Список попыток с фильтрами |
| GET | `/api/admin/attempts/{id}` | Детализация попытки |
| GET | `/api/admin/metrics?testId=` | Метрики (по тесту или по всем) |

### Примеры curl

```bash
# Загрузить тест
curl -X POST http://localhost:8080/api/admin/tests \
  -H "X-Admin-Password: admin" \
  -F "file=@sample-test.md" \
  -F "title=Тест по продукту АРМК" \
  -F "prepLinkTitles=Документация" \
  -F "prepLinkUrls=https://example.com/docs"

# Список тестов
curl http://localhost:8080/api/tests

# Начать попытку
curl -X POST http://localhost:8080/api/tests/1/attempts/start \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Иван","lastName":"Иванов","team":"Alpha"}'

# Завершить попытку
curl -X POST http://localhost:8080/api/attempts/1/submit \
  -H "Content-Type: application/json" \
  -d '{"answers":[{"questionId":1,"selectedOption":"В"},{"questionId":2,"selectedOption":"Б"}]}'

# Метрики
curl http://localhost:8080/api/admin/metrics -H "X-Admin-Password: admin"
```

## Схема БД

`tests` → `questions` → `question_options`; `tests` → `prep_links`; `tests` → `attempts` → `attempt_answers`; `tests` → `metrics` (агрегаты по тесту). Правильный ответ хранится прямо в `questions.correct_option`.

## Локальная разработка без Docker

```bash
# Backend
cd backend
mvn spring-boot:run
# (нужен локальный PostgreSQL, см. переменные SPRING_DATASOURCE_* в application.yml)

# Frontend
cd frontend
npm install
npm run dev
# проксирует /api на http://localhost:8080
```

## Тесты

```bash
cd backend
mvn test
```

Юнит-тесты покрывают парсер MD-файла (несколько пар в строке ключа, неполная строка, отсутствующие/лишние ответы) и подсчёт баллов.
