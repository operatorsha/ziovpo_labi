# ZIOVPO Labi Backend

Серверная часть для задания 1. Проект подготовлен как отдельный Git-репозиторий `operatorsha/ziovpo_labi` и локально расположен в папке `labi`.

## Что сделано

- Создан Spring Boot backend на Java 21.
- Добавлена аутентификация через JWT access/refresh tokens.
- Refresh-токены хранятся в БД только в виде SHA-256 hash.
- Добавлена авторизация по ролям `USER` и `ADMIN`.
- Настроены правила доступа:
  - `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh` доступны без токена;
  - `/api/profile` доступен ролям `USER` и `ADMIN`;
  - `/api/admin/**` доступен только роли `ADMIN`;
  - остальные endpoints требуют аутентификацию.
- Подключена PostgreSQL через Spring Data JPA.
- HTTPS вынесен в конфигурацию через переменные окружения.
- Добавлен пример переменных окружения в `.env.example`.
- Добавлен GitHub Actions pipeline с отдельными job `test` и `build`.
- Добавлены тесты базового auth flow и загрузки контекста.

## Переменные окружения

```env
SERVER_PORT=8443
DB_URL=jdbc:postgresql://localhost:5432/ziovpo_labi
DB_USERNAME=ziovpo
DB_PASSWORD=change-me
JWT_SECRET=change-me-to-a-random-64-character-secret-value-for-hs256
SSL_ENABLED=false
SSL_KEYSTORE_PATH=classpath:server.p12
SSL_KEYSTORE_PASSWORD=change-me
SSL_KEY_ALIAS=ziovpo
```

Для локального запуска без сертификата оставьте `SSL_ENABLED=false`. Для HTTPS нужно добавить keystore-файл `.p12` или `.jks`, прописать путь в `SSL_KEYSTORE_PATH` и пароль в `SSL_KEYSTORE_PASSWORD`.

## Запуск

PostgreSQL можно поднять локально:

```bash
docker compose up -d
```

```bash
./gradlew test
./gradlew build
./gradlew bootRun
```

На Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat bootRun
```

Тестовый администратор создается автоматически:

- email: `admin@example.com`
- password: `Admin123!`

## Основные endpoints

- `GET /api/health` - проверка сервера.
- `POST /api/auth/register` - регистрация пользователя.
- `POST /api/auth/login` - получение access/refresh токенов.
- `POST /api/auth/refresh` - обновление пары токенов.
- `POST /api/auth/logout` - отзыв одной refresh-сессии.
- `POST /api/auth/logout-all` - отзыв всех активных refresh-сессий пользователя.
- `GET /api/profile` - профиль текущего пользователя.
- `GET /api/admin/panel` - пример endpoint только для `ADMIN`.

## UML

UML, Unified Modeling Language, используется для визуального описания структуры и поведения системы.

Основные типы UML-диаграмм:

- Диаграмма классов показывает классы, поля, методы и связи между сущностями.
- Диаграмма вариантов использования показывает роли пользователей и доступные им сценарии.
- Диаграмма последовательности показывает порядок обмена сообщениями между объектами во времени.
- Диаграмма активности описывает бизнес-процесс или алгоритм.
- Диаграмма состояний показывает состояния объекта и переходы между ними.
- Диаграмма компонентов показывает крупные модули системы и зависимости между ними.
- Диаграмма развертывания описывает размещение приложения, БД и инфраструктуры.

Для этого backend полезны диаграммы классов для `UserAccount`, `UserSession`, `AuthService`, диаграмма последовательности для login/refresh flow и диаграмма компонентов для API, PostgreSQL и CI.

## ER-диаграммы

ER-диаграмма описывает структуру реляционной базы данных: сущности, атрибуты и связи между таблицами.

В текущем проекте минимальная ER-модель:

- `users` - учетные записи пользователей.
- `user_sessions` - refresh-сессии пользователей.
- Связь `users 1:N user_sessions`: один пользователь может иметь много активных или отозванных refresh-сессий.

Ключевые поля:

- `users.id` - primary key.
- `users.email` - unique.
- `user_sessions.id` - primary key.
- `user_sessions.user_id` - foreign key на `users.id`.
- `user_sessions.refresh_token_hash` - unique hash refresh-токена.
