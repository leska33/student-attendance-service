# REST-сервис системы учёта студентов и их посещаемости (Student Attendance Service)

Данный проект представляет собой разработку базового REST-сервиса для учёта студентов и их посещаемости.
Сервис реализован с использованием фреймворка Spring Boot и демонстрирует принципы построения многослойной архитектуры, обработку HTTP-запросов с параметрами и использование DTO.

# Сожержание:
- Технологический стек
- Архитектура приложения
- Модель данных
- Реализованные REST-endpoint
- Запуск проекта
- Вывод

# Технологический стек:
- Java 25 — язык программирования
- Spring Boot 3.x — основной фреймворк
- Spring Web — для создания REST API
- Maven — система сборки
- Postman — тестирование API

# Архитектура приложения
Проект реализует классическую многослойную архитектуру, состоящую из трёх уровней:
1. Controller Layer (Уровень контроллеров):
- Принимает и обрабатывает входящие HTTP-запросы
- Использует @PathVariable для извлечения параметров из URL
- Использует @RequestParam для извлечения параметров запроса
- Возвращает ответы клиенту в формате JSON
- Возвращает корректные HTTP-статусы (200, 404)

2. Service Layer (Сервисный уровень):
- Содержит бизнес-логику приложения
- Выполняет фильтрацию студентов по группе
- Выполняет поиск студента по номеру студенческого билета
- Выполняет маппинг между сущностями и DTO
- Реализует правила обработки данных

3. Repository Layer (Репозиторий):
- Обеспечивает доступ к данным
- В данной реализации использует in-memory хранилище (List)
- Хранит список студентов внутри приложения
- Предоставляет методы получения данных

# Модель данных
Сущность Student (Студент):
- studentId — номер студенческого билета (7 цифр)
- fullName — ФИО студента
- groupNumber — номер группы (6 цифр)
- attendanceCount — количество посещённых занятий
- averageGrade — средний балл по 10-балльной шкале

# Data Transfer Object (DTO)
Для изоляции клиентской части от внутренней модели данных используется:
- StudentResponseDto - используется для ответа клиенту. Содержит данные студента, которые возвращаются через API.

DTO позволяет:
- изолировать внутреннюю модель
- контролировать формат ответа
- соблюдать принципы архитектуры


# Требования:
- Установленная Java 17 или выше
- IntelliJ IDEA (или другая IDE)
- Maven

# Запуск:
- Открыть проект в IntelliJ IDEA
- Выполнить mvn clean install
- Запустить класс StudentApplication
- Перейти по адресу: http://localhost:8080/students

# Запуск backend + frontend (React + Vite):
- Backend (Spring Boot):
  - из корня проекта выполнить `mvn spring-boot:run`
  - backend поднимется на `http://localhost:8080`
- Frontend в dev-режиме (Vite):
  - перейти в папку `frontend`
  - выполнить `npm install` (первый раз)
  - выполнить `npm run dev`
  - открыть адрес из консоли Vite (обычно `http://localhost:5173`)
- Production-сборка frontend в Spring static:
  - в папке `frontend` выполнить `npm run build`
  - Vite соберет файлы в `src/main/resources/static`
  - после этого открыть `http://localhost:8080`

# Как отправить ссылку другим людям
- Быстрый способ (публичная ссылка через туннель):
  - запустить backend: `mvn spring-boot:run`
  - в новом терминале выполнить: `npx localtunnel --port 8080`
  - появится публичный URL вида `https://xxxx.loca.lt`
  - отправьте этот URL другим, и они смогут открыть сайт **EduFlow**

# Docker и Docker Compose
- Переменные окружения: скопируйте `.env.example` в `.env` и при необходимости измените пароль и порты.
- Запуск приложения и PostgreSQL:
  - `docker compose up --build`
  - API и веб-интерфейс: `http://localhost:8080`
  - Проверка здоровья: `http://localhost:8080/actuator/health`
- Отдельная сборка образа: `docker build -t silverpear .`

# Публикация на PaaS (Render)
- В репозитории есть `render.yaml` (Blueprint): в [Render](https://render.com) создайте **Blueprint** и подключите GitHub-репозиторий — поднимутся **Web Service (Docker)** и **PostgreSQL**.
- Приложение читает БД так: `DB_HOST`, `DB_PORT`, `DB_NAME`, `SPRING_DATASOURCE_USERNAME`, `DB_PASSWORD` (как в blueprint), либо можно задать полный `SPRING_DATASOURCE_URL` вручную.
- Убедитесь, что тарифы **Free** на Render вам подходят (лимиты меняются — см. документацию Render).

# CI/CD (GitHub Actions)
- Файл `.github/workflows/ci-cd.yml`: при push и pull request в `main`/`master` выполняются сборка фронтенда, `mvn test` (профиль `ci`), сборка Docker-образа.
- При **push** в `main`/`master` дополнительно:
  - если задан секрет `RENDER_DEPLOY_HOOK_URL` — вызывается [Deploy Hook](https://render.com/docs/deploy-hooks) Render;
  - если задан секрет `DEPLOY_URL` (публичный URL сервиса **без** завершающего `/`) — выполняется опрос `/actuator/health` до статуса `UP`.
- Настройка секретов: репозиторий GitHub → **Settings → Secrets and variables → Actions**.