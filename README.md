# service-desk

Сервис-деск и каталог услуг для преподавателей. Многомодульный монолит на Spring Boot 4 (Java 21, Maven).

## Модули и зависимости

| Модуль | Назначение | Зависит от |
|---|---|---|
| `common` | общие утилиты, без бизнес-логики | — |
| `auth` | пользователи, подразделения, роли, вход | `common` |
| `catalog` | каталог услуг, шаблоны чек-листов | `common`, `auth` |
| `requests` | заявки, согласование, статусы, SLA | `common`, `auth`, `catalog` |
| `notifications` | уведомления, SSE | `common`, `auth`, `requests` (только события) |
| `app` | точка входа, сборка jar | все модули |

Зависимости между модулями задаются в `pom.xml` каждого модуля. Циклы Maven не пропустит.

## Раскладка внутри модуля

```
<module>/src/main/java/rut/miit/servicedesk/<module>/
├── (публичное: интерфейсы, DTO, события)
└── internal/   (entity, repository, service, controller: package-private)
```

## Сборка и запуск

```bash
mvn clean package
DB_URL=jdbc:postgresql://localhost:5432/servicedesk DB_USER=... DB_PASSWORD=... \
  java -jar app/target/service-desk.jar
```

Для запуска нужна доступная PostgreSQL. Пока не настроен `SecurityFilterChain`, Spring Security закрывает все эндпоинты.
