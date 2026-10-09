# План для Claude Code: первоначальная настройка проекта service-desk и проверка, что минимальная версия запускается

Этот документ — инструкция для Claude Code. Выполняй этапы по порядку, после каждого выполняй проверки из раздела «Критерии готовности» и только потом переходи дальше.

## 0. Контекст и правила работы

**Проект:** сервис-деск и каталог услуг для преподавателей. Многомодульный монолит (Maven), Java 21, Spring Boot 4.0.x, PostgreSQL, Keycloak. Base package: `rut.miit.servicedesk`.

**Что уже есть в корне репозитория:**
- `service-desk-guide.md` — гайд по архитектуре проекта. Это источник истины по структуре модулей, правилам зависимостей, размещению сущностей, взаимодействию модулей, Docker и тестам. **Прочитай его целиком до начала работы.**
- `.env` — файл с настройками окружения (часть значений может быть секретной).
- Возможно, заготовка проекта (`pom.xml`, модули). Проверь в этапе 1, что именно есть.

**Цель этого плана:** получить проект, который собирается, поднимается вместе с PostgreSQL и Keycloak через `docker-compose`, проходит минимальные тесты и содержит заготовки классов с подробными комментариями, что в них нужно реализовать. Бизнес-логику не реализуем.

### Жёсткие правила

1. **Секреты.** Не выводи значения из `.env` в логи, ответы и коммиты. Для анализа читай только имена переменных (например, `sed 's/=.*/=<скрыто>/' .env`). `.env` не коммитить, проверить, что он есть в `.gitignore`.
2. **`.env` можно менять.** Если нужной переменной нет, добавь её имя с безопасным значением-заглушкой в файл `.env` и сообщи об этом в отчёте. Не придумывай реальные секреты, это просто демонстрационное приложение.
3. **Гайд не менять.** Если реальность противоречит гайду (например, артефакт с таким названием не существует), внеси минимальное исправление в код и запиши его в раздел «Отклонения от гайда» итогового отчёта.
4. **Никакой бизнес-логики.** Только каркас, конфигурация, тесты запуска и заготовки с комментариями.
5. **Никаких лишних зависимостей** сверх таблицы из этапа 2. Нельзя использовать H2 вместо PostgreSQL.
6. **Версии не выдумывай.** Если не уверен в версии или названии артефакта, проверь, что Maven его резолвит (`mvn dependency:resolve`), а не угадывай.
7. **Работай маленькими коммитами**, по одному на этап (сообщения в разделе каждого этапа). Не делай `git push`, `git push --force` и не переписывай историю.
8. **Docker.** Если Docker в окружении недоступен, выполни всё, что возможно без него, и явно перечисли в отчёте, какие проверки не выполнены. Не помечай такие проверки как пройденные.
9. **Auth module** В некоторых модулях auth зависимость закомментирована, если требуется, то раскомментируй для работоспособности проекта. В конце комментарий обратно не ставь


### Когда остановиться и спросить пользователя

- В `.env` нет данных, без которых нельзя запустить PostgreSQL или Keycloak, и нет разумного значения по умолчанию.
- В репозитории есть SQL-схема БД, которая противоречит гайду (например, другие названия таблиц), и непонятно, чему следовать.
- Для сборки нужно поменять версию Java, Spring Boot или добавить зависимость вне таблицы.
- Любое действие, которое удаляет существующие файлы пользователя.

---

## 1. Этап 1. Разведка

**Цель:** понять, что есть, до внесения изменений.

1. Прочитай `service-desk-guide.md`.
2. Покажи структуру репозитория (`git status`, `git log --oneline -n 5`, дерево файлов без `target/` и `.git/`).
3. Прочитай `.env` **только по именам переменных** (значения скрыты) и составь соответствие «смысл → имя переменной» для:
   - PostgreSQL: имя БД, пользователь, пароль, порт;
   - Keycloak: администратор (логин и пароль), порт, имя realm, адрес;
   - приложение: порт, прочее.
   Если имя переменной нужного смысла уже есть, **используй его** в `docker-compose.yml`, а не заводи новую.
