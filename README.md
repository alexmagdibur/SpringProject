# Magical Creature Shelter

Консольное приложение для управления магическим приютом фантастических существ,
разработанное в рамках лабораторной работы №5 по курсу «Технология программирования».

---

## Содержательная часть

### Описание

Система позволяет управлять каталогом фантастических существ: просматривать доступных
питомцев, фильтровать их по различным критериям, оформлять магическую привязку
(усыновление) и просматривать статистику приюта.

### Функциональность

**Просмотр и поиск существ**
- Отображение полного каталога существ с подробной информацией
- Фильтрация по виду (species) и темпераменту (temperament)
- Поиск по имени (регистронезависимый)

**Процесс усыновления**
- Посетитель вводит своё имя и ежемесячный бюджет
- Система проверяет, покрывает ли бюджет ежедневные расходы на содержание существа
- При успехе генерируется контракт, существо удаляется из доступных
- При отказе предлагаются альтернативные существа, подходящие под бюджет

**Статистика приюта**
- Общее количество существ в каталоге
- Количество уже усыновлённых существ
- Самый популярный вид существ

### Пример работы

```
=== Magical Creature Shelter ===

1. List all creatures
2. Filter by species
3. Filter by temperament
4. Search by name
5. Adopt a creature
6. Shelter statistics
0. Exit
Choose: 5

Your name: Alex
Monthly budget (gold): 300
Creature ID: 2

Contract generated! Alex has bonded with Gloom (ID: 2). Adoption cost: 200.0 gold.
```

### Каталог существ (пример)

| ID | Имя       | Вид         | Темперамент | Расходы/день | Стоимость | Способности            |
|----|-----------|-------------|-------------|--------------|-----------|------------------------|
| 1  | Ember     | Phoenix     | Calm        | 5.0          | 100.0     | Healing, Firebreathing |
| 2  | Gloom     | Shadow Wolf | Fierce      | 8.0          | 200.0     | Shadowmeld, Tracking   |
| 3  | Pip       | Pixie Fox   | Playful     | 2.0          | 50.0      | Illusion, Speed        |
| 4  | Stonewall | Rock Golem  | Passive     | 3.0          | 80.0      | Shield, Tremor         |
| 5  | Aqua      | Sea Serpent | Calm        | 10.0         | 300.0     | Waterbreathing, Healing|

---

## Техническая часть

### Стек технологий

| Технология     | Версия  | Назначение                        |
|----------------|---------|-----------------------------------|
| Java           | 25      | Язык разработки                   |
| Spring Context | 7.0.7   | IoC-контейнер, DI                 |
| Spring Core    | 7.0.7   | Базовые утилиты Spring            |
| Lombok         | 1.18.38 | Генерация геттеров, сеттеров      |
| JUnit Jupiter  | 5.10.2  | Юнит-тестирование                 |
| Maven          | 3.x     | Сборка и управление зависимостями |

### Архитектура проекта

Проект построен по принципу **layered architecture** с разделением на слои:

```
ru.bmstu
├── App.java                          # Точка входа, main()
├── config/
│   └── AppConfig.java                # @Configuration, описание бинов
├── domain/
│   └── Creature.java                 # Доменный объект
├── reader/
│   └── CreatureCsvReader.java        # Чтение CSV через ClassPathResource
├── service/
│   ├── CreatureService.java          # Интерфейс
│   ├── AdoptionService.java          # Интерфейс
│   └── ShelterStatisticsService.java # Интерфейс
└── service/impl/
    ├── CreatureServiceImpl.java
    ├── AdoptionServiceImpl.java
    └── ShelterStatisticsServiceImpl.java
```

Тесты располагаются в:
```
src/test/java/ru/bmstu/service/impl/
└── CreatureServiceImplTest.java
```

Ресурсы:
```
src/main/resources/
├── creatures.csv            # Каталог существ
└── application.properties   # Конфигурационные параметры
```

### Spring IoC и конфигурация

Конфигурация описана через **Java + Annotation-based** подход (без XML, без Spring Boot).

`AppConfig.java` — единственный `@Configuration`-класс:
```java
@Configuration
@ComponentScan("ru.bmstu")
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Value("${shelter.csv.path}")
    private String csvPath;

    @Bean
    public List<Creature> creatures(CreatureCsvReader reader) {
        return reader.read(csvPath);
    }
}
```

Все сервисы помечены `@Service` и регистрируются в контейнере автоматически через `@ComponentScan`.
Зависимости между сервисами внедряются через **конструктор** (constructor injection).

Контекст поднимается в `App.java`:
```java
var context = new AnnotationConfigApplicationContext(AppConfig.class);
```

### Доменная модель

```java
@Data
@AllArgsConstructor
public class Creature {
    private final String id;
    private String name;
    private String species;
    private String temperament;
    private double dailyCost;
    private double adoptionCost;
    private List<String> abilities;
}
```

### Чтение CSV как ресурса

CSV-файл читается через `ClassPathResource` (требование задания — не через `new File(...)`):

```java
ClassPathResource resource = new ClassPathResource(resourcePath);
BufferedReader br = new BufferedReader(
    new InputStreamReader(resource.getInputStream())
);
```

### Тестирование

Юнит-тесты покрывают `CreatureServiceImpl` без поднятия Spring-контекста —
сервис создаётся напрямую через конструктор с тестовыми данными:

| Тест                                  | Что проверяет                              |
|---------------------------------------|--------------------------------------------|
| `findAll_returnsAllCreatures`         | Возвращаются все существа из каталога      |
| `findBySpecies_returnsCorrectSubset`  | Фильтрация по виду работает корректно      |
| `findBySpecies_returnsEmptyIfNotFound`| Пустой список при отсутствии вида          |
| `findByName_isCaseInsensitive`        | Поиск по имени регистронезависимый         |
| `findById_returnsCorrectCreature`     | Поиск по ID возвращает верное существо     |
| `findById_returnsNullIfNotFound`      | null при отсутствии ID                     |
| `findByTemperament_returnsCorrectSubset` | Фильтрация по темпераменту корректна    |

### Правила оформления кода

- Имена классов — `CamelCase`, переменных и методов — `lowerCamelCase`, констант — `SCREAMING_SNAKE_CASE`
- Все поля классов `private`, доступ только через геттеры/сеттеры (генерируются Lombok)
- Неизменяемые поля помечены `final`
- Все сервисы описаны интерфейсами; реализации в подпакете `impl`
- `Scanner` и `PrintStream` в IoC-контейнер не кладутся
- Весь ввод-вывод на английском языке
- Форматирование кода: `Alt + Ctrl + L` в IntelliJ IDEA

### Сборка и запуск

```bash
# Сборка проекта
mvn clean install

# Запуск тестов
mvn test

# Запуск приложения
mvn exec:java -Dexec.mainClass="ru.bmstu.App"
```
