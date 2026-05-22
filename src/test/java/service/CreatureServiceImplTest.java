package service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.bmstu.domain.Creature;
import ru.bmstu.service.CreatureService;
import ru.bmstu.service.impl.CreatureServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreatureServiceImplTest {

    private CreatureService service;

    @BeforeEach
    void setUp() {
        List<Creature> testData = List.of(
                new Creature("1", "Ember", "Phoenix", "Calm", 5.0, 100.0, List.of("Healing")),
                new Creature("2", "Gloom", "Shadow Wolf", "Fierce", 8.0, 200.0, List.of("Tracking")),
                new Creature("3", "Pip", "Phoenix", "Playful", 2.0, 50.0, List.of("Speed"))
        );
        service = new CreatureServiceImpl(testData);
    }

    // 1. findAll возвращает все существа
    @Test
    void findAll_returnsAllCreatures() {
        assertEquals(3, service.findAll().size());
    }

    // 2. findBySpecies возвращает только нужный вид
    @Test
    void findBySpecies_returnsCorrectSubset() {
        List<Creature> result = service.findBySpecies("Phoenix");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getSpecies().equals("Phoenix")));
    }

    // 3. findBySpecies с несуществующим видом возвращает пустой список
    @Test
    void findBySpecies_returnsEmptyIfNotFound() {
        assertTrue(service.findBySpecies("Dragon").isEmpty());
    }

    // 4. findByName регистронезависимый поиск
    @Test
    void findByName_isCaseInsensitive() {
        assertFalse(service.findByName("ember").isEmpty());
        assertFalse(service.findByName("EMBER").isEmpty());
    }

    // 5. findById находит существо по id
    @Test
    void findById_returnsCorrectCreature() {
        Creature result = service.findById("1");
        assertNotNull(result);
        assertEquals("Ember", result.getName());
    }

    // 6. findById возвращает null если id не существует
    @Test
    void findById_returnsNullIfNotFound() {
        assertNull(service.findById("999"));
    }

    // 7. findByTemperament возвращает правильную выборку
    @Test
    void findByTemperament_returnsCorrectSubset() {
        List<Creature> result = service.findByTemperament("Calm");
        assertEquals(1, result.size());
        assertEquals("Ember", result.get(0).getName());
    }
}