4. Проверь окружение и запиши версии в заметки для отчёта:
   ```bash
   java -version
   mvn -v
   docker --version
   docker compose version
   ```
   Требования: Java 21+, Maven 3.9+, для Docker-этапов нужен доступный Docker daemon (`docker info`).
5. Найди в репозитории SQL-файлы со схемой БД (`find . -name "*.sql" -not -path "*/target/*"`). Если схема есть, она будет использоваться в этапе 5.
6. Проверь, есть ли уже `pom.xml`, модули, `Dockerfile`, `docker-compose.yml`. Существующее не перезаписывай: сначала сравни с гайдом и выбери минимальные правки.

**Критерии готовности:**
- Составлено соответствие переменных `.env` и список недостающих.
- Известно, какие инструменты доступны.
- Известно, нужно ли создавать проект с нуля или дорабатывать существующий.

**Коммит:** не нужен (изменений нет).

---

## 2. Этап 2. Maven-каркас и проверка зависимостей

**Цель:** многомодульный проект по гайду, который собирается, с проверенными зависимостями.

### 2.1. Структура

```
service-desk/
├── pom.xml            # родительский (spring-boot-starter-parent 4.0.x, Java 21)
├── common/
├── auth/
├── catalog/
├── requests/
├── notifications/
└── app/
```

- `groupId` — `rut.miit.servicedesk`, `artifactId` родителя — `service-desk`.
- Версию Spring Boot возьми **последнюю патч-версию ветки 4.0.x**, доступную в Maven Central (не берёт «4.1» и выше без согласования). Версию springdoc выбери совместимую с этой версией Spring Boot (ветка 3.x), версию ArchUnit — актуальную стабильную 1.4.x.
- Версии всех сторонних зависимостей задаются один раз в родительском `pom.xml` (`dependencyManagement` и `properties`). В модулях версии не указываются.
- `spring-boot-maven-plugin` подключается **только в `app`**, `finalName` — `service-desk`. Остальные модули — обычные jar-библиотеки.

### 2.2. Зависимости модулей

| Модуль | Внутренние | Spring Boot starters (compile) | Тестовые |
|---|---|---|---|
| `common` | — | `webmvc`, `validation` | — |
| `auth` | `common` | `webmvc`, `security`, `data-jpa`, `validation`, `security-oauth2-resource-server` | `webmvc-test`, `data-jpa-test`, `security-test`, `spring-boot-testcontainers`, Testcontainers (`junit-jupiter`, `postgresql`), драйвер `postgresql` (test) |
| `catalog` | `common` (и `auth`, только если понадобится «автор изменения») | `webmvc`, `security`, `data-jpa`, `validation` | как у `auth` |
| `requests` | `common`, `auth`, `catalog` | `webmvc`, `security`, `data-jpa`, `validation` | как у `auth` |
| `notifications` | `common`, `auth`, `requests` | `webmvc`, `security`, `data-jpa`, `validation` | как у `auth` |
| `app` | все модули | `actuator`, springdoc `webmvc-ui`, драйвер `postgresql` (runtime) | `webmvc-test`, `security-test`, `spring-boot-testcontainers`, Testcontainers (`junit-jupiter`, `postgresql`), `archunit` |

Для всех модулей в родительском `pom.xml` общая тестовая зависимость `spring-boot-starter-test`.

**Важно про названия.** В Spring Boot 4 стартеры модульные, и названия отличаются от Spring Boot 3 (например, `spring-boot-starter-webmvc` вместо `spring-boot-starter-web`, у каждой технологии есть `...-test`-стартер). Названия Testcontainers 2.x тоже отличаются (`testcontainers-junit-jupiter`, `testcontainers-postgresql`). Если какой-то артефакт не находится, найди правильное название (документация Spring Boot 4 / Maven Central), исправь и запиши в «Отклонения».

В `app/pom.xml` оставь закомментированный блок Flyway (миграции не включаем, схема БД готовая).

### 2.3. Проверка зависимостей

Выполни и проанализируй результат:

```bash
mvn -B -ntp -q validate
mvn -B -ntp dependency:resolve
mvn -B -ntp dependency:tree
mvn -B -ntp -DskipTests package
```

