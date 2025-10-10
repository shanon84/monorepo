# Neo4j Node ORM Library - Anweisungen für Claude Code

## Übersicht

Die **neo4j-node-orm** Bibliothek ist ein leichtgewichtiges ORM-Framework für Neo4j, das Spring Data Neo4j nutzt und
Bulk-Operations für effiziente Node- und Relationship-Erstellung bietet.

## Projekt-Informationen

- **Modul**: `libs/java/neo4j-node-orm`
- **Package**: `com.example.neo4j.nodeorm`
- **Parent POM**: `com.example:rest:${revision}`
- **Dependencies**:
    - Spring Boot Data Neo4j
    - Neo4j Migrations Spring Boot Starter (2.19.1)

## Architektur

### Hauptkomponenten

```
com.example.neo4j.nodeorm/
├── Neo4jNodeRepository          # Interface für Repository-Operationen
├── Neo4jNodeRepositoryImpl      # Implementierung mit Bulk-Operations
├── metadata/
│   ├── NodeMetadata             # Metadaten für Node-Klassen
│   ├── FieldMetadata            # Basis-Metadaten für Felder
│   ├── PropertyMetadata         # Property-Metadaten
│   ├── RelationshipMetadata     # Relationship-Metadaten
│   └── NodeMetadataExtractor    # Extrahiert Metadaten aus annotierten Klassen
└── validation/
    └── NodeValidator            # Validiert Node-Entities
```

### Funktionsweise

1. **Metadata Extraction**: `NodeMetadataExtractor` analysiert `@Node`-annotierte Klassen und extrahiert:
    - Node-Label (aus `@Node`)
    - ID-Feld (mit `@Id`)
    - Properties (mit `@Property`)
    - Relationships (mit `@Relationship`)

2. **Bulk Save Operation**: `Neo4jNodeRepositoryImpl.saveAll()` führt aus:
    - Validierung aller Entities
    - Rekursives Sammeln aller zu speichernden Nodes (inkl. Related Nodes)
    - Bulk-Insert aller Nodes (gruppiert nach Typ) via UNWIND
    - ID-Aktualisierung in den Original-Objekten
    - Bulk-Erstellung von Relationships

3. **Relationship Creation**: Unterstützt:
    - `OUTGOING` und `INCOMING` Richtungen
    - Collections und Single-Value Relationships
    - Bulk-Erstellung via UNWIND

## Code-Stil & Konventionen

### Lombok

- **Node-Entities**: Nur `@Data` verwenden (chain = true ist bereits GLOBAL in `lombok.config` gesetzt)
- **Services/Components**: `@RequiredArgsConstructor` für Constructor Injection
- **VERBOTEN**: `@Builder`, `@Value`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Accessors` (global gesetzt!)
- **NIEMALS** `@Accessors(chain = true)` hinzufügen - ist bereits in `lombok.config` aktiviert!

### Spring Data Neo4j Annotationen

**Node-Definition**:

```java
@Node("Person")
@Data
public class PersonNode {
    @Id
    private Long id;

    @Property("name")
    private String name;

    @Relationship(type = "WORKS_FOR", direction = Relationship.Direction.OUTGOING)
    private CompanyNode company;

