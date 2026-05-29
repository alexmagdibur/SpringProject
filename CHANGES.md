# Отчёт об изменениях — Лабораторная работа №6

## Содержание
1. [Общее описание](#1-общее-описание)
2. [Структура проекта после изменений](#2-структура-проекта-после-изменений)
3. [pom.xml — новые зависимости и плагины](#3-pomxml--новые-зависимости-и-плагины)
4. [Изменения существующих файлов](#4-изменения-существующих-файлов)
5. [Новые конфигурационные классы](#5-новые-конфигурационные-классы)
6. [Аннотация RequiresRole](#6-аннотация-requiresrole)
7. [AOP-аспекты](#7-aop-аспекты)
8. [Новый сервис аудита](#8-новый-сервис-аудита)
9. [REST-контроллеры](#9-rest-контроллеры)
10. [Тесты](#10-тесты)
11. [Нетривиальные технические решения](#11-нетривиальные-технические-решения)
12. [API-справочник](#12-api-справочник)

---

## 1. Общее описание

Проект был преобразован из **консольного приложения** (LabWork 5) в **Spring MVC REST API** с
встроенным сервером Apache Tomcat. Ключевые нововведения:

| Что добавлено | Технология |
|---|---|
| HTTP-сервер без Spring Boot | Embedded Tomcat 11.0.2 |
| REST-контроллеры (4 шт.) | Spring MVC 7.0.7 (`@RestController`) |
| Проверка ролей | AOP + кастомная аннотация `@RequiresRole` |
| Журнал вызовов сервисов | AOP `@Before` на все методы `service.impl.*` |
| JSON-сериализация | Jackson Databind 2.19.0 |
| Swagger UI | springdoc-openapi + swagger-ui webjar |
| Тесты контроллеров | MockMvc (standalone + полный Spring-контекст) |

Все старые сервисы, `AppConfig.java`, `Creature.java` и `creatures.csv` **сохранены без изменений
в бизнес-логике**. Никакого Spring Boot — запрещено условием задания.

---

## 2. Структура проекта после изменений

```
src/main/java/ru/bmstu/
├── App.java                              ← изменён (embedded Tomcat)
├── annotation/
│   └── RequiresRole.java                ← НОВЫЙ
├── aspect/
│   ├── RoleCheckAspect.java             ← НОВЫЙ
│   └── AuditLogAspect.java              ← НОВЫЙ
├── config/
│   ├── AppConfig.java                   ← без изменений
│   ├── WebConfig.java                   ← НОВЫЙ
│   └── OpenApiConfig.java               ← НОВЫЙ
├── controller/
│   ├── AdoptionController.java          ← НОВЫЙ
│   ├── AdoptionRequest.java             ← НОВЫЙ (DTO)
│   ├── CreatureController.java          ← НОВЫЙ
│   ├── StatisticsController.java        ← НОВЫЙ
│   └── StatusController.java            ← НОВЫЙ
├── domain/
│   └── Creature.java                    ← без изменений
├── reader/
│   └── CreatureCsvReader.java           ← изменён (classloader fix)
├── service/
│   ├── AdoptionService.java             ← без изменений
│   ├── AuditLogService.java             ← НОВЫЙ
│   ├── CreatureService.java             ← без изменений
│   └── ShelterStatisticsService.java    ← без изменений
└── service/impl/
    ├── AdoptionServiceImpl.java         ← без изменений
    ├── AuditLogServiceImpl.java         ← НОВЫЙ
    ├── CreatureServiceImpl.java         ← без изменений
    └── ShelterStatisticsServiceImpl.java← без изменений

src/test/java/ru/bmstu/
├── controller/
│   ├── AdoptionControllerTest.java      ← НОВЫЙ
│   ├── CreatureControllerRoleTest.java  ← НОВЫЙ (интеграционный)
│   ├── CreatureControllerTest.java      ← НОВЫЙ
│   ├── StatisticsControllerTest.java    ← НОВЫЙ
│   └── StatusControllerTest.java        ← НОВЫЙ
└── service/impl/
    ├── AdoptionServiceImplTest.java     ← НОВЫЙ
    ├── CreatureServiceImplTest.java     ← без изменений
    └── ShelterStatisticsServiceImplTest.java ← НОВЫЙ

src/test/resources/
└── mockito-extensions/
    └── org.mockito.plugins.MockMaker    ← НОВЫЙ (конфиг Mockito)
```

---

## 3. pom.xml — новые зависимости и плагины

**Файл:** [`pom.xml`](pom.xml)

### 3.1 Новые свойства (строки 15–16)

```xml
<spring.version>7.0.7</spring.version>
<tomcat.version>11.0.2</tomcat.version>
```

Вынесены в свойства, чтобы не дублировать версию в каждой зависимости.

### 3.2 Флаг компилятора `-parameters` (строки 26–28)

```xml
<compilerArgs>
    <arg>-parameters</arg>
</compilerArgs>
```

**Зачем:** Spring MVC использует рефлексию для определения имён параметров методов
(`@PathVariable String id`, `@RequestParam String name`). Без флага `-parameters` компилятор
Java не сохраняет имена параметров в байткоде, и Spring не может их определить →
`IllegalArgumentException: Name for argument not specified`.

### 3.3 Новые зависимости

| Артефакт | Версия | Строки | Назначение |
|---|---|---|---|
| `spring-webmvc` | 7.0.7 | 72–76 | DispatcherServlet, @RestController, MockMvc |
| `spring-aop` | 7.0.7 | 79–83 | AOP-инфраструктура Spring |
| `aspectjweaver` | 1.9.22 | 84–88 | Реализация AspectJ для `@Aspect`, pointcut'ов |
| `tomcat-embed-core` | 11.0.2 | 91–95 | Встроенный HTTP-сервер |
| `tomcat-embed-jasper` | 11.0.2 | 96–100 | Поддержка JSP (требование задания) |
| `jackson-databind` | 2.19.0 | 103–107 | Сериализация объектов в JSON и обратно |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.6 | 110–114 | Swagger/OpenAPI документация |
| `swagger-ui` (webjar) | 5.32.6 | 115–119 | Статические файлы Swagger UI |
| `webjars-locator-core` | 0.59 | 120–124 | Автоопределение версии webjars |
| `junit-jupiter` | **6.1.0** | 136–140 | JUnit 6 (обязателен для Spring Test 7) |
| `mockito-core` | 5.11.0 | 141–146 | Мок-объекты в тестах |
| `spring-test` | 7.0.7 | 147–152 | MockMvc, @ContextConfiguration |
| `hamcrest` | 2.2 | 153–158 | Матчеры для MockMvc assertions |
| `json-path` | 2.9.0 | 159–164 | Парсинг JSON в тестах (`jsonPath("$.field")`) |

> **Важное замечание про springdoc:** В задании указан артефакт
> `springdoc-openapi-webmvc-core 2.8.x`, однако такой артефакт в Maven Central существует
> **только до версии 1.8.0**. Начиная с версии 2.x springdoc переименовал модуль в
> `springdoc-openapi-starter-webmvc-ui`. Использован корректный актуальный артефакт.

> **Важное замечание про JUnit:** `spring-test 7.0.7` скомпилирован против JUnit 6 API
> (метод `ExtensionContext.Store.computeIfAbsent(Object, Function, Class)` появился только
> в JUnit 6). Ни одна версия JUnit 5.x этот метод не содержит — там он называется
> `getOrComputeIfAbsent`. Поэтому используется JUnit **6.1.0**.

### 3.4 Плагины

**exec-maven-plugin** (строки 39–42) — позволяет запускать `mvn exec:java -Dexec.mainClass=ru.bmstu.App`.

**maven-surefire-plugin** (строки 43–54) — настроен с JVM-аргументами:
```xml
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
```
Нужны для Mockito 5.x на Java 25: без них Mockito не может инструментировать классы из
закрытых модулей JDK.

---

## 4. Изменения существующих файлов

### 4.1 App.java

**Файл:** [`src/main/java/ru/bmstu/App.java`](src/main/java/ru/bmstu/App.java)

Файл **полностью переписан**. Консольный цикл на `Scanner` заменён запуском встроенного Tomcat.

#### Как работает новый App.java (строка за строкой)

```java
Tomcat tomcat = new Tomcat();       // строка 14 — создаём Tomcat
tomcat.setPort(8080);               // строка 15 — порт 8080
tomcat.getConnector();              // строка 16 — инициализируем HTTP-коннектор
```

```java
AnnotationConfigWebApplicationContext context =
        new AnnotationConfigWebApplicationContext();     // строки 18–19
context.setClassLoader(App.class.getClassLoader());    // строка 20 — FIX (см. §11.2)
context.register(AppConfig.class, WebConfig.class);    // строка 21
```

`AnnotationConfigWebApplicationContext` — web-вариант контекста Spring, поддерживает
`@RequestMapping`, `@RestController` и работу с `HttpServletRequest`/`HttpServletResponse`.
Регистрируем оба конфигурационных класса: `AppConfig` (CSV + properties) и `WebConfig` (MVC).

```java
DispatcherServlet dispatcher = new DispatcherServlet(context);  // строка 23
Context ctx = tomcat.addContext("", null);                       // строка 24
Wrapper servlet = Tomcat.addServlet(ctx, "dispatcher", dispatcher); // строка 25
servlet.setLoadOnStartup(1);        // строка 26 — инициализировать сразу при старте
servlet.setAsyncSupported(true);    // строка 27 — поддержка async-запросов
ctx.addServletMappingDecoded("/*", "dispatcher"); // строка 28 — все URL → dispatcher
```

`DispatcherServlet` — центральный сервлет Spring MVC. Он маршрутизирует HTTP-запросы к
соответствующим `@RestController`. Маппинг `/*` означает, что все запросы обрабатываются
через него.

```java
tomcat.start();                                         // строка 30
System.out.println("Server started at http://localhost:8080");   // строка 31
System.out.println("Swagger UI: ...");                           // строка 32
tomcat.getServer().await();                             // строка 33 — блокировка, сервер работает
```

`await()` блокирует главный поток, не давая JVM завершиться. Сервер работает, пока не будет
остановлен вручную.

### 4.2 CreatureCsvReader.java

**Файл:** [`src/main/java/ru/bmstu/reader/CreatureCsvReader.java`](src/main/java/ru/bmstu/reader/CreatureCsvReader.java)

Изменена **одна строка** — строка 19:

```java
// БЫЛО:
ClassPathResource resource = new ClassPathResource(resourcePath);

// СТАЛО:
ClassPathResource resource = new ClassPathResource(resourcePath, getClass().getClassLoader());
```

**Зачем:** При запуске через embedded Tomcat сервер подменяет `Thread.currentThread().getContextClassLoader()`
на собственный `WebappClassLoader`, который не знает о файлах в `target/classes` Maven-проекта.
Конструктор `ClassPathResource(path)` без явного `ClassLoader` использует именно thread context
classloader → файл не находится.

Передача `getClass().getClassLoader()` (загрузчик класса `CreatureCsvReader`, который был
загружен Spring-контекстом с classloader'ом приложения) гарантирует, что `creatures.csv`
будет найден независимо от того, какой поток выполняет чтение.

---

## 5. Новые конфигурационные классы

### 5.1 WebConfig.java

**Файл:** [`src/main/java/ru/bmstu/config/WebConfig.java`](src/main/java/ru/bmstu/config/WebConfig.java)

```java
@Configuration          // строка 14 — Spring-конфигурация
@EnableWebMvc           // строка 15 — включает полный Spring MVC (HandlerMapping, MessageConverters и т.д.)
@ComponentScan("ru.bmstu") // строка 16 — сканируем все пакеты проекта
@EnableAspectJAutoProxy // строка 17 — включает обработку @Aspect аннотаций
public class WebConfig implements WebMvcConfigurer {
```

**`@EnableWebMvc`** — регистрирует в контексте ключевые бины Spring MVC:
`RequestMappingHandlerMapping`, `RequestMappingHandlerAdapter`, `ExceptionHandlerExceptionResolver` и др.
Без этой аннотации контроллеры не будут найдены.

**`@EnableAspectJAutoProxy`** — заставляет Spring создавать прокси-обёртки для бинов,
методы которых перехватываются аспектами (`@Aspect`). Без неё `@Before`, `@Around` и пр.
не работают.

#### Метод `configureMessageConverters` (строки 21–23)

```java
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new MappingJackson2HttpMessageConverter());
}
```

Регистрирует Jackson как конвертер HTTP-сообщений. Именно он превращает Java-объекты
(например, `List<Creature>`) в JSON-тело ответа и парсит JSON из тела запроса.

#### Метод `addResourceHandlers` (строки 26–29)

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
}
```

Подключает статику из webjars (JAR-файлов с фронтендом). URL `/webjars/swagger-ui/...`
автоматически маппится на файлы внутри `swagger-ui-5.32.6.jar`. Это необходимо для
работы Swagger UI.

### 5.2 OpenApiConfig.java

**Файл:** [`src/main/java/ru/bmstu/config/OpenApiConfig.java`](src/main/java/ru/bmstu/config/OpenApiConfig.java)

```java
@Configuration          // строка 8
public class OpenApiConfig {

    @Bean               // строка 11
    public OpenAPI shelterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Magical Creature Shelter API")   // строка 15
                        .version("v1")                           // строка 16
                        .description("REST API for managing magical creature shelter")); // строка 17
    }
}
```

Этот бин подхватывается библиотекой springdoc и используется для формирования
спецификации OpenAPI 3.0 по адресу `/v3/api-docs`. Swagger UI, расположенный по
адресу `http://localhost:8080/swagger-ui/index.html`, читает эту спецификацию и
отображает интерактивную документацию API.

---

## 6. Аннотация RequiresRole

**Файл:** [`src/main/java/ru/bmstu/annotation/RequiresRole.java`](src/main/java/ru/bmstu/annotation/RequiresRole.java)

```java
@Target(ElementType.METHOD)      // строка 8 — аннотация применяется только к методам
@Retention(RetentionPolicy.RUNTIME) // строка 9 — видна в рантайме (нужно для AspectJ)
public @interface RequiresRole {
    String value();               // строка 11 — обязательный параметр: имя требуемой роли
}
```

Используется так:
```java
@GetMapping("/all")
@RequiresRole("ADMIN")    // только администратор может вызвать этот endpoint
public ResponseEntity<List<Creature>> getAll() { ... }
```

`@Retention(RetentionPolicy.RUNTIME)` критически важен — без него аспект не увидит
аннотацию во время выполнения программы.

---

## 7. AOP-аспекты

### 7.1 RoleCheckAspect.java — проверка ролей

**Файл:** [`src/main/java/ru/bmstu/aspect/RoleCheckAspect.java`](src/main/java/ru/bmstu/aspect/RoleCheckAspect.java)

```java
@Aspect     // строка 14 — AspectJ-аспект
@Component  // строка 15 — Spring-компонент, регистрируется в контексте
public class RoleCheckAspect {

    @Around("@annotation(requiresRole)")   // строка 18
    public Object checkRole(ProceedingJoinPoint pjp, RequiresRole requiresRole) throws Throwable {
```

**`@Around`** — «оборачивает» вызов метода: выполняется ДО и ПОСЛЕ него. Если проверка
не пройдена, метод не вызывается вовсе.

**`@annotation(requiresRole)`** — pointcut, который перехватывает **любой метод** в
любом Spring-бине, помеченный `@RequiresRole`. Параметр `requiresRole` связывается с
самой аннотацией, чтобы извлечь значение `value()`.

#### Логика проверки (строки 20–32)

```java
String requiredRole = requiresRole.value();   // строка 20 — читаем требуемую роль из аннотации

ServletRequestAttributes attributes =
    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes(); // строки 21–22
HttpServletRequest request = attributes.getRequest(); // строка 23 — получаем текущий HTTP-запрос

String role = request.getHeader("X-Role");     // строка 24 — читаем заголовок X-Role
if (role == null) {
    role = "USER";                             // строки 25–27 — роль по умолчанию USER
}
if (!role.equalsIgnoreCase(requiredRole)) {    // строка 28 — сравниваем без учёта регистра
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,                  // строка 29 — HTTP 403 Forbidden
        "Access denied: requires role " + requiredRole);
}
return pjp.proceed();                          // строка 32 — пропускаем вызов только при совпадении
```

`RequestContextHolder` — механизм Spring MVC, который сохраняет текущий `HttpServletRequest`
в thread-local переменной. Это позволяет получить запрос из любого места кода, не передавая
его явно как параметр.

**Схема работы:**
```
HTTP GET /api/v1/creatures/all
    X-Role: USER
         │
         ▼
  DispatcherServlet
         │
         ▼
  RoleCheckAspect.checkRole()   ← перехватывает вызов до того, как он дойдёт до контроллера
         │
    role = "USER" ≠ "ADMIN"
         │
         ▼
  ResponseStatusException(403)  ← метод контроллера так и не был вызван
         │
         ▼
  HTTP 403 Forbidden
```

### 7.2 AuditLogAspect.java — журнал вызовов сервисов

**Файл:** [`src/main/java/ru/bmstu/aspect/AuditLogAspect.java`](src/main/java/ru/bmstu/aspect/AuditLogAspect.java)

```java
@Aspect     // строка 12
@Component  // строка 13
public class AuditLogAspect {

    private final AuditLogService auditLogService;     // строка 16

    public AuditLogAspect(AuditLogService auditLogService) { // строки 18–20 — constructor injection
        this.auditLogService = auditLogService;
    }

    @Before("execution(public * ru.bmstu.service.impl.*.*(..)) " +
            "&& !within(ru.bmstu.service.impl.AuditLogServiceImpl)")  // строки 22–23
    public void audit(JoinPoint joinPoint) {
```

**`@Before`** — выполняется ДО вызова метода-цели (в отличие от `@Around` не может
остановить выполнение).

**Pointcut:** `execution(public * ru.bmstu.service.impl.*.*(..))` перехватывает все
публичные методы всех классов в пакете `ru.bmstu.service.impl`.

**`!within(ru.bmstu.service.impl.AuditLogServiceImpl)`** — исключение `AuditLogServiceImpl`
из перехвата. Без этого возникла бы **бесконечная рекурсия**: аспект вызывает
`auditLogService.log()` → перехватывается снова → вызывает `log()` → ...

#### Логика журналирования (строки 25–30)

```java
String timestamp = LocalDateTime.now().toString();          // строка 25 — метка времени
String signature = joinPoint.getSignature().toShortString(); // строка 26 — имя метода
String args = Arrays.toString(joinPoint.getArgs());          // строка 27 — аргументы
String entry = "[AUDIT] " + timestamp + " | " + signature + " | " + args; // строка 28
System.out.println(entry);           // строка 29 — вывод в консоль
auditLogService.log(entry);          // строка 30 — сохранение в памяти
```

Пример вывода в консоль при вызове `GET /api/v1/creatures`:
```
[AUDIT] 2026-05-30T00:01:15.123 | AdoptionService.findAffordable(..) | [1.7976931348623157E308]
[AUDIT] 2026-05-30T00:01:15.124 | CreatureService.findAll() | []
```

---

## 8. Новый сервис аудита

### 8.1 AuditLogService.java (интерфейс)

**Файл:** [`src/main/java/ru/bmstu/service/AuditLogService.java`](src/main/java/ru/bmstu/service/AuditLogService.java)

```java
public interface AuditLogService {
    void log(String entry);       // строка 6 — добавить запись в журнал
    List<String> getLog();        // строка 7 — получить весь журнал
}
```

### 8.2 AuditLogServiceImpl.java (реализация)

**Файл:** [`src/main/java/ru/bmstu/service/impl/AuditLogServiceImpl.java`](src/main/java/ru/bmstu/service/impl/AuditLogServiceImpl.java)

```java
@Service    // строка 9 — регистрируется в Spring-контексте
public class AuditLogServiceImpl implements AuditLogService {

    private final List<String> log = new ArrayList<>();  // строка 12 — хранилище записей в памяти

    @Override
    public void log(String entry) {
        log.add(entry);              // строка 16
    }

    @Override
    public List<String> getLog() {
        return List.copyOf(log);     // строка 21 — возвращаем неизменяемую копию
    }
}
```

`List.copyOf(log)` — защита от изменения внутреннего состояния извне.
Записи хранятся в памяти (in-memory) — при перезапуске сервера журнал очищается.

---

## 9. REST-контроллеры

Все контроллеры находятся в пакете `ru.bmstu.controller`, используют **constructor injection**
для зависимостей и возвращают `ResponseEntity<?>` с явным HTTP-статусом.

### 9.1 StatusController.java

**Файл:** [`src/main/java/ru/bmstu/controller/StatusController.java`](src/main/java/ru/bmstu/controller/StatusController.java)

```java
@RestController                 // строка 10 — @Controller + @ResponseBody
@RequestMapping("/api/v1")      // строка 11 — базовый URL-префикс
public class StatusController {

    @GetMapping("/status")      // строка 14 — обрабатывает GET /api/v1/status
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Magical Creature Shelter"
        ));
    }
}
```

**Пример ответа:**
```json
HTTP 200 OK
{ "status": "UP", "service": "Magical Creature Shelter" }
```

### 9.2 CreatureController.java

**Файл:** [`src/main/java/ru/bmstu/controller/CreatureController.java`](src/main/java/ru/bmstu/controller/CreatureController.java)

| Метод | URL | Роль | Описание |
|---|---|---|---|
| GET | `/api/v1/creatures` | USER/ADMIN | Доступные (не усыновлённые) существа |
| GET | `/api/v1/creatures/all` | **ADMIN** | Все существа включая усыновлённых |
| GET | `/api/v1/creatures/{id}` | USER/ADMIN | Существо по ID |
| GET | `/api/v1/creatures/search?name=X` | USER/ADMIN | Поиск по имени (без учёта регистра) |
| GET | `/api/v1/creatures/filter?species=X` | USER/ADMIN | Фильтр по виду |
| GET | `/api/v1/creatures/filter?temperament=X` | USER/ADMIN | Фильтр по темпераменту |

#### Constructor injection (строки 23–26)

```java
public CreatureController(CreatureService creatureService, AdoptionService adoptionService) {
    this.creatureService = creatureService;
    this.adoptionService = adoptionService;
}
```

#### Доступные существа (строки 28–31)

```java
@GetMapping
public ResponseEntity<List<Creature>> getAvailable() {
    return ResponseEntity.ok(adoptionService.findAffordable(Double.MAX_VALUE));
}
```

`findAffordable(Double.MAX_VALUE)` — возвращает всех существ, которых можно содержать при
бесконечном бюджете, то есть всех **не усыновлённых**. Хитрость: метод уже отфильтровывает
усыновлённых через внутренний `Set<String> adoptedIds` в `AdoptionServiceImpl`.

#### Эндпоинт только для ADMIN (строки 33–37)

```java
@GetMapping("/all")
@RequiresRole("ADMIN")     // строка 34 — аспект проверит X-Role перед выполнением
public ResponseEntity<List<Creature>> getAll() {
    return ResponseEntity.ok(creatureService.findAll());
}
```

#### Поиск по ID с обработкой 404 (строки 39–46)

```java
@GetMapping("/{id}")
public ResponseEntity<Creature> getById(@PathVariable("id") String id) {
    Creature creature = creatureService.findById(id);
    if (creature == null) {
        return ResponseEntity.notFound().build();  // HTTP 404
    }
    return ResponseEntity.ok(creature);            // HTTP 200
}
```

#### Комбинированный фильтр (строки 53–64)

```java
@GetMapping("/filter")
public ResponseEntity<List<Creature>> filter(
        @RequestParam(name = "species", required = false) String species,
        @RequestParam(name = "temperament", required = false) String temperament) {
    if (species != null) {
        return ResponseEntity.ok(creatureService.findBySpecies(species));
    }
    if (temperament != null) {
        return ResponseEntity.ok(creatureService.findByTemperament(temperament));
    }
    return ResponseEntity.ok(creatureService.findAll());
}
```

Один эндпоинт обрабатывает оба вида фильтрации через `required = false`.

### 9.3 AdoptionController.java + AdoptionRequest.java

**Файлы:**
- [`src/main/java/ru/bmstu/controller/AdoptionController.java`](src/main/java/ru/bmstu/controller/AdoptionController.java)
- [`src/main/java/ru/bmstu/controller/AdoptionRequest.java`](src/main/java/ru/bmstu/controller/AdoptionRequest.java)

#### AdoptionRequest — DTO для тела запроса

```java
@Data               // строка 7 — Lombok: геттеры, сеттеры, equals, hashCode, toString
@NoArgsConstructor  // строка 8 — конструктор без аргументов (нужен Jackson для десериализации)
@AllArgsConstructor // строка 9 — конструктор со всеми аргументами (удобно в тестах)
public class AdoptionRequest {
    private String visitorName;   // строка 11
    private double monthlyBudget; // строка 12
    private String creatureId;    // строка 13
}
```

Jackson использует конструктор без аргументов и сеттеры для десериализации JSON:
```json
{ "visitorName": "Alex", "monthlyBudget": 300.0, "creatureId": "1" }
```

#### AdoptionController — логика обработки ответа

```java
@PostMapping                                           // строка 24 — POST /api/v1/adoptions
public ResponseEntity<?> adopt(@RequestBody AdoptionRequest request) {
    String result = adoptionService.adopt(             // строки 26–30
            request.getVisitorName(),
            request.getMonthlyBudget(),
            request.getCreatureId()
    );
    if (result.startsWith("Error: creature not found")) {
        return ResponseEntity.notFound().build();       // строки 31–33 — HTTP 404
    }
    if (result.startsWith("Error") || result.startsWith("Adoption denied")) {
        List<Creature> alternatives = adoptionService.findAffordable(request.getMonthlyBudget());
        return ResponseEntity.badRequest().body(Map.of(// строки 34–39 — HTTP 400
                "message", result,
                "alternatives", alternatives
        ));
    }
    return ResponseEntity.ok(Map.of("contract", result)); // строки 40–42 — HTTP 200
}
```

**Логика маппинга HTTP-кодов:**

| Результат `adoptionService.adopt()` | HTTP-код | Тело ответа |
|---|---|---|
| Начинается с `"Contract generated!"` | 200 OK | `{"contract": "..."}` |
| Начинается с `"Error: creature not found"` | 404 Not Found | пустое |
| Начинается с `"Error"` или `"Adoption denied"` | 400 Bad Request | `{"message":"...", "alternatives":[...]}` |

### 9.4 StatisticsController.java

**Файл:** [`src/main/java/ru/bmstu/controller/StatisticsController.java`](src/main/java/ru/bmstu/controller/StatisticsController.java)

```java
@RestController
@RequestMapping("/api/v1")      // строка 15
public class StatisticsController {

    // строки 17–23: constructor injection ShelterStatisticsService + AuditLogService

    @GetMapping("/statistics")  // строка 26
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(Map.of(
                "total", statsService.getTotalCount(),
                "adopted", statsService.getAdoptedCount(),
                "mostPopularSpecies", statsService.getMostPopularSpecies()
        ));
    }

    @GetMapping("/audit")       // строка 35
    @RequiresRole("ADMIN")      // строка 36 — только ADMIN
    public ResponseEntity<List<String>> getAuditLog() {
        return ResponseEntity.ok(auditLogService.getLog());
    }
}
```

**Пример ответа `/api/v1/statistics`:**
```json
{ "total": 5, "adopted": 1, "mostPopularSpecies": "Phoenix" }
```

**Пример ответа `/api/v1/audit` (ADMIN):**
```json
[
  "[AUDIT] 2026-05-30T00:01:15 | CreatureService.findAll() | []",
  "[AUDIT] 2026-05-30T00:01:16 | AdoptionService.adopt(..) | [Alex, 300.0, 1]"
]
```

---

## 10. Тесты

Итоговое количество тестов: **36**, все проходят (`mvn clean install` → BUILD SUCCESS).

### 10.1 Тесты сервисов (без Spring-контекста)

Сервисы тестируются напрямую через конструктор — без поднятия Spring-контекста.
Это быстро и изолированно.

#### AdoptionServiceImplTest.java

**Файл:** [`src/test/java/ru/bmstu/service/impl/AdoptionServiceImplTest.java`](src/test/java/ru/bmstu/service/impl/AdoptionServiceImplTest.java)

Создаёт `CreatureServiceImpl` с тестовыми данными и `AdoptionServiceImpl` с бюджетом 1000.

| Тест | Строка | Что проверяет |
|---|---|---|
| `adopt_successfulAdoption` | 28 | При достаточном бюджете контракт генерируется |
| `adopt_insufficientDailyBudget` | 36 | При нехватке дневного бюджета — отказ |
| `adopt_creatureNotFound` | 43 | Несуществующий ID → "Error: creature not found" |
| `adopt_alreadyAdopted` | 49 | Повторное усыновление → "already been adopted" |
| `findAffordable_returnsCreaturesWithinBudget` | 56 | Фильтр по бюджету работает корректно |
| `findAffordable_excludesAlreadyAdopted` | 63 | Усыновлённые не попадают в список |
| `getRemainingBudget_decreasesAfterAdoption` | 71 | Бюджет уменьшается на стоимость усыновления |

#### ShelterStatisticsServiceImplTest.java

**Файл:** [`src/test/java/ru/bmstu/service/impl/ShelterStatisticsServiceImplTest.java`](src/test/java/ru/bmstu/service/impl/ShelterStatisticsServiceImplTest.java)

| Тест | Строка | Что проверяет |
|---|---|---|
| `getTotalCount_returnsCorrectCount` | 30 | Общее количество существ |
| `getAdoptedCount_initiallyZero` | 35 | Изначально никто не усыновлён |
| `getAdoptedCount_incrementsAfterAdoption` | 40 | Счётчик растёт после усыновления |
| `getMostPopularSpecies_returnsCorrectSpecies` | 46 | Phoenix (2 штуки) > Shadow Wolf (1) |
| `getMostPopularSpecies_noCreatures_returnsNA` | 52 | Пустой каталог → "N/A" |

### 10.2 Тесты контроллеров (MockMvc standaloneSetup)

Тесты контроллеров используют `MockMvcBuilders.standaloneSetup(controller)` — это настройка
MockMvc только для одного конкретного контроллера, без загрузки Spring-контекста.
Зависимости (сервисы) заменяются Mockito-моками.

```
standaloneSetup                      webAppContextSetup
    │                                    │
    │  - Быстро (~10 мс)                 │  - Медленнее (~700 мс)
    │  - Без Spring-контекста            │  - Полный Spring-контекст
    │  - AOP не работает                 │  - AOP работает
    │  - Сервисы = моки                  │  - Реальные бины
    ▼                                    ▼
StatusControllerTest           CreatureControllerRoleTest
CreatureControllerTest
AdoptionControllerTest
StatisticsControllerTest
```

#### StatusControllerTest.java

**Файл:** [`src/test/java/ru/bmstu/controller/StatusControllerTest.java`](src/test/java/ru/bmstu/controller/StatusControllerTest.java)

```java
@BeforeEach
void setUp() {
    StatusController controller = new StatusController();         // строка 18
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); // строка 19
}

@Test
void status_returns200() throws Exception {
    mockMvc.perform(get("/api/v1/status"))
           .andExpect(status().isOk());    // строка 25 — HTTP 200
}

@Test
void status_bodyContainsUp() throws Exception {
    mockMvc.perform(get("/api/v1/status"))
           .andExpect(jsonPath("$.status").value("UP"))                 // строка 31
           .andExpect(jsonPath("$.service").value("Magical Creature Shelter")); // строка 32
}
```

#### CreatureControllerTest.java

**Файл:** [`src/test/java/ru/bmstu/controller/CreatureControllerTest.java`](src/test/java/ru/bmstu/controller/CreatureControllerTest.java)

Зависимости мокируются через Mockito:
```java
creatureService = Mockito.mock(CreatureService.class);   // строка 29
adoptionService = Mockito.mock(AdoptionService.class);   // строка 30
```

| Тест | Строка | Ожидаемый результат |
|---|---|---|
| `getAll_returns200` | 37 | 200 + первое существо — Ember |
| `getById_returns200` | 47 | 200 + имя Ember, вид Phoenix |
| `getById_unknown_returns404` | 56 | 404 при несуществующем ID |
| `searchByName_returns200` | 63 | 200 + результаты поиска |
| `filterBySpecies_returns200` | 74 | 200 + отфильтрованный список |

#### AdoptionControllerTest.java

**Файл:** [`src/test/java/ru/bmstu/controller/AdoptionControllerTest.java`](src/test/java/ru/bmstu/controller/AdoptionControllerTest.java)

Тело запроса сериализуется через `ObjectMapper`:
```java
AdoptionRequest request = new AdoptionRequest("Alice", 300.0, "1");
mockMvc.perform(post("/api/v1/adoptions")
       .contentType(MediaType.APPLICATION_JSON)
       .content(objectMapper.writeValueAsString(request)))
```

| Тест | Строка | Что проверяет |
|---|---|---|
| `adopt_validAdoption_returns200` | 36 | Успешное усыновление → 200 + поле "contract" |
| `adopt_insufficientBudget_returns400` | 46 | Нехватка бюджета → 400 + поле "message" |
| `adopt_creatureNotFound_returns404` | 57 | Несуществующий ID → 404 |
| `adopt_alreadyAdopted_returns400` | 67 | Уже усыновлён → 400 |

#### StatisticsControllerTest.java

**Файл:** [`src/test/java/ru/bmstu/controller/StatisticsControllerTest.java`](src/test/java/ru/bmstu/controller/StatisticsControllerTest.java)

| Тест | Строка | Что проверяет |
|---|---|---|
| `statistics_returns200` | 35 | 200 + total=5, adopted=2, mostPopularSpecies="Phoenix" |
| `auditLog_returns200` | 44 | 200 + первый элемент журнала |

### 10.3 Интеграционный тест с полным Spring-контекстом

**Файл:** [`src/test/java/ru/bmstu/controller/CreatureControllerRoleTest.java`](src/test/java/ru/bmstu/controller/CreatureControllerRoleTest.java)

Это единственный тест, который поднимает **полный Spring-контекст** — только так
AOP-аспект `RoleCheckAspect` будет активен и станет перехватывать вызовы.

```java
@ExtendWith(SpringExtension.class)                       // строка 19 — JUnit-расширение Spring
@WebAppConfiguration                                     // строка 20 — web-контекст для MockMvc
@ContextConfiguration(classes = {AppConfig.class, WebConfig.class}) // строка 21
class CreatureControllerRoleTest {

    @Autowired
    private WebApplicationContext wac;                   // строка 25 — авто-инжектится Spring'ом

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build(); // строка 31 — с полным контекстом
    }
```

| Тест | Строка | Что проверяет |
|---|---|---|
| `getAllAdmin_returns403WhenRoleIsUser` | 35 | `X-Role: USER` → 403 на `/all` |
| `getAllAdmin_returns200WhenRoleIsAdmin` | 41 | `X-Role: ADMIN` → 200 на `/all` |
| `audit_returns403WhenRoleIsUser` | 47 | `X-Role: USER` → 403 на `/audit` |
| `getAllAdmin_returns403WhenNoHeaderProvided` | 53 | Без заголовка → 403 (default = USER) |

### 10.4 Конфигурация Mockito для Java 25

**Файл:** [`src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`](src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker)

```
mock-maker-subclass
```

Mockito 5.x по умолчанию использует **inline mock maker** (на основе Java Instrumentation API),
который на Java 25 не работает из-за ужесточённых ограничений доступа к модулям JDK.
Переключение на **subclass mock maker** заставляет Mockito использовать JDK Dynamic Proxies
для интерфейсов и подклассы ByteBuddy для обычных классов — это совместимо с Java 25.

---

## 11. Нетривиальные технические решения

### 11.1 JUnit 6 вместо JUnit 5

`spring-test 7.0.7` в байткоде вызывает метод
`ExtensionContext.Store.computeIfAbsent(Object, Function, Class)`.
Во **всех** публично доступных версиях JUnit 5.x этот метод называется
`getOrComputeIfAbsent` (другое имя). Метод `computeIfAbsent` появился только в **JUnit 6.0.0**.
Попытка использовать любую версию JUnit 5 приводит к `NoSuchMethodError` при запуске
интеграционных тестов.

### 11.2 Classloader при старте через embedded Tomcat

При вызове `tomcat.start()` Tomcat заменяет `Thread.currentThread().getContextClassLoader()`
на собственный `WebappClassLoader`. Этот загрузчик не знает о файлах Maven-проекта
(`target/classes`). Из-за этого:

- `ClassPathResource("creatures.csv")` без явного ClassLoader → берёт thread context classloader
  → `WebappClassLoader` → файл не найден → `RuntimeException`

**Исправление 1** (App.java, строка 20):
```java
context.setClassLoader(App.class.getClassLoader());
```
Устанавливает classloader Spring-контекста до старта Tomcat. Это исправляет `@PropertySource`
в `AppConfig`, который теперь найдёт `application.properties`.

**Исправление 2** (CreatureCsvReader.java, строка 19):
```java
new ClassPathResource(resourcePath, getClass().getClassLoader())
```
`getClass().getClassLoader()` возвращает загрузчик, которым был загружен сам
`CreatureCsvReader` — то есть тот же classloader Spring-контекста. Это исправляет чтение
`creatures.csv` внутри метода `read()`, вызываемого уже во время инициализации Tomcat.

### 11.3 Артефакт springdoc в версии 2.x

В задании указано `springdoc-openapi-webmvc-core 2.8.x`. Однако этот артефакт
существует **только в версии 1.x** (последняя — 1.8.0, для Spring 5). В springdoc 2.x
этот модуль был переименован:

```
springdoc 1.x:  springdoc-openapi-webmvc-core
springdoc 2.x:  springdoc-openapi-starter-webmvc-ui  ← используется
```

Использован корректный актуальный артефакт `springdoc-openapi-starter-webmvc-ui:2.8.6`.

### 11.4 Флаг -parameters компилятора

Spring MVC 7 для разрешения параметров `@PathVariable` и `@RequestParam` полагается на
метаданные имён параметров, записанные в байткоде. Без флага `-parameters`:

```
IllegalArgumentException: Name for argument of type [java.lang.String] not specified,
and parameter name information not available via reflection.
Ensure that the compiler uses the '-parameters' flag.
```

Флаг добавлен в `maven-compiler-plugin` (pom.xml, строка 27). Дополнительно, для
надёжности, во всех аннотациях явно указаны имена параметров:
`@PathVariable("id")`, `@RequestParam("name")`, `@RequestParam(name = "species")`.

---

## 12. API-справочник

Базовый URL: `http://localhost:8080`

### Статус сервиса

```
GET /api/v1/status
Заголовки: —
Ответ 200: { "status": "UP", "service": "Magical Creature Shelter" }
```

### Существа

```
GET /api/v1/creatures
Заголовки: X-Role: USER (или ADMIN, или отсутствует)
Ответ 200: [ { "id":"1", "name":"Ember", "species":"Phoenix", ... }, ... ]
Возвращает только не усыновлённых существ.

GET /api/v1/creatures/all
Заголовки: X-Role: ADMIN  ← обязательно
Ответ 200: список всех существ, включая усыновлённых
Ответ 403: { "status":403, "error":"Forbidden" }  если роль ≠ ADMIN

GET /api/v1/creatures/{id}
Ответ 200: { "id":"1", "name":"Ember", ... }
Ответ 404: если существо не найдено

GET /api/v1/creatures/search?name=ember
Ответ 200: список (поиск регистронезависимый, по подстроке)

GET /api/v1/creatures/filter?species=Phoenix
GET /api/v1/creatures/filter?temperament=Calm
Ответ 200: отфильтрованный список
```

### Усыновление

```
POST /api/v1/adoptions
Content-Type: application/json
Тело: { "visitorName": "Alex", "monthlyBudget": 300.0, "creatureId": "1" }

Ответ 200: { "contract": "Contract generated! Alex has bonded with Ember..." }
Ответ 400: { "message": "Adoption denied...", "alternatives": [...] }
Ответ 404: если существо с указанным ID не найдено
```

### Статистика и аудит

```
GET /api/v1/statistics
Ответ 200: { "total": 5, "adopted": 1, "mostPopularSpecies": "Shadow Wolf" }

GET /api/v1/audit
Заголовки: X-Role: ADMIN  ← обязательно
Ответ 200: ["[AUDIT] 2026-05-30T00:01:15 | AdoptionService.adopt(..) | [...]", ...]
Ответ 403: если роль ≠ ADMIN
```

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html  — интерактивная документация
http://localhost:8080/v3/api-docs            — спецификация OpenAPI 3.0 (JSON)
```

---

## Сборка и запуск

```bash
# Сборка + все тесты
mvn clean install

# Запуск сервера
mvn exec:java -Dexec.mainClass="ru.bmstu.App"

# Только тесты
mvn test
```

Результат тестов: **36 тестов, 0 ошибок, 0 пропущено**.