Проверь вручную или скриптом:

- [ ] Порядок сборки (`Reactor Build Order`): `common → auth → catalog → requests → notifications → app`.
- [ ] Все артефакты резолвятся, нет `Could not find artifact`.
- [ ] Нет двух разных версий одной библиотеки в `dependency:tree` `app` (особенно Jackson, Hibernate, Spring Security).
- [ ] Плагин `spring-boot-maven-plugin` присутствует **только** в `app/pom.xml`.
- [ ] Нет циклических и «обратных» зависимостей: `catalog` не зависит от `requests`/`notifications`; `auth` ни от кого, кроме `common`; `notifications` использует `requests` только ради событий.
- [ ] В `app/target/` появился `service-desk.jar`, а `unzip -l` показывает внутри `BOOT-INF/lib` jar-ы всех пяти внутренних модулей.
- [ ] В `common`, `auth`, `catalog`, `requests`, `notifications` в `BOOT-INF/lib` нет дубликатов самого Spring Boot.

`mvn dependency:analyze` можно запустить для информации, но предупреждения «unused declared» на этом этапе **ожидаемы** (классов пока нет), зависимости из таблицы по этим предупреждениям не удаляй.

**Критерии готовности:** `mvn -B -ntp -DskipTests package` проходит, все пункты выше отмечены.

**Коммит:** `build: multi-module maven skeleton with verified dependencies`

---

## 3. Этап 3. Пакеты и заготовки классов с комментариями

**Цель:** создать структуру пакетов и заготовки, по которым подкоманды поймут, что и где реализовывать.

### 3.1. Правила раскладки (из гайда)

- Публичный контракт модуля лежит в корне пакета модуля: `rut.miit.servicedesk.<module>`.
- Всё остальное лежит в **плоском** пакете `rut.miit.servicedesk.<module>.internal`, классы **package-private**. Подпакетов внутри `internal` не создавать.
- В каждом пакете есть `package-info.java` с описанием.
- Главный класс `ServiceDeskApplication` лежит в корневом пакете `rut.miit.servicedesk` модуля `app`.

### 3.2. Что создаём

