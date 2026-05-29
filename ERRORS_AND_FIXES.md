# Отчёт об ошибках и их исправлениях

Данный документ описывает все проблемы, которые возникли после написания основного кода
проекта Spring MVC REST API, и способы их решения.

---

## Содержание

1. [Неверное имя артефакта springdoc](#1-неверное-имя-артефакта-springdoc)
2. [Отсутствует зависимость Hamcrest](#2-отсутствует-зависимость-hamcrest)
3. [Mockito не может создать моки на Java 25](#3-mockito-не-может-создать-моки-на-java-25)
4. [Отсутствует зависимость json-path](#4-отсутствует-зависимость-json-path)
5. [Несовместимость JUnit 5 со Spring Test 7](#5-несовместимость-junit-5-со-spring-test-7)
6. [Отсутствует флаг компилятора -parameters](#6-отсутствует-флаг-компилятора--parameters)
7. [ClassLoader при запуске через Embedded Tomcat](#7-classloader-при-запуске-через-embedded-tomcat)
8. [Порт 8080 занят Windows-службой National Instruments](#8-порт-8080-занят-windows-службой-national-instruments)
9. [Предупреждения SLF4J: No providers found](#9-предупреждения-slf4j-no-providers-found)
10. [INFO-логи Tomcat не подавляются через logback.xml](#10-info-логи-tomcat-не-подавляются-через-logbackxml)
11. [Swagger UI недоступен — 404](#11-swagger-ui-недоступен--404)
12. [Ошибка в Swagger: $ref must be a string](#12-ошибка-в-swagger-ref-must-be-a-string)
13. [В Swagger лишние HTTP-методы у каждого эндпоинта](#13-в-swagger-лишние-http-методы-у-каждого-эндпоинта)
14. [404 при открытии корневого URL](#14-404-при-открытии-корневого-url)

---

## 1. Неверное имя артефакта springdoc

### Симптом

```
[ERROR] org.springdoc:springdoc-openapi-webmvc-core:jar:2.8.6 was not found
in https://repo.maven.apache.org/maven2
```

### Причина

В задании указан артефакт `springdoc-openapi-webmvc-core 2.8.x`, однако этот артефакт
существует **только в версии 1.x** (последняя — 1.8.0). Начиная с версии 2.x библиотека
springdoc переименовала все модули:

```
springdoc 1.x:  springdoc-openapi-webmvc-core        ← для Spring 5, не существует в 2.x
springdoc 2.x:  springdoc-openapi-starter-webmvc-ui  ← правильное имя
```

### Исправление

В `pom.xml` заменили артефакт на корректный:

```xml
<!-- БЫЛО (не существует в 2.x): -->
<artifactId>springdoc-openapi-webmvc-core</artifactId>
<version>2.8.6</version>

<!-- СТАЛО: -->
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<version>2.8.6</version>
```

---

## 2. Отсутствует зависимость Hamcrest

### Симптом

```
[ERROR] cannot access org.hamcrest.Matcher
  class file for org.hamcrest.Matcher not found
```

### Причина

Метод `MockMvcResultMatchers.jsonPath()` использует Hamcrest для проверок (`value()`,
`is()` и др.). В Spring 7 Hamcrest не входит в `spring-test` как транзитивная зависимость
— нужно добавлять явно.

### Исправление

Добавлена зависимость в `pom.xml`:

```xml
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest</artifactId>
    <version>2.2</version>
    <scope>test</scope>
</dependency>
```

---

## 3. Mockito не может создать моки на Java 25

### Симптом

```
Mockito cannot mock this class: interface ru.bmstu.service.CreatureService.

You are seeing this disclaimer because Mockito is configured to create inlined mocks.
Underlying exception: Could not modify all classes [interface ...]
```

### Причина

Mockito 5.x по умолчанию использует **inline mock maker** — он модифицирует байткод
классов напрямую через Java Instrumentation API. На Java 25 модульная система JDK
запрещает такое инструментирование без явных флагов доступа.

### Исправление

**Шаг 1** — переключить Mockito на **subclass mock maker**, который использует JDK Dynamic
Proxies для интерфейсов (не требует инструментирования).

Создан файл `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`:
```
mock-maker-subclass
```

**Шаг 2** — добавить `--add-opens` в `maven-surefire-plugin` в `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <argLine>
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

---

## 4. Отсутствует зависимость json-path

### Симптом

```
[ERROR] StatusControllerTest.status_bodyContainsUp — NoClassDefFound com/jayway/jsonpath/TypeRef
```

### Причина

Метод `MockMvcResultMatchers.jsonPath("$.field")` в тестах требует библиотеку
`json-path` для парсинга JSON-ответов. Она не входит в `spring-test` транзитивно.

### Исправление

```xml
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.9.0</version>
    <scope>test</scope>
</dependency>
```

---

## 5. Несовместимость JUnit 5 со Spring Test 7

### Симптом

```
java.lang.NoSuchMethodError: 'java.lang.Object
  org.junit.jupiter.api.extension.ExtensionContext$Store.computeIfAbsent(
    java.lang.Object, java.util.function.Function, java.lang.Class)'
```

### Причина

`SpringExtension` из `spring-test 7.0.7` вызывает метод
`ExtensionContext.Store.computeIfAbsent(Object, Function, Class)`. Этот метод был добавлен
в JUnit только начиная с **JUnit 6.0.0**. Во **всех** версиях JUnit 5.x этот метод
называется `getOrComputeIfAbsent` — другое имя, бинарная несовместимость.

Проверка через `javap` подтвердила: JUnit 5.0.3, 5.4.2, 5.8.2, 5.9.3, 5.10.2, 5.11.4 —
ни одна не содержит `computeIfAbsent`.

```
# JUnit 5.x (любая версия):
public abstract <K, V> V getOrComputeIfAbsent(K, Function<K,V>, Class<V>);  ← другое имя

# JUnit 6.0.0+:
public abstract <K, V> V computeIfAbsent(K, Function<K,V>, Class<V>);  ← то, что нужно Spring Test 7
```

### Исправление

Обновлена версия JUnit с `5.10.2` до `6.1.0`:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.1.0</version>  <!-- было 5.10.2 -->
    <scope>test</scope>
</dependency>
```

---

## 6. Отсутствует флаг компилятора -parameters

### Симптом

```
jakarta.servlet.ServletException: Request processing failed:
java.lang.IllegalArgumentException: Name for argument of type [java.lang.String]
not specified, and parameter name information not available via reflection.
Ensure that the compiler uses the '-parameters' flag.
```

### Причина

Spring MVC для разрешения `@PathVariable String id` и `@RequestParam String name`
использует рефлексию для получения имён параметров метода. По умолчанию Java-компилятор
**не сохраняет** имена параметров в байткоде. Без флага `-parameters` Spring не может
определить, что переменная `id` соответствует path-сегменту `{id}`.

### Исправление

Добавлен флаг в `maven-compiler-plugin` в `pom.xml`:

```xml
<configuration>
    <compilerArgs>
        <arg>-parameters</arg>
    </compilerArgs>
    ...
</configuration>
```

Также для надёжности во всех аннотациях явно указаны имена:

```java
// БЫЛО:
@PathVariable String id
@RequestParam String name

// СТАЛО:
@PathVariable("id") String id
@RequestParam("name") String name
```

---

## 7. ClassLoader при запуске через Embedded Tomcat

### Симптом (первая часть)

```
Failed to start component [StandardContext[]]:
class path resource [application.properties] cannot be opened because it does not exist
```

После исправления первой части:

```
Failed to instantiate [java.util.List]: Factory method 'creatures' threw exception:
Failed to read creature CSV: creatures.csv:
class path resource [creatures.csv] cannot be opened because it does not exist
```

### Причина

При вызове `tomcat.start()` Tomcat подменяет `Thread.currentThread().getContextClassLoader()`
на собственный `WebappClassLoader`. Этот загрузчик создаётся для изолированного
веб-приложения и **не знает** о файлах Maven-проекта в `target/classes`.

- `@PropertySource("classpath:application.properties")` использует classloader Spring-контекста
- `new ClassPathResource("creatures.csv")` без явного classloader берёт thread context
  classloader → `WebappClassLoader` → файл не найден

При этом `application.properties` и `creatures.csv` физически находятся в `target/classes`
и доступны через classloader приложения.

### Исправление

**В `App.java`** — установить classloader Spring-контекста до старта Tomcat:

```java
AnnotationConfigWebApplicationContext context =
        new AnnotationConfigWebApplicationContext();
context.setClassLoader(App.class.getClassLoader());  // ← добавлено
context.register(AppConfig.class, WebConfig.class);
```

**В `CreatureCsvReader.java`** — передать явный classloader в `ClassPathResource`:

```java
// БЫЛО:
ClassPathResource resource = new ClassPathResource(resourcePath);

// СТАЛО:
ClassPathResource resource = new ClassPathResource(resourcePath, getClass().getClassLoader());
```

`getClass().getClassLoader()` возвращает загрузчик, которым был загружен сам класс
`CreatureCsvReader` — то есть classloader Spring-контекста, заданный в App.java.

---

## 8. Порт 8080 занят Windows-службой National Instruments

### Симптом

```
SEVERE: Failed to initialize component [Connector[*http-nio-8080*]]
org.apache.catalina.LifecycleException: Protocol handler initialization failed
Caused by: java.net.BindException: Address already in use: bind
```

Сервер выводил "Server started at http://localhost:8080", но реально не слушал запросы.

### Причина

При диагностике обнаружено:

```
netstat -ano | findstr ":8080"
TCP  0.0.0.0:8080  LISTENING  8748

Get-WmiObject Win32_Service | Where-Object { $_.ProcessId -eq 8748 }
Name: NIApplicationWebServer
PathName: "C:\Program Files (x86)\National Instruments\Shared\NI WebServer\ApplicationWebServer.exe"
```

Порт 8080 постоянно занят **Windows-службой NI Application Web Server** (ПО от National
Instruments / LabVIEW). Служба запускается автоматически вместе с Windows и не зависит от
IntelliJ IDEA. Завершить её без прав администратора невозможно.

### Исправление в два этапа

**Этап 1** — добавлена проверка доступности порта в `App.java` с понятным сообщением об
ошибке вместо невнятного `SEVERE` от Tomcat:

```java
private static void checkPortAvailable(int port) {
    try (ServerSocket ignored = new ServerSocket(port)) {
        // порт свободен
    } catch (Exception e) {
        System.err.println("ERROR: Port " + port + " is already in use.");
        System.exit(1);
    }
}
```

**Этап 2** — изменён порт с `8080` на `8090`:

```java
private static final int PORT = 8090;
```

---

## 9. Предупреждения SLF4J: No providers found

### Симптом

```
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
```

### Причина

Spring, Tomcat и другие библиотеки используют SLF4J как фасад логирования. В проекте
была только обёртка SLF4J API, но не было конкретной реализации (Logback, Log4j и т.д.),
которая бы принимала и выводила сообщения.

### Исправление

Добавлена реализация Logback в `pom.xml`:

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.18</version>
</dependency>
```

Создан файл `src/main/resources/logback.xml` с подавлением INFO-сообщений от
Tomcat/Spring и видимостью INFO от нашего кода (`ru.bmstu`).

---

## 10. INFO-логи Tomcat не подавляются через logback.xml

### Симптом

Несмотря на настройку `logback.xml` с `<logger name="org.apache" level="WARN"/>`,
консоль продолжала заполняться сообщениями:

```
INFO: Initializing ProtocolHandler ["http-nio-8090"]
INFO: Starting service [Tomcat]
INFO: Starting Servlet engine: [Apache Tomcat/11.0.2]
INFO: Initializing Spring DispatcherServlet 'dispatcher'
```

### Причина

Tomcat пишет логи через **Java Util Logging (JUL)** — стандартную систему логирования
Java. `logback.xml` управляет только **SLF4J**-логгерами. Это две разные, независимые
системы логирования. Настройки Logback на JUL не влияют.

### Исправление

Добавлен мост JUL → SLF4J: теперь все JUL-сообщения перенаправляются в Logback, где они
подавляются согласно `logback.xml`.

**Зависимость в `pom.xml`:**

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jul-to-slf4j</artifactId>
    <version>2.0.17</version>
</dependency>
```

**Активация в `App.java`** до старта Tomcat:

```java
private static void installJulBridge() {
    LogManager.getLogManager().reset();  // сбросить стандартные JUL-обработчики
    SLF4JBridgeHandler.install();        // все JUL-сообщения → SLF4J → Logback
}
```

После исправления консоль показывает только две строки:

```
Server started at http://localhost:8090
Swagger UI: http://localhost:8090/swagger-ui/index.html
```

---

## 11. Swagger UI недоступен — 404

### Симптом

```
[WARN] o.s.web.servlet.PageNotFound - No mapping for GET /swagger-ui/index.html
[WARN] o.s.web.servlet.PageNotFound - No endpoint GET /swagger-ui/index.html
```

### Причина

`springdoc-openapi-starter-webmvc-ui` рассчитан на работу со **Spring Boot**: его
конфигурационные классы загружаются через механизм Boot-автоконфигурации
(`AutoConfiguration.imports`). Без Spring Boot этот механизм не запускается, поэтому:

- Контроллер `OpenApiWebMvcResource` (отвечает за `/v3/api-docs`) не регистрируется
- Обработчик `/swagger-ui/**` не регистрируется
- Перенаправление инициализатора swagger-ui на наш API не происходит

Файлы swagger-ui физически присутствуют в JAR (`swagger-ui-5.32.6.jar`), но путь к ним
не настроен.

### Исправление

Три изменения:

**1. Добавить resource handler в `WebConfig.java`:**

```java
registry.addResourceHandler("/swagger-ui/**")
        .addResourceLocations(
                "classpath:/swagger-ui/",          // наш swagger-initializer.js
                "classpath:/META-INF/resources/webjars/swagger-ui/5.32.6/");
```

**2. Создать `src/main/resources/swagger-ui/swagger-initializer.js`** — переопределяет
файл из webjar и направляет swagger-ui на наш API (а не на petstore по умолчанию):

```javascript
window.onload = function () {
    window.ui = SwaggerUIBundle({
        url: "/v3/api-docs",
        dom_id: "#swagger-ui",
        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
        layout: "StandaloneLayout"
    });
};
```

**3. Создать `ApiDocsController.java`** — самописный контроллер, обслуживающий
`/v3/api-docs` с вручную описанными путями API (поскольку springdoc не сканирует
контроллеры без Boot-автоконфигурации).

---

## 12. Ошибка в Swagger: $ref must be a string

### Симптом

В Swagger UI отображалась красная плашка:

```
Errors
Resolver error at paths./api/v1/status.get.responses.200.$ref
$ref: must be a string (JSON-Ref)
```

### Причина

Классы OpenAPI-модели (`ApiResponse`, `Schema` и др.) содержат поле `$ref`, унаследованное
от базового класса. Если это поле не установлено (равно `null`), Jackson при стандартных
настройках сериализует его в JSON как:

```json
{
  "200": {
    "description": "Status response",
    "$ref": null
  }
}
```

Swagger UI при обработке спецификации встречает `"$ref": null` и выдаёт ошибку, потому что
по спецификации JSON-Ref поле `$ref` **должно быть строкой** (`"$ref": "#/components/..."`)
или отсутствовать.

### Исправление

Настроен `ObjectMapper` в `WebConfig.java` так, чтобы `null`-поля не включались в JSON:

```java
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL); // ← добавлено
    converters.add(new MappingJackson2HttpMessageConverter(mapper));
}
```

Теперь поле `"$ref": null` просто не попадает в JSON-ответ.

---

## 13. В Swagger лишние HTTP-методы у каждого эндпоинта

### Симптом

В Swagger UI появилась группа **"default"** с PUT, POST, DELETE, OPTIONS, HEAD, PATCH
для каждого пути — хотя в контроллерах эти методы не определены.

### Причина

`ApiDocsController.getApiDocs()` **мутировал** singleton-бин `OpenAPI`:

```java
@GetMapping("/v3/api-docs")
public ResponseEntity<OpenAPI> getApiDocs() {
    openAPI.setPaths(buildPaths());  // ← меняем общий объект
    return ResponseEntity.ok(openAPI);
}
```

Библиотека springdoc, частично активная без Boot, успевала добавлять в этот же
singleton-объект автоматически сгенерированные `PathItem` со всеми HTTP-методами.
В результате в ответе оказывались и наши пути, и пути от springdoc одновременно.

### Исправление

Метод переписан так, чтобы при каждом запросе создавался **новый** объект `OpenAPI`,
а не менялся общий бин:

```java
@GetMapping("/v3/api-docs")
public ResponseEntity<OpenAPI> getApiDocs() {
    OpenAPI spec = new OpenAPI()        // ← новый объект, не мутируем singleton
            .info(baseOpenAPI.getInfo())
            .paths(buildPaths());
    return ResponseEntity.ok(spec);
}
```

---

## 14. 404 при открытии корневого URL

### Симптом

При переходе на `http://localhost:8090` браузер показывал:

```
HTTP Status 404 – Not Found
Message: No endpoint GET /
Apache Tomcat/11.0.2
```

### Причина

В проекте нет контроллера, обрабатывающего путь `/`. Это корректное поведение с точки
зрения REST API, но неудобно для пользователя.

### Исправление

В `StatusController.java` добавлен редирект с `/` на Swagger UI:

```java
@GetMapping("/")
public RedirectView root() {
    return new RedirectView("/swagger-ui/index.html");
}
```

Теперь при открытии `http://localhost:8090` браузер автоматически переходит на
`http://localhost:8090/swagger-ui/index.html`.

---

## Итоговая сводка

| № | Ошибка | Уровень | Файл исправления |
|---|--------|---------|-----------------|
| 1 | Неверное имя артефакта springdoc | Build | `pom.xml` |
| 2 | Отсутствует Hamcrest | Build (тесты) | `pom.xml` |
| 3 | Mockito падает на Java 25 | Runtime (тесты) | `pom.xml`, `mockito-extensions/` |
| 4 | Отсутствует json-path | Build (тесты) | `pom.xml` |
| 5 | JUnit 5 несовместим со Spring Test 7 | Runtime (тесты) | `pom.xml` |
| 6 | Отсутствует флаг `-parameters` | Runtime | `pom.xml`, контроллеры |
| 7 | ClassLoader Embedded Tomcat | Runtime | `App.java`, `CreatureCsvReader.java` |
| 8 | Порт 8080 занят NI-службой | Runtime | `App.java` (смена порта) |
| 9 | SLF4J без провайдера | Runtime | `pom.xml`, `logback.xml` |
| 10 | JUL-логи Tomcat не подавляются | Runtime | `pom.xml`, `App.java` |
| 11 | Swagger UI недоступен | Runtime | `WebConfig.java`, `ApiDocsController.java`, `swagger-initializer.js` |
| 12 | `$ref: null` в OpenAPI JSON | Runtime | `WebConfig.java` |
| 13 | Лишние HTTP-методы в Swagger | Runtime | `ApiDocsController.java` |
| 14 | 404 на корневом URL | Runtime | `StatusController.java` |