    @Relationship(type = "WORKS_ON", direction = Relationship.Direction.OUTGOING)
    private List<ProjectNode> projects;
}
```

### Validierung

- ID-Felder müssen `null` sein beim Erstellen (IDs werden von Neo4j generiert)
- Alle Entities müssen eine `@Id`-annotierte Field haben
- Node-Namen werden aus `@Node` extrahiert

## Testing

### Test-Konventionen

- **Integration Tests**: `*IT.java` (z.B. `Neo4jNodeRepositoryIT.java`) - **BEVORZUGT**
    - **`@IT` Annotation verwenden** (aus `com.example.global.annotations`)
    - **`@Import(TestRepositoryConfiguration.class)` hinzufügen** für typisierte Repositories
    - **KEINE Constructor Injection** - stattdessen `@Autowired` auf Fields
    - Tatsächliche Neo4j-Instanz über Docker Compose
    - **KEINE Testcontainer** verwenden
- **Unit Tests**: `*Test.java` - nur für sehr komplexe Logik
- **Test Data**: Separate Package `testdata/` mit Node-Klassen und typisierten Repositories
    - Node-Klassen: `SimpleNode.java`, `PersonNode.java`, `CompanyNode.java`, `ProjectNode.java`
    - Repository-Interfaces: `SimpleNodeRepository.java`, `PersonNodeRepository.java`, etc.
    - Test-Konfiguration: `TestRepositoryConfiguration.java`

### Test-Setup

- **Konfiguration**: `application.yml` in `src/test/resources/`
    - Neo4j Connection (bolt://localhost:7687)
    - Authentication (username/password)
    - Migrations deaktivieren (`org.neo4j.migrations.enabled: false`)
- **Field Injection**: `@Autowired` auf Fields verwenden (KEINE Constructor Injection in Tests!)
- **Cleanup**: `@AfterEach` Methode zum Löschen aller Nodes (`MATCH (n) DETACH DELETE n`)
- **Podman Compose**: Neo4j muss laufen (`podman compose -f compose/docker-compose.yaml up -d`)

## Performance-Optimierung

### Bulk Operations

Die Library ist optimiert für Bulk-Operations:

1. **UNWIND für Nodes**: Alle Nodes eines Typs werden in einem Query erstellt
2. **Temp IDs**: `System.identityHashCode()` als temporäre ID während Bulk-Insert
3. **Grouping by Type**: Nodes werden nach Typ gruppiert für effiziente Queries
4. **Bulk Relationships**: Relationships werden gesammelt und in Bulk erstellt

### Cypher-Queries

**Node Creation**:

```cypher
UNWIND $nodes AS node
CREATE (n:NodeLabel)
SET n = node.properties
RETURN node.tempId AS tempId, id(n) AS generatedId
```

**Relationship Creation**:

```cypher
MATCH (source) WHERE id(source) = $sourceId
UNWIND $targetIds AS targetId
MATCH (target) WHERE id(target) = targetId
CREATE (source)-[:RELATIONSHIP_TYPE]->(target)
```

## Häufige Aufgaben

### Neue Node-Entity erstellen

1. Klasse mit `@Node("NodeLabel")` annotieren
2. `@Id private Long id;` hinzufügen
3. Properties mit `@Property` annotieren
4. Relationships mit `@Relationship` annotieren
5. Lombok: Nur `@Data` verwenden (chain = true ist bereits global gesetzt!)

### Repository verwenden

**Typisierte Repository-Interfaces (EMPFOHLEN)**:

Für jede Node-Klasse sollte ein typisiertes Repository-Interface erstellt werden:

```java
public interface PersonNodeRepository extends Neo4jNodeRepository<PersonNode> {
}
```

**Bean-Konfiguration für typisierte Repositories**:

```java
@TestConfiguration
@RequiredArgsConstructor
public class TestRepositoryConfiguration {

    private final Neo4jSaveService saveService;
    private final Neo4jFindService findService;
    private final Neo4jDeleteService deleteService;

    @Bean
    public PersonNodeRepository personNodeRepository() {
        return createTypedRepository(PersonNodeRepository.class);
    }

    @SuppressWarnings("unchecked")
    private <T, R extends Neo4jNodeRepository<T>> R createTypedRepository(Class<R> repositoryInterface) {
        Neo4jNodeRepositoryImpl<T> impl = new Neo4jNodeRepositoryImpl<>(saveService, findService, deleteService);

        return (R) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(impl, args);
                    } catch (Exception e) {
                        throw e.getCause() != null ? e.getCause() : e;
                    }
                }
        );
    }
}
```

**Verwendung in Services**:

```java
@RequiredArgsConstructor
@Service
public class MyService {
    private final PersonNodeRepository personNodeRepository;