**Реализовать полностью (небольшие, нужны для запуска):**
- `app`: `ServiceDeskApplication` (`@SpringBootApplication`, `@EnableAsync`).
- `auth/internal`: `SecurityConfig` — по примеру из гайда (раздел 4): stateless, CSRF выключен, `permitAll` для `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, всё остальное требует аутентификации, JWT-конвертер ролей из `realm_access.roles` в `ROLE_*`, `@EnableMethodSecurity`.
- `common`: `NotFoundException`, `ConflictException` (наследуются от `RuntimeException`), `ErrorResponse` (`record`: `code`, `message`, `details`, `timestamp`).

**Публичные контракты (интерфейсы, `record`, `enum`): создать с сигнатурами, но без реализации.** Сигнатуры ниже — отправная точка, пометь их комментарием `TODO: согласовать с командой`.

| Модуль | Публичные типы |
|---|---|
| `auth` | `Role` (enum: `TEACHER`, `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF`, `DUTY_OFFICER`, `ADMIN`); `UserDto` (`id`, `fullName`, `departmentId`); `CurrentUser` (`long id()`, `String fullName()`, `Long departmentId()`, `boolean hasRole(Role role)`); `UserDirectory` (`Optional<UserDto> findById(long id)`, `Optional<UserDto> headOfDepartment(long departmentId)`, `List<UserDto> findByRoleAndDepartment(Role role, Long departmentId)`) |
| `catalog` | `CatalogService` (`ServiceItemDto getActive(long id)`, `List<ServiceItemDto> listActive()`); `ServiceItemDto` (`id`, `title`, `description`, `slaHours`, `List<ChecklistItemDto> checklist`); `ChecklistItemDto` (`position`, `text`, `required`) |
| `requests` | `RequestService` (`create`, `approve`, `reject`, `confirm`, `start`, `complete`, `cancel`, `get`; сигнатуры по процессу из гайда); `RequestDto`; `CreateRequestCommand` (`serviceId`, `audienceId`, `description`); `RequestStatus` (enum: `PENDING_HEAD`, `PENDING_DEPARTMENT`, `ASSIGNED`, `IN_PROGRESS`, `DONE`, `REJECTED_BY_HEAD`, `REJECTED_BY_DEPARTMENT`, `CANCELLED`); события-`record` (ниже) |
| `notifications` | публичных типов нет (модуль только слушает события) |

**События** (`record` в публичном пакете `requests`). У каждого есть общие поля `requestId`, `requesterId`, `departmentId`, `serviceTitle`, `occurredAt`, чтобы слушателю не приходилось идти за данными обратно:

| Событие | Дополнительные поля |
|---|---|
| `RequestCreated` | — |
| `RequestApproved` | `approvedBy` |
| `RequestRejected` | `rejectedBy`, `stage`, `reason` |
| `RequestAssigned` | `assigneeId`, `confirmedBy` |
| `RequestStarted` | `assigneeId` |
| `RequestCompleted` | `assigneeId` |
| `RequestCancelled` | — |

**Заготовки классов (в `internal`): пустые, package-private, с подробным Javadoc.**

⚠️ **Не навешивай на заготовки `@Entity`, `@Service`, `@Component`, `@RestController`, `@Repository` и не создавай репозитории Spring Data.** Иначе при `ddl-auto: validate` приложение упадёт на несуществующих таблицах, а Spring попытается создать бины из пустых классов. Эти аннотации добавят подкоманды, когда реализуют класс. Об этом должен сообщать Javadoc каждой заготовки.

| Модуль | Заготовки |
|---|---|
| `auth` | `CurrentUserImpl`, `UserDirectoryImpl`, `UserSyncService` (создаёт или обновляет запись в `users` при первом входе по токену), `UserEntity`, `DepartmentEntity` |
| `catalog` | `CatalogServiceImpl`, `CatalogController`, `ServiceItemMapper`, `ServiceItemEntity`, `ChecklistTemplateItemEntity` |
| `requests` | `RequestServiceImpl`, `RequestController`, `RequestMapper`, `RequestStatusMachine` (допустимые переходы статусов), `RequestStatsService` (статистика и SLA), `RequestEntity`, `AudienceEntity`, `RequestChecklistItemEntity`, `RequestStatusHistoryEntity` |
| `notifications` | `RequestEventsListener`, `NotificationService`, `NotificationController`, `SseEmitterRegistry`, `NotificationEntity` |
| `common` | `GlobalExceptionHandler` (будет `@RestControllerAdvice`, формат `ErrorResponse`) |

### 3.3. Шаблон комментария для каждой заготовки

Каждый класс-заготовка должен содержать Javadoc на русском по шаблону:

```java
/**
 * <Одна строка: что это за класс.>
 *
 * <p><b>Что нужно реализовать:</b>
 * <ul>
 *   <li>...конкретные шаги, ссылки на методы интерфейса, правила...</li>
 * </ul>
 *
 * <p><b>Зависимости:</b> какие публичные интерфейсы других модулей использовать
 * (и только их, к чужому internal не обращаться).
 *
 * <p><b>Аннотации, которые нужно добавить при реализации:</b> @Service / @Entity(@Table(name=...)) / ...
 *
 * <p><b>Ограничения:</b> границы модулей, транзакции, права доступа, события (что публиковать/слушать).
 *
 * <p>Подробности: service-desk-guide.md, раздел N.
 */
