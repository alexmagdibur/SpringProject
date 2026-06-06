# JSON-ответы при всех ошибках REST API

## Проблема

По умолчанию при ошибках (403, 404, 405, 500 и т.д.) Tomcat возвращал собственную
HTML-страницу вида:

```html
<!doctype html><html lang="en"><head><title>HTTP Status 403 – Forbidden</title>...
<b>Message</b> Access denied: requires role ADMIN, your role: USER...
```

Это неприемлемо для REST API — клиент ожидает JSON, а не HTML.

---

## Решение: три уровня защиты

```
HTTP-запрос
    │
    ▼
┌─────────────────────────────────────────┐
│         Spring DispatcherServlet        │
│                                         │
│  ┌──────────────────────────────────┐   │
│  │     GlobalExceptionHandler       │   │  ← Уровень 1: Spring
│  │  (перехватывает все исключения)  │   │    (все обычные ошибки)
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
    │ если исключение вырвалось наружу
    ▼
┌─────────────────────────────────────────┐
│              Apache Tomcat              │
│                                         │
│  error page → форвард на /error        │  ← Уровень 2: Tomcat
│                                         │    (страховка)
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│           ErrorController               │  ← Уровень 3: /error
│     (обрабатывает форвард Tomcat)       │    возвращает JSON
└─────────────────────────────────────────┘
```

---

## Уровень 1 — `GlobalExceptionHandler.java`

**Файл:** `src/main/java/ru/bmstu/controller/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<...> handleStatus(ResponseStatusException ex) { ... }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<...> handleNotFound(NoHandlerFoundException ex) { ... }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<...> handleMethodNotAllowed(...) { ... }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<...> handleBadRequest(...) { ... }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<...> handleMissingParam(...) { ... }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<...> handleGeneric(Exception ex) { ... }
}
```

### Как работает `@RestControllerAdvice`

`@RestControllerAdvice` — это `@ControllerAdvice` + `@ResponseBody`. Spring регистрирует
этот класс как глобальный обработчик исключений для всех `@RestController`-ов.

Когда любой контроллер или аспект бросает исключение, Spring не даёт ему дойти
до Tomcat — перехватывает в `@ExceptionHandler` и возвращает `ResponseEntity` с JSON.

### Какие исключения обрабатываются

| Исключение | HTTP-код | Когда возникает |
|---|---|---|
| `ResponseStatusException` | 401 / 403 | `RoleCheckAspect` (неверный ключ или роль) |
| `NoHandlerFoundException` | 404 | URL не совпал ни с одним контроллером |
| `HttpRequestMethodNotSupportedException` | 405 | POST вместо GET и т.п. |
| `HttpMessageNotReadableException` | 400 | Невалидный JSON в теле запроса |
| `MissingServletRequestParameterException` | 400 | Обязательный параметр отсутствует |
| `Exception` (catch-all) | 500 | Любое необработанное исключение |

### Почему `NoHandlerFoundException` работает без дополнительной настройки

В Spring Framework 7 `NoHandlerFoundException` бросается по умолчанию, когда
DispatcherServlet не находит обработчика для URL. В более ранних версиях (до 6.x)
требовалось явно включать это через `setThrowExceptionIfNoHandlerFound(true)` —
в Spring 7 этот метод и вовсе удалён, так как поведение стало дефолтным.

---

## Уровень 2 — Tomcat error pages (`App.java`)

**Файл:** `src/main/java/ru/bmstu/App.java`

```java
private static void registerTomcatErrorPages(Context ctx) {
    for (int code : new int[]{400, 401, 403, 404, 405, 500}) {
        ErrorPage page = new ErrorPage();
        page.setErrorCode(code);
        page.setLocation("/error");
        ctx.addErrorPage(page);
    }
    ErrorPage exceptionPage = new ErrorPage();
    exceptionPage.setExceptionType(Throwable.class.getName());
    exceptionPage.setLocation("/error");
    ctx.addErrorPage(exceptionPage);
}
```

### Зачем нужен этот уровень

`GlobalExceptionHandler` перехватывает только те исключения, которые возникают
**внутри** Spring MVC (в контроллерах, сервисах, аспектах). Но есть случаи,
когда ошибка происходит **до** того, как запрос дошёл до Spring:

- Исключение в `Filter` (до DispatcherServlet)
- Ошибка при инициализации контекста
- Ошибка записи в ответ после того, как заголовки уже отправлены

В этих случаях Tomcat перехватывает исключение сам и, не найдя JSON-обработчика,
рисует HTML. Зарегистрированные `ErrorPage` говорят Tomcat: вместо стандартной
HTML-страницы сделать внутренний forward на `/error`.

### Как Tomcat форвардит на `/error`

Tomcat выполняет server-side forward (не redirect). Он добавляет в `HttpServletRequest`
специальные атрибуты:

| Атрибут | Содержимое |
|---|---|
| `jakarta.servlet.error.status_code` | HTTP-код ошибки (Integer) |
| `jakarta.servlet.error.message` | Сообщение об ошибке |
| `jakarta.servlet.error.exception` | Исходное исключение |
| `jakarta.servlet.error.request_uri` | URL, вызвавший ошибку |

Затем форвард попадает в `DispatcherServlet` (который маппится на `/*`) и
обрабатывается контроллером `/error`.

---

## Уровень 3 — `ErrorController.java`

**Файл:** `src/main/java/ru/bmstu/controller/ErrorController.java`

```java
@RestController
@RequestMapping("/error")
public class ErrorController {

    @RequestMapping  // любой HTTP-метод
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusAttr = request.getAttribute("jakarta.servlet.error.status_code");
        int status = statusAttr instanceof Integer s ? s : 500;
        return ResponseEntity.status(status)
                .body(Map.of("status", status, "error", "Unexpected server error"));
    }
}
```

### Почему `@RequestMapping` без метода

Tomcat форвардит на `/error` независимо от исходного HTTP-метода запроса
(GET, POST, DELETE...). Поэтому `@RequestMapping` без указания метода позволяет
обрабатывать все варианты.

---

## Итоговая таблица ответов

| Ситуация | Уровень | HTTP | Тело ответа |
|---|---|---|---|
| Нет API-ключа на ADMIN-эндпоинте | Spring | 403 | `{"status":403,"error":"Access denied..."}` |
| Невалидный API-ключ | Spring | 401 | `{"status":401,"error":"Invalid API key"}` |
| Несуществующий URL | Spring | 404 | `{"status":404,"error":"No endpoint: GET /foo"}` |
| Неверный HTTP-метод | Spring | 405 | `{"status":405,"error":"Method not allowed: PUT"}` |
| Невалидный JSON в теле | Spring | 400 | `{"status":400,"error":"Malformed JSON request body"}` |
| Пропущен параметр | Spring | 400 | `{"status":400,"error":"Missing required parameter: name"}` |
| Необработанное исключение | Spring | 500 | `{"status":500,"error":"Internal server error"}` |
| Ошибка до Spring (фильтры и т.п.) | Tomcat → `/error` | N | `{"status":N,"error":"Unexpected server error"}` |
