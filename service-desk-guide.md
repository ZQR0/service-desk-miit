# Гайд по разработке многомодульного монолита (сервис-деск)
# Гайд по разработке многомодульного монолита (сервис-деск)

Практическое руководство для команды: как устроены модули, где лежат сущности, как модули общаются, как запускать всё в Docker и какие правила помогают не превратить монолит в кашу.

Базовые параметры проекта: Java 21, Spring Boot 4.0.x, Maven, PostgreSQL (схема уже готова), Keycloak для авторизации, base package `rut.miit.servicedesk`.

## Содержание

1. [Принципы и устройство проекта](#1-принципы-и-устройство-проекта)
2. [Сущности: где лежат и как связаны](#2-сущности-где-лежат-и-как-связаны)
3. [Как модули общаются](#3-как-модули-общаются)
4. [Безопасность и Keycloak](#4-безопасность-и-keycloak)
5. [Тестирование и защита границ](#5-тестирование-и-защита-границ)
6. [Запуск в Docker](#6-запуск-в-docker)
7. [Командная работа](#7-командная-работа)
8. [Частые ошибки](#8-частые-ошибки)
9. [Чек-листы](#9-чек-листы)

---

## 1. Принципы и устройство проекта

Один Spring Boot приложение, разбитое на Maven-модули по доменам. Каждый модуль владеет своими данными и логикой, а с остальными общается только через публичную часть.

### Модули и зависимости

| Модуль | Назначение | Зависит от |
|---|---|---|
| `common` | общие утилиты, формат ошибок, без бизнес-логики | — |
| `auth` | локальный справочник пользователей и кафедр, настройка безопасности, `CurrentUser` | `common` |
| `catalog` | каталог услуг, шаблоны чек-листов | `common` (и `auth`, только если нужен «автор изменения») |
| `requests` | заявки, согласование, статусы, чек-листы, история, статистика SLA | `common`, `auth`, `catalog` |
| `notifications` | уведомления, SSE | `common`, `auth`, `requests` (только события) |
| `app` | точка входа, конфигурация, сборка jar | все модули |

Зависимости идут в одну сторону, циклов быть не должно (Maven их не пропустит).

### Структура репозитория

```
service-desk/
├── pom.xml                   # родительский: версии и список модулей
├── common/
├── auth/
├── catalog/
├── requests/
├── notifications/
├── app/                      # @SpringBootApplication, application.yml
├── docker/ или корень        # Dockerfile, docker-compose.yml, keycloak/realm-export.json
└── .github/CODEOWNERS
```

### Раскладка внутри модуля

Публичное лежит в корне пакета модуля, всё остальное в `internal`:

```
requests/src/main/java/rut/miit/servicedesk/requests/
├── RequestService.java           # public interface: что умеет модуль
├── RequestDto.java               # public record
├── RequestStatus.java            # public enum (нужен в DTO и событиях)
├── RequestCreated.java           # public record: событие
└── internal/
    ├── RequestServiceImpl.java   # package-private, @Service
    ├── RequestEntity.java        # package-private, @Entity
    ├── RequestRepository.java    # package-private
    └── RequestController.java    # package-private, @RestController
```

Правила:

- **Публично только то, что нужно другим модулям:** интерфейсы сервисов, DTO, enum, события.
- **`internal` плоский.** Классы без `public` видны только в своём пакете, поэтому подпапки внутри `internal` не делайте: в них пришлось бы открывать классы.
- **Сущности наружу не выходят.** Другим модулям отдаются только DTO и `record`.
- **Контроллер принадлежит модулю.** Префиксы путей: `/api/auth`, `/api/catalog`, `/api/requests`, `/api/notifications`.
- **Главный класс лежит в корневом пакете** `rut.miit.servicedesk` (в `app`), поэтому Spring сам находит бины, сущности и репозитории всех модулей. Условие: пакеты всех модулей начинаются с `rut.miit.servicedesk`.

### Зачем так

Если позже понадобится вынести модуль в отдельный сервис, интерфейс сервиса заменяется HTTP-клиентом, а события Spring заменяются сообщениями брокера. Остальной код не трогается.

---

## 2. Сущности: где лежат и как связаны

Сущность принадлежит модулю, который владеет её данными. Лежит она в `internal` своего модуля.

### Размещение

| Модуль | Сущности (таблицы) |
|---|---|
| `auth` | `UserEntity` (`users`), `DepartmentEntity` (`department`), при необходимости роли (`role`/`user_role`) |
| `catalog` | `ServiceItemEntity` (`service_catalog`), `ChecklistTemplateItemEntity` (`checklist_template_item`) |
| `requests` | `RequestEntity` (`request`), `AudienceEntity` (`audience`), `RequestChecklistItemEntity` (`request_checklist_item`), `RequestStatusHistoryEntity` (`request_status_history`) |
| `notifications` | `NotificationEntity` (`notification`) |
| `common` | сущностей нет, максимум `@MappedSuperclass` с общими полями (`createdAt`, `updatedAt`) |

`audience` (кабинет) лежит в `requests`, потому что нужен только заявкам, и так не появляется лишний вызов между модулями.

### Правила связей

1. **Внутри модуля** связи JPA обычные (`@ManyToOne`, `@OneToMany`): заявка и её пункты чек-листа, заявка и кабинет.
2. **Между модулями** связей JPA нет. Заявка хранит `userId`, `serviceId` как обычные поля `Long`, а нужные данные получает через интерфейс другого модуля.
3. **Внешние ключи на уровне БД** можно оставить: они защищают целостность и модулям не мешают. Но в JPA они не маппятся на сущности чужого модуля.
4. **Снимки вместо постоянных обращений.** В заявке сохраняются `departmentId` автора, при желании название услуги и сами пункты чек-листа (копия шаблона). Тогда список заявок не требует вызовов в соседние модули, а изменение каталога не ломает старые заявки.
5. **Запросы только к своим таблицам.** Никаких JOIN с таблицами другого модуля в JPQL и нативных запросах.

### Пример

```java
// requests/internal
@Entity
@Table(name = "request")
class RequestEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;          // автор: таблица users, модуль auth

    @Column(name = "service_id", nullable = false)
    private Long serviceId;       // услуга: таблица service_catalog, модуль catalog

    @Column(name = "department_id", nullable = false)
    private Long departmentId;    // снимок кафедры автора на момент создания

    private Long approverId;
    private Long executorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_id")
    private AudienceEntity audience;                      // тот же модуль: обычная связь

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestChecklistItemEntity> checklist = new ArrayList<>();

    // description, rejectReason, createdAt, updatedAt, dueAt ...

    protected RequestEntity() {}   // нужен JPA
}
```

### Работа с готовой схемой БД

- В `application.yml` стоит `ddl-auto: validate`: Hibernate ничего не меняет, только проверяет, что маппинг совпадает с таблицами. Названия таблиц и столбцов указываются явно (`@Table`, `@Column`).
- Если миграции всё же понадобятся, подключите Flyway (стартеры закомментированы в `app/pom.xml`) и держите скрипты каждого модуля в отдельной папке.
- **Доработки схемы, которые рекомендуется внести:**
  - таблицы `checklist_template_item`, `request_checklist_item`, `request_status_history`;
  - в `request` один `status` вместо `status_id` и `is_aproved`, плюс `department_id`, `updated_at`, `due_at`, `confirmed_by` (второй этап согласования);
  - в `service_catalog` поле `sla_hours` вместо `deadline` и `is_active`;
  - в `department` поле `head_user_id`;
  - в `users` убрать `login` и `password_hash` (пароли в Keycloak), добавить `keycloak_id uuid unique`;
  - индексы на `request(status)`, `(user_id)`, `(executor_id)`, `(department_id)`, `(created_at)`.

### Пользователи и Keycloak

В Keycloak хранятся логины, пароли и роли. В таблице `users` остаётся локальный справочник (ФИО, кафедра, `keycloak_id`), нужный для связей и поиска получателей уведомлений. Запись создаётся (или обновляется) при первом входе по данным токена (`sub`, имя, `department`). Таким образом `Long`-идентификаторы в сущностях и событиях сохраняются, а токен сопоставляется с локальным пользователем через `keycloak_id`.

---

## 3. Как модули общаются

Правило: если нужен ответ прямо сейчас, это синхронный вызов интерфейса модуля. Если нужно сообщить, что что-то произошло, это событие.

### Карта взаимодействий

| Откуда → куда | Способ | Зачем |
|---|---|---|
| `requests` → `catalog` | вызов `CatalogService.getActive(id)` | услуга и шаблон чек-листа при создании заявки, в одной транзакции |
| `requests` → `auth` | вызов (`CurrentUser`, `UserDirectory`) | кто автор, какая у него кафедра, является ли исполнитель дежурным |
| `requests` → `notifications` | событие | `requests` публикует, не зная, кто слушает |
| `notifications` → `requests` | только классы событий | сервисы `requests` не вызываются |
| `notifications` → `auth` | вызов `UserDirectory` | найти получателей (зав. кафедрой, сотрудники отдела) |
| `catalog` → `auth` | почти не нужен | роли проверяются аннотациями Spring Security |

Чего не делать:

- Обратные зависимости (`catalog → requests`, `auth → requests`): граф зависимостей остаётся направленным.
- Передавать entity между модулями: только DTO и `record`.
- Класть в событие только идентификаторы: добавляйте всё, что нужно для текста уведомления (название услуги, причина отказа), чтобы слушатель не ходил за данными обратно.

### Синхронный вызов

```java
// catalog (публичный пакет)
public interface CatalogService {
    ServiceItemDto getActive(long id);   // бросает NotFoundException, если услуги нет или она неактивна
}
public record ServiceItemDto(long id, String title, int slaHours, List<ChecklistItemDto> checklist) {}
```

```java
// catalog/internal
@Service
class CatalogServiceImpl implements CatalogService {
    private final ServiceItemRepository repo;
    CatalogServiceImpl(ServiceItemRepository repo) { this.repo = repo; }

    @Override
    @Transactional(readOnly = true)
    public ServiceItemDto getActive(long id) {
        var e = repo.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new NotFoundException("Услуга не найдена"));
        return ServiceItemMapper.toDto(e);
    }
}
```

```java
// requests/internal
@Service
class RequestServiceImpl implements RequestService {
    private final RequestRepository repo;
    private final CatalogService catalog;                  // из модуля catalog
    private final CurrentUser currentUser;                 // из модуля auth
    private final ApplicationEventPublisher events;

    @Transactional
    public RequestDto create(CreateRequestCommand cmd) {
        var item = catalog.getActive(cmd.serviceId());
        var req = RequestEntity.create(currentUser.id(), currentUser.departmentId(), item, cmd);
        repo.save(req);
        events.publishEvent(new RequestCreated(
            req.getId(), req.getUserId(), req.getDepartmentId(), item.title(), Instant.now()));
        return RequestMapper.toDto(req);
    }
}
```

Всё происходит в одном процессе и одной транзакции: если сохранение упадёт, событие не уйдёт и ничего не останется наполовину.

### События

Класс события лежит в публичной части модуля, который его публикует:

```java
// requests (публичный пакет)
public record RequestCreated(long requestId, long requesterId, long departmentId,
                             String serviceTitle, Instant occurredAt) {}
```

Слушатель в другом модуле:

```java
// notifications/internal
@Component
class RequestEventsListener {
    private final UserDirectory users;                 // из модуля auth
    private final NotificationService notifications;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(RequestCreated e) {
        users.headOfDepartment(e.departmentId()).ifPresent(head ->
            notifications.send(head.id(), "Новая заявка №" + e.requestId() + ": " + e.serviceTitle()));
    }
}
```

Что здесь важно:

- **`AFTER_COMMIT`:** слушатель срабатывает только если транзакция публикатора зафиксировалась, то есть уведомление не уйдёт о заявке, которой нет.
- **`REQUIRES_NEW`:** после коммита исходной транзакции записи слушателя без новой транзакции не сохранятся.
- **`@Async`:** слушатель не задерживает ответ пользователю. Требует `@EnableAsync` (уже стоит в главном классе).
- **Ошибка слушателя не должна ломать заявку.** Исключение логируется, основная операция уже завершена.
- **События лежат в памяти.** При падении приложения между коммитом и обработкой событие теряется. Для MVP допустимо; если станет важно, подключите Spring Modulith (`@ApplicationModuleListener` с сохранением публикаций в БД) или пересчитывайте уведомления по истории статусов.
- **Не публикуйте события из слушателя в цикле**, это путает порядок и транзакции.

### Заглушки для параллельной работы

Пока соседний модуль не готов, подкоманда не ждёт, а пользуется заглушкой его публичного интерфейса:

- в тестах: Mockito (`@Mock CatalogService`);
- при запуске: бин-заглушка в профиле `stub`, возвращающий фиксированные данные.

```java
// app (или модуль потребителя), профиль stub
@Component @Profile("stub")
class StubCatalogService implements CatalogService {
    public ServiceItemDto getActive(long id) {
        return new ServiceItemDto(id, "Тестовая услуга", 24, List.of(/* ... */));
    }
}
```

Когда настоящий модуль готов, профиль `stub` просто не включают (и бин заглушки не должен конфликтовать с настоящим: профиль или `@ConditionalOnMissingBean`).

### Контракт сначала

Публичные интерфейсы, DTO и события согласуются **до** реализации и коммитятся первыми. Изменение публичной части идёт через pull request с ревью всех подкоманд-потребителей. Это самое важное правило параллельной работы.

---

## 4. Безопасность и Keycloak

Keycloak отвечает за вход, пароли, выдачу токенов и роли. Модуль `auth` только проверяет токены и даёт остальным доступ к данным пользователя.

### Настройка Keycloak

1. **Realm** `servicedesk`.
2. **Realm roles:** `TEACHER`, `DEPARTMENT_HEAD`, `DEPARTMENT_STAFF`, `DUTY_OFFICER`, `ADMIN`.
3. **Атрибут `department`:** объявить в *Realm settings → User profile* (неописанные атрибуты по умолчанию отключены) и заполнить у пользователей.
4. **Client `servicedesk-frontend`:** public, Standard flow с PKCE, указать `Valid redirect URIs` и `Web origins`.
5. **Client scope** с мапперами: *User Attribute* `department` → claim `department`; *Audience* → `servicedesk-backend`. Роли по умолчанию приходят в `realm_access.roles`.
6. **Client `servicedesk-backend`** (confidential, Service accounts): нужен, только если бэкенд обращается к Admin API (например, искать пользователей по роли). Ему выдаются роли `view-users` и `query-users` клиента `realm-management`. Для проверки токенов он не нужен.
7. **Время жизни access token:** 5–15 минут, фронтенд обновляет токен через refresh token.
8. Тестовые пользователи по одному на каждую роль, с заполненным `department`.

### Настройка Spring

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER:http://localhost:8180/realms/servicedesk}
```

```java
// auth/internal
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realm = jwt.getClaim("realm_access");
            @SuppressWarnings("unchecked")
            List<String> roles = realm == null ? List.of() : (List<String>) realm.get("roles");
            return roles.stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        });
        return converter;
    }
}
```

После этого в любом модуле работает `@PreAuthorize("hasRole('DEPARTMENT_HEAD')")`. Проверять надо не только роль, но и принадлежность данных: зав. кафедрой может согласовывать только заявки своей кафедры (`request.departmentId == currentUser.departmentId()`).

### `CurrentUser`

Публичный интерфейс модуля `auth`, который читает токен из `SecurityContext` и сопоставляет `sub` с локальным пользователем через `keycloak_id`:

```java
public interface CurrentUser {
    long id();                    // локальный id из таблицы users
    String fullName();
    Long departmentId();
    boolean hasRole(String role);
}
```

### SSE и токен

Стандартный браузерный `EventSource` не умеет отправлять заголовок `Authorization`. Для Keycloak решение: на фронтенде использовать `@microsoft/fetch-event-source`, которая передаёт `Bearer`-токен. Договоритесь об этом с фронтендом заранее.

### CORS

Если фронтенд на другом адресе, настройте CORS в `SecurityConfig` (`http.cors(...)`) и добавьте адрес фронтенда в `Web origins` клиента Keycloak.

---

## 5. Тестирование и защита границ

### Уровни тестов

| Уровень | Что проверяет | Инструменты |
|---|---|---|
| Юнит | сервисы и логика переходов статусов, без Spring | JUnit, Mockito |
| Срез | контроллеры, репозитории одного модуля | `@WebMvcTest`, `@DataJpaTest` + Testcontainers |
| Модуль | один модуль целиком, соседи подменены заглушками | `@SpringBootTest` + Mockito-бины |
| Интеграция | сквозной сценарий по всему приложению | `@SpringBootTest` в `app` + Testcontainers |

Для тестов репозиториев используйте реальную PostgreSQL через Testcontainers, а не H2: диалекты различаются, и ошибки вылезут уже в продакшене.

### Защита границ модулей

Раз отдельных `api`/`impl`-модулей нет, правила проверяет ArchUnit. Тест лежит в `app` и падает в CI при нарушении:

```java
class ModuleBoundariesTest {

    private static final JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("rut.miit.servicedesk");

    @ParameterizedTest
    @ValueSource(strings = {"auth", "catalog", "requests", "notifications"})
    void internalPackagesAreNotUsedByOtherModules(String module) {
        noClasses().that().resideOutsideOfPackage("..servicedesk." + module + "..")
            .should().dependOnClassesThat().resideInAPackage("..servicedesk." + module + ".internal..")
            .check(classes);
    }

    @Test
    void catalogDoesNotDependOnRequestsOrNotifications() {
        noClasses().that().resideInAPackage("..servicedesk.catalog..")
            .should().dependOnClassesThat().resideInAnyPackage("..servicedesk.requests..", "..servicedesk.notifications..")
            .check(classes);
    }

    @Test
    void modulesAreFreeOfCycles() {
        slices().matching("rut.miit.servicedesk.(*)..").should().beFreeOfCycles().check(classes);
    }
}
```

Зависимость в `app/pom.xml`: `com.tngtech.archunit:archunit` (scope `test`). Тест написан без интеграции с JUnit-движком ArchUnit, поэтому не зависит от версии JUnit.

### CI

Минимум для каждого pull request:

```yaml
# .github/workflows/ci.yml
name: ci
on: [pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: maven }
      - run: mvn -B verify
```

Testcontainers в GitHub Actions работает «из коробки», Docker там уже есть. Если у вас другой CI (GitLab и т.п.), нужна та же команда `mvn -B verify` и доступ к Docker.

---

## 6. Запуск в Docker

Схема: PostgreSQL, Keycloak и приложение в одной сети Docker Compose.

### Dockerfile (в корне проекта)

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -pl app -am package

FROM eclipse-temurin:21-jre
RUN useradd -r -u 1001 app
WORKDIR /app
COPY --from=build /workspace/app/target/service-desk.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```

- `-pl app -am` собирает модуль `app` и все модули, от которых он зависит.
- Кэш Maven (`--mount=type=cache`) ускоряет повторные сборки (нужен BuildKit, в современном Docker он включён).
- Приложение работает не от root.

`.dockerignore`:

```
**/target
.git
.idea
*.iml
.vscode
```

### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: servicedesk
      POSTGRES_USER: servicedesk
      POSTGRES_PASSWORD: servicedesk
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d:ro     # sql-скрипты схемы, выполняются при первом запуске
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U servicedesk -d servicedesk"]
      interval: 5s
      timeout: 3s
      retries: 10

  keycloak:
    image: quay.io/keycloak/keycloak:26.4          # подставьте актуальный тег 26.x
    command: start-dev --import-realm
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
      KC_HOSTNAME: http://localhost:8180            # фиксирует адрес в поле iss токенов
    ports:
      - "8180:8080"
    volumes:
      - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm.json:ro

  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_started
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/servicedesk
      DB_USER: servicedesk
      DB_PASSWORD: servicedesk
      # издатель токенов: адрес, который видит браузер
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://localhost:8180/realms/servicedesk
      # ключи подписи: адрес внутри сети Docker
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/servicedesk/protocol/openid-connect/certs
    ports:
      - "8080:8080"

volumes:
  pgdata:
```

### Важный нюанс с Keycloak в Docker

Токен содержит поле `iss` с адресом, по которому к Keycloak обращался браузер (`http://localhost:8180/...`). А приложение внутри Docker видит Keycloak по другому адресу (`http://keycloak:8080`). Если указать только `issuer-uri`, Spring будет брать метаданные по внутреннему адресу и получит несовпадение издателя, а проверка токена упадёт.

Решение, как в compose выше:

- `KC_HOSTNAME` фиксирует адрес, который Keycloak подставляет в токены;
- `issuer-uri` задаёт ожидаемое значение `iss`;
- `jwk-set-uri` указывает на внутренний адрес для загрузки ключей. Когда он задан, Spring берёт ключи оттуда, а `issuer-uri` использует только для проверки.

### Готовая БД

Если у вас уже есть работающая PostgreSQL с готовой схемой, сервис `postgres` из compose убирается, а в `DB_URL` указывается её адрес (из контейнера хост машины доступен как `host.docker.internal`, на Linux добавьте `extra_hosts: ["host.docker.internal:host-gateway"]`). Если БД будет создаваться заново, положите SQL со схемой в `db/init/`: он выполнится при первом старте контейнера.

### Два режима работы

1. **Разработка (удобно для отладки).** В Docker поднимаются только зависимости, а приложение запускается из IDE:
   ```bash
   docker compose up -d postgres keycloak
   ```
   Затем запустите `ServiceDeskApplication` с профилем `dev`. Подключение к БД идёт на `localhost:5432`, `iss` совпадает, потому что `KC_HOSTNAME` зафиксирован.
2. **Полный запуск (проверка сборки образа, демонстрация).**
   ```bash
   docker compose up --build
   ```

### Полезные команды

```bash
docker compose logs -f app          # логи приложения
docker compose up -d --build app    # пересобрать и перезапустить только приложение
docker compose down                 # остановить
docker compose down -v              # остановить и удалить данные БД (чистый старт)
```

### Что стоит знать

- **Keycloak в `start-dev`** хранит данные во встроенной БД и подходит только для разработки. Для демонстрации на постоянном сервере используйте режим `start` с отдельной БД и настроенным HTTPS.
- **Realm-экспорт** (`keycloak/realm-export.json`) кладите в репозиторий: тогда у всех в команде одинаковые роли, клиенты и тестовые пользователи. Его удобно получить, настроив realm вручную и выгрузив через *Realm settings → Action → Partial export* (или командой экспорта Keycloak).
- **Секреты** (пароли БД, секрет клиента) в репозиторий не коммитьте: файл `.env` и переменные окружения, а в compose ссылки вида `${DB_PASSWORD}`.
- **Часовые пояса:** всё хранится в UTC (`hibernate.jdbc.time_zone: UTC`), перевод в местное время делает фронтенд.
- **Память:** `-XX:MaxRAMPercentage=75` заставляет JVM уважать лимит памяти контейнера.

---

## 7. Командная работа

### Владение модулями

Файл `.github/CODEOWNERS` закрепляет ревьюера за каждым модулем:

```
/auth/          @user-a @user-b
/catalog/       @user-a @user-b
/requests/      @user-c @user-d @user-e
/notifications/ @user-f @user-g
/pom.xml        @tech-lead
/app/           @tech-lead
/common/        @tech-lead
```

Общий код (родительский `pom.xml`, `app`, `common`) принадлежит одному человеку, чтобы не плодить конфликты. Правки в нём небольшие и частые.

### Правила pull request

- Правки чужого модуля идут только через pull request с ревью владельца.
- Изменение публичной части (интерфейс, DTO, событие) требует согласия всех команд-потребителей. В описании pull request перечислите, кого это затрагивает.
- Каждый pull request проходит CI: сборка, тесты, проверка границ модулей.
- Основная ветка всегда собирается и запускается.
- Небольшие pull request лучше крупных: меньше конфликтов, быстрее ревью.

### Порядок работы над фичей

1. Договориться о публичных интерфейсах, DTO и событиях (контракт).
2. Закоммитить контракт, чтобы подкоманды могли писать против него.
3. Реализовывать модуль, подменяя чужие модули заглушками.
4. Вливать в основную ветку маленькими частями, а не одним огромным pull request в конце.
5. Заменить заглушки настоящими модулями и прогнать сквозной сценарий.

### Определение «готово» для модуля

- Публичные интерфейсы реализованы и покрыты тестами.
- Контроллеры возвращают ошибки в договорённом формате, права доступа проверены.
- Тесты репозиториев проходят на реальной PostgreSQL (Testcontainers).
- Архитектурные тесты проходят.
- В README модуля описано: за что отвечает, что отдаёт наружу, какие события публикует и слушает.

---

## 8. Частые ошибки

| Ошибка | Чем плоха | Как избежать |
|---|---|---|
| Модули используют `internal` друг друга | границы существуют только на бумаге | ArchUnit в CI, package-private классы |
| `@ManyToOne` между сущностями разных модулей | модули срастаются через ORM | хранить идентификатор, данные брать через интерфейс |
| JOIN с таблицами чужого модуля | то же самое на уровне БД | снимки (`departmentId`), вызов интерфейса |
| Entity возвращается из публичного метода | утечка внутренней модели, ленивая загрузка вне транзакции | отдавать DTO |
| Публикация события и запись в БД в слушателе без `REQUIRES_NEW` | запись слушателя не сохраняется | `AFTER_COMMIT` + `REQUIRES_NEW` |
| Забыли `@EnableAsync` | `@Async` молча не работает | включено в главном классе |
| Циклическая зависимость модулей | не собирается или раздувает связность | пересмотреть границу, вынести событие |
| Бизнес-логика в `common` | `common` превращается в свалку | в `common` только утилиты |
| Ленивая загрузка в контроллере (`open-in-view`) | скрытые запросы и N+1 | `open-in-view: false` (уже в конфигурации), DTO, `JOIN FETCH` где нужно |
| Статистика SLA считается в памяти по всем заявкам | медленно при росте данных | агрегирующие запросы, индексы |
| Утечка `SseEmitter` | память и соединения | удалять эмиттер при `onCompletion`, `onTimeout`, `onError`, слать keep-alive |
| Пароли и секреты в репозитории | утечка | `.env`, переменные окружения |
| Проверка только роли, без проверки владельца данных | можно открыть чужую заявку | проверять и роль, и принадлежность (кафедра, автор, исполнитель) |
| Токен Keycloak не проходит проверку в Docker | разные адреса для браузера и контейнера | `KC_HOSTNAME`, `issuer-uri` и `jwk-set-uri` как в разделе 6 |

---

## 9. Чек-листы

### Перед началом разработки

- [ ] Согласованы публичные интерфейсы, DTO и события.
- [ ] Определены роли, статусы заявки и допустимые переходы.
- [ ] Схема БД доработана (чек-листы, история статусов, `department_id` в заявке и т.д.).
- [ ] Развёрнут Keycloak с realm, ролями, клиентами и тестовыми пользователями.
- [ ] Настроен CI и проверка границ модулей.
- [ ] Заведён `CODEOWNERS`.
- [ ] Подготовлены заглушки соседних модулей.

### Перед слиянием pull request

- [ ] Публичная часть не менялась, либо изменение согласовано с потребителями.
- [ ] Нет обращений к `internal` чужого модуля, нет новых циклов.
- [ ] Нет `@ManyToOne` и JOIN на таблицы других модулей.
- [ ] Из публичных методов возвращаются DTO, а не entity.
- [ ] Новые эндпоинты защищены ролями и проверкой владельца данных.
- [ ] Тесты проходят локально и в CI.
- [ ] Обновлён README модуля, если изменилось поведение или контракт.

### Перед сдачей

- [ ] Сквозной сценарий проходит: создание заявки → одобрение зав. кафедрой → подтверждение отделом и назначение → выполнение с чек-листом → закрытие.
- [ ] Негативные сценарии: отказ на каждом этапе, отмена, действие не той ролью, чужая заявка.
- [ ] Уведомления доходят в браузер без перезагрузки страницы.
- [ ] Статистика SLA показывает осмысленные значения.
- [ ] `docker compose up --build` поднимает всё с нуля.
- [ ] Подготовлены демонстрационные данные и тестовые учётные записи.
- [ ] Общий README: архитектура, роли, как запустить, как пройти основной сценарий.