class ИмяКласса {
    // TODO: реализовать
}
```

Содержание «Что нужно реализовать» бери из гайда и из процесса заявки. Примеры того, что должно быть в комментариях:

- **`RequestServiceImpl`:** получить услугу через `CatalogService.getActive`, создать заявку со статусом `PENDING_HEAD`, скопировать шаблон чек-листа в `RequestChecklistItemEntity`, записать переход в `RequestStatusHistoryEntity`, опубликовать `RequestCreated` через `ApplicationEventPublisher` внутри одной транзакции; для каждого действия проверять роль и принадлежность заявки (кафедра, автор, исполнитель).
- **`RequestStatusMachine`:** перечислить допустимые переходы (`PENDING_HEAD → PENDING_DEPARTMENT | REJECTED_BY_HEAD`, `PENDING_DEPARTMENT → ASSIGNED | REJECTED_BY_DEPARTMENT`, `ASSIGNED → IN_PROGRESS`, `IN_PROGRESS → DONE`, отмена до `IN_PROGRESS`); недопустимый переход — `ConflictException` (409).
- **`RequestEntity`:** таблица `request`; поля `userId`, `serviceId`, `departmentId` как простые `Long` (без `@ManyToOne` в другие модули), связь с `AudienceEntity` и пунктами чек-листа обычная; `status` как `@Enumerated(EnumType.STRING)`; перечислить поля, которые нужно добавить в готовую схему (см. «Доработки схемы» в гайде).
- **`RequestEventsListener`:** `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)`, определить получателей через `UserDirectory`, сохранить уведомление и отправить в SSE; ошибка слушателя не должна ломать основную операцию.
- **`SseEmitterRegistry`:** хранить `SseEmitter` по `userId`, удалять при `onCompletion`/`onTimeout`/`onError`, слать keep-alive; помнить, что `EventSource` не передаёт `Authorization` (см. гайд, раздел 4).
- **`UserSyncService`:** по `sub`, имени и `department` из токена создать или обновить запись в `users` (`keycloak_id`), вернуть локальный `id`.
- **`UserEntity`:** таблица `users`; поля `keycloak_id`, ФИО, `department_id`; `login` и `password_hash` не использовать (пароли в Keycloak).

**Критерии готовности:**
- [ ] Структура пакетов совпадает с гайдом, в каждом пакете есть `package-info.java`.
- [ ] Каждая заготовка содержит Javadoc по шаблону и пустое тело с `TODO`.
- [ ] На заготовках нет Spring- и JPA-аннотаций.
- [ ] `mvn -B -ntp -DskipTests package` проходит.
- [ ] `grep -rn "@Entity\|@Service\|@Component\|@RestController\|@Repository" --include=*.java` находит такие аннотации только в классах, реализованных полностью (`SecurityConfig`).

**Коммит:** `feat: module skeletons with contracts and documented stubs`

---

## 4. Этап 4. Конфигурация приложения

**Цель:** `application.yml`, который работает и с `.env`, и в Docker.

В `app/src/main/resources/application.yml`:

- Подключение к БД через переменные окружения (`DB_URL`, `DB_USER`, `DB_PASSWORD`) с локальными значениями по умолчанию для разработки.
- `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, `hibernate.jdbc.time_zone: UTC`.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` (адрес, который видит браузер), а в Docker дополнительно `jwk-set-uri` (внутренний адрес Keycloak); значения задаются переменными окружения (см. гайд, раздел 6).
- `management.endpoints.web.exposure.include: health,info`.
- springdoc: `swagger-ui.path: /swagger-ui.html`.
- `application-dev.yml`: уровень логирования `rut.miit.servicedesk: DEBUG`.

Имена переменных окружения в `application.yml` согласуй с тем, что уже есть в `.env` (этап 1). Если в `.env` используются другие имена, подстрой compose-файл (сопоставление через `environment:` в `docker-compose.yml`), а не `.env`.

**Критерии готовности:** `application.yml` не содержит захардкоженных секретов, только `${VAR:значение-по-умолчанию}` для разработки.

**Коммит:** `config: application settings via environment`

---

## 5. Этап 5. Docker: Dockerfile, PostgreSQL и Keycloak в docker-compose

**Цель:** `docker compose up` поднимает PostgreSQL, Keycloak и приложение.

### 5.1. Файлы

- `Dockerfile` в корне (по гайду, раздел 6): многоэтапная сборка `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre`, сборка `mvn -pl app -am package`, кэш Maven (`--mount=type=cache`), запуск не от root.
- `.dockerignore` (`**/target`, `.git`, `.idea`, `*.iml`, `.vscode`, `.env`).
- `docker-compose.yml` с сервисами `postgres`, `keycloak`, `app`.
- `keycloak/realm-export.json` — начальный realm (см. 5.3).
- `db/init/` — каталог для SQL-скриптов схемы (если в этапе 1 нашёлся SQL-файл схемы, помести его туда или смонтируй; иначе оставь `.gitkeep` и добавь пометку в README).
- `.env.example` — список переменных с безопасными значениями-заглушками (если в этапе 1 нашлись недостающие).

### 5.2. docker-compose.yml: требования

**Общие:**
- Значения настроек берутся из `.env` (Compose подхватывает его автоматически из каталога проекта) через `${VAR}` или `${VAR:-default}`, используй имена переменных, найденные в этапе 1.
- Секреты в `docker-compose.yml` не пишутся.

**`postgres`:**
- Образ `postgres:16-alpine`.
- Переменные `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` из `.env`.
- Том для данных, монтирование `./db/init:/docker-entrypoint-initdb.d:ro`.
- `healthcheck` через `pg_isready`.
- Порт наружу из `.env` (по умолчанию 5432).

**`keycloak`:**
- Образ `quay.io/keycloak/keycloak:26.x`: выбери актуальный существующий тег 26.x (проверь, что `docker pull` его находит).
- Команда `start-dev --import-realm`.
- Администратор: `KC_BOOTSTRAP_ADMIN_USERNAME` и `KC_BOOTSTRAP_ADMIN_PASSWORD` из `.env`.
- `KC_HOSTNAME` с фиксированным внешним адресом (`http://localhost:<порт>`), чтобы поле `iss` в токенах не зависело от того, откуда пришёл запрос (см. гайд, раздел 6).
- Порт наружу из `.env` (по умолчанию 8180 → 8080 в контейнере).
- Монтирование `./keycloak/realm-export.json:/opt/keycloak/data/import/realm.json:ro`.
- Healthcheck: в образе нет `curl`, поэтому включи `KC_HEALTH_ENABLED=true` и проверяй готовность `/health/ready` на management-порту 9000 средствами оболочки (`/dev/tcp`) или другим способом без дополнительных утилит. Проверь, что он действительно переходит в `healthy`.