    public List<PersonNode> savePersons(List<PersonNode> persons) {
        return StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();
    }
}
```

**Verwendung in Tests**:

```java
@IT
@Import(TestRepositoryConfiguration.class)
class MyIntegrationTest {

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @Test
    void shouldSavePerson() {
        PersonNode person = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe");

        PersonNode saved = personNodeRepository.save(person);

        assertThat(saved.getId()).isNotNull();
    }
}
```

### Neue Operation hinzufügen

1. Methode in `Neo4jNodeRepository` Interface deklarieren
2. Implementierung in `Neo4jNodeRepositoryImpl` hinzufügen
3. Bulk-Operation bevorzugen (UNWIND verwenden)
4. Tests schreiben

## Bekannte Einschränkungen

- **Nur Save-Operation**: Derzeit nur `saveAll()` implementiert
- **Keine Update-Logic**: Existing Nodes werden nicht geupdatet
- **Keine Cascade-Delete**: Relationships werden nicht automatisch gelöscht
- **ID-Format**: Nur `Long` als ID-Typ unterstützt

## Migration & Versioning

- Neo4j Migrations werden über `neo4j-migrations-spring-boot-starter` verwaltet
- Migration-Scripts in `src/main/resources/neo4j/migrations/`
- Cypher-basierte Migrations (z.B. `V001__initial_schema.cypher`)

## Debugging

### Logging

```yaml
logging:
    level:
        com.example.neo4j.nodeorm: DEBUG
        org.springframework.data.neo4j: DEBUG
```

### Häufige Fehler

1. **IllegalAccessException**: Field nicht accessible → Lombok/Reflection-Problem
2. **NullPointerException bei ID**: ID-Field nicht gefunden → `@Id` vergessen
3. **Relationship nicht erstellt**: Direction falsch oder Target-Node nicht gespeichert

## Best Practices

1. **Batch Size**: Große Listen in Batches aufteilen (z.B. 1000 Nodes pro Batch)
2. **Transaktionalität**: Service-Layer mit `@Transactional` annotieren
3. **Null-Checks**: Relationships können null sein
4. **ID-Management**: IDs nie manuell setzen, immer von Neo4j generieren lassen
5. **Testing**: Integration Tests mit Docker Compose Neo4j-Instanz

## Hinweise für Claude

### Code-Konventionen

- **Konfiguration**: Immer `application.yml` verwenden, NIEMALS `*.properties`
- **Lombok**: `@RequiredArgsConstructor` für Services, `@Data` für Entities
- **Constructor Injection**: Immer `private final` + `@RequiredArgsConstructor`, NIEMALS manuell Constructor schreiben
- **Klassen-Typen**: Entweder `@Data` (Model) ODER Component-Annotation
- **Eigene Annotations verwenden**:
    - **`@Persistence`**: Für Klassen mit Cypher/DB-Aufrufen (z.B. Neo4jNodeRepositoryImpl) - wichtig für Jaeger
    - **`@Validator`**: Für Validator-Klassen (z.B. NodeValidator)
    - **`@IT`**: Für Integration Tests

### Testing

- **BEVORZUGT**: Integration Tests (`*IT.java`) mit echter Neo4j-Instanz
- **`@IT` Annotation verwenden** - NIEMALS `@SpringBootTest` direkt
- **`@Import(TestRepositoryConfiguration.class)`**: Immer importieren für typisierte Repositories
- **Field Injection**: `@Autowired` auf Fields - KEINE Constructor Injection in Tests!
- **KEINE Testcontainer** verwenden - Docker Compose nutzen
- **Typisierte Repositories**: Immer typisierte Repository-Interfaces verwenden (z.B. `PersonNodeRepository` statt raw `Neo4jNodeRepository`)

### Performance & Security

- Repository ist generisch (`<T>`), aber spezialisiert auf Node-Entities
- Reflection wird intensiv genutzt → Performance bei großen Datasets beachten
- Cypher-Queries sind optimiert für Bulk → nicht einzeln ausführen
- Security: Keine User-Input direkt in Cypher-Queries (Injection-Gefahr)