**`app`:**
- `build: .`.
- `depends_on`: `postgres` (`service_healthy`), `keycloak` (`service_healthy`, если healthcheck заработал, иначе `service_started`).
- Переменные: `DB_URL=jdbc:postgresql://postgres:5432/<БД>`, `DB_USER`, `DB_PASSWORD`; `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` (внешний адрес) и `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` (внутренний, `http://keycloak:8080/realms/<realm>/protocol/openid-connect/certs`).
- Порт наружу из `.env`.
- Для healthcheck приложения через `/actuator/health` в образе нужна утилита (`curl` или `wget`): добавь её в runtime-образ или обойдись проверкой снаружи в smoke-скрипте.

### 5.3. Начальный realm Keycloak

`realm-export.json` должен содержать:

- realm `servicedesk` (имя из `.env`, если задано);
- realm roles: `TEACHER`, `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF`, `DUTY_OFFICER`, `ADMIN`;
- client `servicedesk-frontend`: public, Standard flow, PKCE (`S256`), redirect URI и web origin для адреса фронтенда (спроси пользователя или используй `http://localhost:3000/*` и пометь как заглушку); **на время разработки** включи Direct Access Grants, чтобы smoke-тест мог получить токен по логину и паролю (запиши это в README как dev-only, для продакшена выключить);
- client scope с мапперами: *User Attribute* `department` → claim `department` (access token), *Audience* → `servicedesk-backend`; роли приходят в `realm_access.roles` по умолчанию;
- атрибут пользователя `department`: чтобы Keycloak его принимал, он должен быть объявлен в user profile realm (либо, для dev, включена политика неописанных атрибутов);
- тестовые пользователи, по одному на роль (`teacher`, `head`, `staff`, `duty`, `admin`), с паролями **только для разработки** (отметь это комментарием в README, в самом JSON комментариев нет) и атрибутом `department`.

Составлять такой JSON вручную рискованно. Надёжный способ: поднять Keycloak, настроить realm через Admin REST API или `kcadm.sh`, выгрузить его (`kc.sh export` или частичный экспорт) и сохранить результат в `keycloak/realm-export.json`. Выбери любой способ, главное, чтобы выполнялись критерии ниже.

### Критерии готовности этапа

- [ ] `docker compose config` проходит без ошибок и без предупреждений о неопределённых переменных.
- [ ] `docker compose up -d --build` запускает все три сервиса, `docker compose ps` показывает `postgres` и `keycloak` как `healthy`, `app` как `running`.
- [ ] Приложение подключилось к PostgreSQL (в логах нет ошибок Hibernate и Hikari, `Started ServiceDeskApplication`).
- [ ] Keycloak импортировал realm: консоль доступна, роли и тестовые пользователи созданы.
- [ ] Токен тестового пользователя содержит `realm_access.roles` с нужной ролью, claim `department`, `aud` с `servicedesk-backend`, а `iss` равен `http://localhost:<порт>/realms/<realm>`.

**Коммит:** `infra: docker-compose with postgres and keycloak, dockerfile, realm import`

---

## 6. Этап 6. Тесты запуска

**Цель:** автоматические проверки, что минимальная версия действительно работает.

### 6.1. Автотесты (Maven)

В модуле `app` (тесты уровня всего приложения, `src/test/java/rut/miit/servicedesk/`):

1. **`ModuleBoundariesTest`** (ArchUnit, код из гайда, раздел 5). Проверки:
   - классы вне модуля не зависят от `<module>.internal` (по модулям `auth`, `catalog`, `requests`, `notifications`);
   - `catalog` не зависит от `requests` и `notifications`;
   - между модулями нет циклов (`slices().matching("rut.miit.servicedesk.(*)..").should().beFreeOfCycles()`).
   Тест пишется на `ClassFileImporter` без JUnit-интеграции ArchUnit (не зависит от версии JUnit).

2. **`ApplicationStartupTest`** — `@SpringBootTest` с реальной PostgreSQL в Testcontainers (`@ServiceConnection`):
   - контекст поднимается;
   - `GET /actuator/health` без токена возвращает 200 и `UP`;
   - запрос к защищённому пути (например, `/api/requests`) без токена возвращает 401;
   - запрос к этому же пути с «подставным» JWT (через `spring-security-test`, `jwt()` с ролью) возвращает не 401 (ожидаемо 404, так как контроллеров пока нет).
   Для `issuer-uri`/`jwk-set-uri` в тестовом профиле укажи фиктивные адреса, чтобы тест не зависел от Keycloak (JWT-декодер создаётся лениво). Если без реального Keycloak контекст не поднимается, зафиксируй причину и реши минимально (например, тестовая конфигурация с `JwtDecoder` на заглушке).

Про Spring Boot 4: у тестовых аннотаций (`@AutoConfigureMockMvc` и других) в этой версии изменились пакеты из-за модульности. Если импорт не находится, найди правильный по документации или по содержимому jar, не обходи проблему сменой версии Spring Boot. Для Testcontainers проверь актуальные пакеты классов (`PostgreSQLContainer` в 2.x находится в другом пакете, чем в 1.x).

Если Docker недоступен, Testcontainers-тесты пометь `@Testcontainers(disabledWithoutDocker = true)`, а в отчёте явно укажи, что они были **пропущены**, а не пройдены.

3. В остальных модулях тестов пока не пишем: классов с логикой нет. Если Maven жалуется на пустые тесты, это не ошибка.

### 6.2. Smoke-скрипт

Создай `scripts/smoke-test.sh` (выполняемый, `set -euo pipefail`), который:

1. Поднимает окружение (`docker compose up -d --build`) и ждёт готовности `postgres`, `keycloak` и `app` с таймаутом (например, 180 секунд) и понятным сообщением при превышении.
2. Читает параметры (порты, realm) из окружения или `.env`, **не печатая секреты**.
3. Проверяет:
   - `GET /actuator/health` → 200 без токена;
   - `GET /api/requests` без токена → 401;
   - получение токена `teacher` у Keycloak (password grant, dev-only client) → непустой `access_token`;
   - `GET /api/requests` с этим токеном → **не 401** (ожидаемо 404);
   - в декодированном payload токена есть `realm_access.roles` с `TEACHER` и claim `department` (декодирование через `python3` или `jq`, без установки новых пакетов; если нет ни одного, пропусти эту проверку и сообщи).
4. Печатает итог по каждой проверке (`OK` / `FAIL`) и возвращает ненулевой код при любом `FAIL`.
5. Не останавливает контейнеры сам (опция `--down` для остановки по желанию).

### 6.3. Запуск

```bash
mvn -B -ntp verify                 # сборка и автотесты (нужен Docker для Testcontainers)
./scripts/smoke-test.sh            # проверка в docker compose
docker compose down                # остановка
```

При падении теста или проверки: разберись в причине, исправь код или конфигурацию и повтори. Не отключай и не ослабляй проверки, чтобы получить «зелёный» результат. Если проблема связана с гайдом, зафиксируй её в «Отклонениях».

**Критерии готовности:**
- [ ] `mvn -B -ntp verify` проходит (с указанием, были ли пропущены Testcontainers-тесты).
- [ ] `scripts/smoke-test.sh` завершается кодом 0, все проверки `OK`.
- [ ] `ModuleBoundariesTest` падает, если искусственно обратиться из `requests` к `catalog.internal` (проверь на временной правке и откати её).

**Коммит:** `test: startup tests, module boundary checks and smoke script`

---

## 7. Этап 7. Документация проекта

1. `README.md` в корне:
   - что это за проект и ссылка на `service-desk-guide.md`;
   - таблица модулей и зависимостей;
   - требования (Java 21, Maven 3.9+, Docker);
   - как запустить: режим разработки (`docker compose up -d postgres keycloak` и запуск приложения из IDE) и полный запуск (`docker compose up --build`);
   - как запустить тесты и smoke-скрипт;
   - тестовые пользователи Keycloak и пометка, что пароли только для разработки;
   - таблица переменных окружения (имена и смысл, **без значений**) и ссылка на `.env.example`;
   - как получить токен для проверки API (пример `curl`).
2. Проверь `.gitignore`: `target/`, `.idea/`, `*.iml`, `.vscode/`, `.env`, `*.log`.
3. Если появился `.env.example`, убедись, что в нём нет реальных секретов.

**Коммит:** `docs: readme and environment reference`

---

## 8. Итоговый отчёт

Когда все этапы пройдены (или остановлены по правилам из раздела 0), выведи отчёт со структурой:

1. **Что сделано:** кратко по этапам, список созданных и изменённых файлов.
2. **Итоговые версии:** Java, Maven, Spring Boot, springdoc, ArchUnit, Testcontainers, образы Postgres и Keycloak.
3. **Результаты проверок:** для каждой команды из этапов 2, 5, 6 что запускалось и что получилось (пройдено, не пройдено, пропущено).
4. **Отклонения от гайда:** что пришлось исправить и почему (в том числе переименованные артефакты).
5. **Не удалось проверить:** например, из-за недоступного Docker.
6. **Вопросы к пользователю:** недостающие переменные в `.env`, адрес фронтенда для Keycloak, расхождения со схемой БД и т.п.
7. **Что делать дальше:** следующие шаги для команды (реализация модулей по заготовкам, доработка схемы БД, настройка маппера `department` и т.д.).

Не включай в отчёт значения секретов из `.env`.

---

## Определение «готово» для всего плана

- [ ] Проект собирается: `mvn -B -ntp -DskipTests package`.
- [ ] Зависимости проверены, дубликатов и обратных зависимостей между модулями нет.
- [ ] Все заготовки классов содержат Javadoc по шаблону, Spring/JPA-аннотаций на них нет.
- [ ] `docker compose up -d --build` поднимает PostgreSQL, Keycloak и приложение, все контейнеры в нормальном состоянии.
- [ ] Автотесты и smoke-скрипт проходят.
- [ ] Архитектурные тесты защищают границы модулей.
- [ ] В репозитории нет секретов, `.env` не закоммичен.
- [ ] README описывает запуск, итоговый отчёт составлен.
