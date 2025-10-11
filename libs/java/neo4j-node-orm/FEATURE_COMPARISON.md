# Spring Data Feature Comparison - neo4j-node-orm

Dieser Dokument vergleicht die Fähigkeiten von Standard Spring Data Implementierungen (JPA, Neo4j) mit unserer aktuellen `neo4j-node-orm` Implementierung.

## ✅ Bereits Implementierte Features

### 1. **CRUD Operations**
- ✅ `save(entity)` - Einzelnes Entity speichern
- ✅ `saveAll(entities)` - Bulk Save
- ✅ `findById(id)` - Entity per ID finden
- ✅ `findAll()` - Alle Entities finden
- ✅ `findAllById(ids)` - Multiple Entities per ID finden
- ✅ `existsById(id)` - Prüfen ob Entity existiert
- ✅ `count()` - Anzahl aller Entities
- ✅ `deleteById(id)` - Entity per ID löschen
- ✅ `delete(entity)` - Entity-Objekt löschen
- ✅ `deleteAll()` - Alle Entities löschen
- ✅ `deleteAllById(ids)` - Multiple Entities per ID löschen

### 2. **Query Derivation - Basic**
- ✅ `findBy...` / `findAllBy...` - Einfache Property-Suche mit EQUALS
- ✅ `countBy...` - Zählen nach Kriterien
- ✅ `existsBy...` - Existenz-Check nach Kriterien
- ✅ `deleteBy...` / `deleteAllBy...` - Löschen nach Kriterien
- ✅ AND-Verknüpfung (`findByFirstNameAndLastName`)
- ✅ OR-Verknüpfung (`findByFirstNameOrLastName`)

### 2a. **Query Derivation - Erweiterte Operatoren** ✅ IMPLEMENTIERT
- ✅ `LessThan` - `findByAgeLessThan(Integer age)`
- ✅ `LessThanEqual` - `findByAgeLessThanEqual(Integer age)`
- ✅ `GreaterThan` - `findByAgeGreaterThan(Integer age)`
- ✅ `GreaterThanEqual` - `findByAgeGreaterThanEqual(Integer age)`
- ✅ `Between` - `findByAgeBetween(Integer start, Integer end)`
- ✅ `Before` - `findByBirthDateBefore(LocalDate date)`
- ✅ `After` - `findByBirthDateAfter(LocalDate date)`
- ✅ `Like` - `findByNameLike(String pattern)` mit Regex
- ✅ `NotLike` - `findByNameNotLike(String pattern)`
- ✅ `StartingWith` - `findByNameStartingWith(String prefix)`
- ✅ `EndingWith` - `findByNameEndingWith(String suffix)`
- ✅ `Containing` - `findByNameContaining(String substring)`
- ✅ `NotContaining` - `findByNameNotContaining(String substring)`
- ✅ `IsNull` - `findByBirthDateIsNull()` (nur für Properties)
- ✅ `IsNotNull` - `findByBirthDateIsNotNull()` (nur für Properties)
- ✅ `True` - `findByActiveTrue()`
- ✅ `False` - `findByActiveFalse()`
- ✅ `In` - `findByIdIn(List<String> ids)`
- ✅ `NotIn` - `findByIdNotIn(List<String> ids)`
- ✅ `Not` - `findByAgeNot(Integer age)`
- ✅ OR-Kombinationen (`findByFirstNameOrLastName`, `findByAgeGreaterThanOrLastName`)
- ⚠️ **Einschränkung**: IsNull/IsNotNull nur für Properties, NICHT für Relationships (requires EXISTS pattern)

### 3. **Custom Queries**
- ✅ `@Query` Annotation - Custom Cypher Queries
- ✅ Parameter-Binding mit `@Param`
- ✅ Positional Parameter Binding

### 4. **Relationships**
- ✅ `@Relationship` Annotation
- ✅ OUTGOING / INCOMING Richtungen
- ✅ Single-Value Relationships
- ✅ Collection Relationships (`List<T>`)
- ✅ Rekursive Relationships (z.B. PersonNode.manager → PersonNode)
- ✅ Bulk Relationship Creation

### 5. **Metadata & Reflection**
- ✅ `@Node` Annotation Support
- ✅ `@Property` Annotation Support
- ✅ `@Id` Annotation Support
- ✅ `@GeneratedValue` Support (UUID)
- ✅ Metadata Extraction via Reflection

### 6. **Auditing**
- ✅ `@CreatedBy` Support
- ✅ `@CreatedDate` Support
- ✅ `@LastModifiedBy` Support
- ✅ `@LastModifiedDate` Support

### 7. **Optimistic Locking**
- ✅ `@Version` Annotation Support
- ✅ Version Increment bei Updates
- ✅ OptimisticLockingFailureException bei Konflikten

### 8. **Bulk Operations**
- ✅ Bulk Node Creation via UNWIND
- ✅ Bulk Relationship Creation
- ✅ Grouping by Type für Performance

---

## ❌ Fehlende Features (Spring Data Standard)

### 1. **Query Derivation - Noch Fehlende Operatoren**

#### **String-Operatoren (Case Insensitive)**
- ❌ `IgnoreCase` - `findByNameIgnoreCase(String name)`
- ❌ `AllIgnoreCase` - `findByFirstNameAndLastNameAllIgnoreCase(...)`

### 2. **Sorting & Ordering**

#### **Method Name Sorting**
- ❌ `OrderBy` - `findByLastNameOrderByFirstNameAsc(String lastName)`
- ❌ `OrderBy` mit Multiple Fields - `findAllByOrderByLastNameAscFirstNameDesc()`

#### **Dynamic Sorting**
- ❌ `Sort` Parameter - `findAll(Sort sort)`
- ❌ `Sort` mit Query Methods - `findByLastName(String lastName, Sort sort)`
- ❌ `Sort.by(Direction, String...)` Support
- ❌ `Sort.by(Order...)` mit Complex Sorting

**Beispiele:**
```java
// Fehlt aktuell:
List<PersonNode> findByLastName(String lastName, Sort sort);
List<PersonNode> findAllByOrderByLastNameAsc();
List<PersonNode> findAll(Sort.by(Direction.DESC, "age", "lastName"));
```

### 3. **Pagination**

#### **Pageable Support**
- ❌ `Pageable` Parameter - `findAll(Pageable pageable)`
- ❌ `Page<T>` Return Type - Mit Total Count
- ❌ `Slice<T>` Return Type - Ohne Total Count
- ❌ `PageRequest.of(page, size)` Support
- ❌ `PageRequest.of(page, size, sort)` Support

#### **Limit & Offset**
- ❌ `Limit` Parameter - `findAll(Limit limit)`
- ❌ `Top` / `First` Keywords - `findTop10ByLastName(String lastName)`
- ❌ `First` mit Nummer - `findFirst3ByOrderByAgeDesc()`

**Beispiele:**
```java
// Fehlt aktuell:
Page<PersonNode> findByLastName(String lastName, Pageable pageable);
Slice<PersonNode> findByAge(Integer age, Pageable pageable);
List<PersonNode> findTop10ByOrderByAgeDesc();
List<PersonNode> findFirst5ByLastName(String lastName);
```

### 4. **Projections**

#### **Interface-Based Projections (Closed)**
- ❌ Closed Projections - Interface mit Getter für Subset von Properties
- ❌ Nested Projections - Hierarchische DTOs für Relationships

#### **Interface-Based Projections (Open)**
- ❌ Open Projections mit `@Value` - SpEL Expressions
- ❌ Berechnete Properties - Kombinierte Felder

#### **DTO/Class-Based Projections**
- ❌ DTO Projections - Constructor-basierte Projektion
- ❌ Java Records als DTO

#### **Dynamic Projections**
- ❌ Generic Projection Parameter - `<T> T findByLastName(String lastName, Class<T> type)`

**Beispiele:**
```java
// Fehlt aktuell:

// Closed Projection
interface PersonNameOnly {
    String getFirstName();
    String getLastName();
}
List<PersonNameOnly> findAllProjectedBy();

// Open Projection mit SpEL
interface PersonSummary {
    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();

    Integer getAge();
}

// DTO Projection
record PersonDto(String firstName, String lastName) {}
List<PersonDto> findAllBy();

// Dynamic Projection
<T> List<T> findByLastName(String lastName, Class<T> type);
```

### 5. **Return Types**

#### **Streaming**
- ❌ `Stream<T>` - Java 8 Streams für große Datasets
- ❌ `Streamable<T>` - Spring Data Custom Streamable

#### **Async/CompletableFuture**
- ❌ `@Async` Annotation Support
- ❌ `CompletableFuture<T>` Return Type
- ❌ `CompletableFuture<List<T>>` Return Type
- ❌ Custom TaskExecutor Support

#### **Optional**
- ❌ `Optional<T>` Return Type - Für Single Results

**Beispiele:**
```java
// Fehlt aktuell:
Stream<PersonNode> streamAllBy();
Streamable<PersonNode> findAllBy();
Optional<PersonNode> findByEmail(String email);

@Async
CompletableFuture<List<PersonNode>> findAllByLastName(String lastName);
```

### 6. **Specifications API**

- ❌ `JpaSpecificationExecutor<T>` Interface
- ❌ `Specification<T>` für dynamische Queries
- ❌ `PredicateSpecification` für WHERE Clauses
- ❌ `UpdateSpecification` für Updates
- ❌ `DeleteSpecification` für Deletes
- ❌ Kombinieren von Specifications (AND, OR, NOT)

**Beispiel:**
```java
// Fehlt aktuell:
public interface PersonNodeRepository extends Neo4jNodeRepository<PersonNode>,
                                              JpaSpecificationExecutor<PersonNode> {
}

// Dynamic Query Building
Specification<PersonNode> spec = Specification
    .where(hasLastName("Doe"))
    .and(ageGreaterThan(18))
    .or(hasFirstName("John"));

List<PersonNode> result = repository.findAll(spec);
```

### 7. **Query by Example (QBE)**

- ❌ `QueryByExampleExecutor<T>` Interface
- ❌ `Example<T>` für Template-basierte Suche
- ❌ `ExampleMatcher` für Custom Matching Rules

**Beispiel:**
```java
// Fehlt aktuell:
PersonNode probe = new PersonNode()
    .setLastName("Doe")
    .setAge(30);

Example<PersonNode> example = Example.of(probe);
List<PersonNode> result = repository.findAll(example);
```

### 8. **Querydsl Support**

- ❌ `QuerydslPredicateExecutor<T>` Interface
- ❌ Type-Safe Queries mit Q-Classes
- ❌ Predicate Builder Support

### 9. **Scrolling / Window**

- ❌ `ScrollPosition` Parameter
- ❌ `Window<T>` Return Type - Für Infinite Scrolling
- ❌ Keyset-based Scrolling (bessere Performance als Offset)

**Beispiel:**
```java
// Fehlt aktuell:
Window<PersonNode> findTop10By(ScrollPosition position);
```

### 10. **Named Queries**

- ❌ Named Queries via XML/Properties
- ❌ `@NamedQuery` Annotation Support
- ❌ Query Lookup Strategy Configuration

### 11. **Modifying Queries**

- ❌ `@Modifying` Annotation - Für UPDATE/DELETE Queries
- ❌ Bulk Update Support
- ❌ Bulk Delete Support mit Custom Queries

**Beispiel:**
```java
// Fehlt aktuell:
@Modifying
@Query("MATCH (n:Person) WHERE n.lastName = $lastName SET n.active = false")
void deactivateByLastName(@Param("lastName") String lastName);
```

### 12. **Entity Graph / Fetch Strategies**

- ❌ `@EntityGraph` Support für Lazy/Eager Loading
- ❌ Custom Fetch Depth für Relationships
- ❌ Fetch Join Strategies

### 13. **Lock Support**

- ❌ `@Lock` Annotation - Pessimistic/Optimistic Locking Strategies
- ❌ `LockModeType` Configuration

### 14. **Transactional Support**

- ✅ Grundlegende Transaktionen via Spring `@Transactional` werden unterstützt
- ❌ Custom `@Transactional` Propagation in Repositories
- ❌ Read-Only Transactions Optimization
- ❌ Transaction Timeout Configuration

### 15. **Caching**

- ❌ Spring Cache Abstraction Integration (`@Cacheable`, `@CacheEvict`)
- ❌ Second-Level Cache Support

### 16. **Validation**

- ✅ Basis-Validierung (Node Annotation, ID Field)
- ❌ `@Valid` Parameter Annotation
- ❌ JSR-303/JSR-380 Bean Validation Integration

### 17. **Events**

- ❌ Spring Data Events (`@HandleBeforeSave`, `@HandleAfterSave`)
- ❌ Domain Events (`@DomainEvents`, `@AfterDomainEventPublication`)
- ❌ Lifecycle Callbacks

### 18. **Query Execution Hints**

- ❌ `@QueryHints` Support
- ❌ Performance Hints für Cypher Queries

---

## 🔄 Teilweise Implementiert

### 1. **Query Derivation**
- ✅ Basis-Support (findBy, countBy, existsBy, deleteBy)
- ✅ EQUALS, NOT_EQUALS Operatoren
- ✅ Erweiterte Vergleichsoperatoren (GreaterThan, LessThan, Between, Before, After)
- ✅ String-Operatoren (Like, StartingWith, EndingWith, Containing)
- ✅ Boolean-Operatoren (True, False)
- ✅ Null-Checks (IsNull, IsNotNull) - nur für Properties
- ✅ Collection-Operatoren (In, NotIn)
- ✅ Logische Operatoren (AND, OR, NOT)
- ❌ Case Insensitive (IgnoreCase, AllIgnoreCase)
- ❌ Kein Sorting/Ordering

### 2. **Return Types**
- ✅ `List<T>` Support
- ✅ Single Entity Support
- ✅ `Long` für Count
- ✅ `Boolean` für Exists
- ❌ `Stream<T>`, `Optional<T>`, `CompletableFuture<T>`
- ❌ `Page<T>`, `Slice<T>`, `Window<T>`

### 3. **Auditing**
- ✅ Basic Auditing Fields (@CreatedBy, @CreatedDate, etc.)
- ✅ AuditorAware Integration
- ❌ @EnableJpaAuditing Stil Configuration

---

## 📊 Feature-Priorität für Implementierung

### **Priority 1 (High Impact, Common Use Cases)**

1. ✅ ~~**Query Derivation - Erweiterte Operatoren**~~ **IMPLEMENTIERT**
   - ✅ ~~`GreaterThan`, `LessThan`, `Between`~~
   - ✅ ~~`Like`, `Containing`, `StartingWith`, `EndingWith`~~
   - ✅ ~~`IsNull`, `IsNotNull`~~
   - ✅ ~~`In`, `NotIn`~~

2. **Sorting** ⬅️ NÄCHSTER SCHRITT
   - `Sort` Parameter Support
   - `OrderBy` in Method Names

3. **Pagination**
   - `Pageable` Parameter
   - `Page<T>` / `Slice<T>` Return Types
   - `Top` / `First` Keywords

4. **Return Types**
   - `Optional<T>` Support
   - `Stream<T>` Support

### **Priority 2 (Moderate Impact)**

5. **Projections**
   - Interface-Based Closed Projections
   - DTO/Class-Based Projections
   - Dynamic Projections

6. **OR Logic**
   - `Or` in Query Derivation
   - `findByFirstNameOrLastName(...)`

7. **Case Insensitive**
   - `IgnoreCase` Suffix
   - `AllIgnoreCase` für Multiple Fields

8. **Limit**
   - `Limit` Parameter
   - `Top`/`First` Keywords

### **Priority 3 (Nice to Have)**

9. **Async Support**
   - `@Async` mit `CompletableFuture<T>`

10. **Modifying Queries**
    - `@Modifying` Annotation
    - Bulk Updates/Deletes

11. **Specifications API**
    - Dynamic Query Building
    - Composable Specifications

12. **Query by Example**
    - Template-based Queries

13. **Scrolling**
    - `Window<T>` Return Type
    - Keyset-based Pagination

### **Priority 4 (Advanced/Specialized)**

14. **Querydsl Integration**
15. **Entity Graph / Fetch Strategies**
16. **Lock Support** (beyond @Version)
17. **Caching Integration**
18. **Events & Lifecycle Callbacks**

---

## 💡 Empfehlungen

### **Nächste Schritte:**

1. ✅ ~~**Phase 1 - Query Derivation Erweitern**~~ **ABGESCHLOSSEN**
   - ✅ ~~Implementiere erweiterte Operatoren (GreaterThan, LessThan, Like, etc.)~~
   - ✅ ~~Füge OR-Unterstützung hinzu~~
   - ❌ Implementiere IgnoreCase (später)

2. **Phase 2 - Sorting & Pagination** ⬅️ AKTUELL
   - `Sort` Parameter Support
   - `Pageable` Support mit `Page<T>` / `Slice<T>`
   - `Top`/`First` Keywords

3. **Phase 3 - Projections**
   - Interface-Based Projections (Closed)
   - DTO-Based Projections
   - Dynamic Projections

4. **Phase 4 - Return Types & Async**
   - `Optional<T>` Support
   - `Stream<T>` Support
   - `@Async` mit `CompletableFuture<T>`

### **Architektur-Überlegungen:**

- **QueryMethodParser** erweitern für komplexere Patterns
- **QueryOperator** Enum erweitern (derzeit nur EQUALS)
- **CypherQueryGenerator** erweitern für OR, Sorting, Paging
- Neue Klassen: `SortSpecification`, `PageableSupport`, `ProjectionHandler`

---

## 📝 Zusammenfassung

**Aktueller Stand:**
- ✅ Solide Basis mit CRUD, Basic Query Derivation, @Query, Relationships
- ✅ Bulk Operations, Auditing, Optimistic Locking
- ✅ Rekursive Relationships funktionieren
- ✅ **NEU**: Erweiterte Query Derivation Operatoren (GreaterThan, LessThan, Between, Like, Containing, etc.)
- ✅ **NEU**: OR-Verknüpfung in Query Methods
- ✅ **NEU**: Boolean, Null-Checks, Collection-Operatoren

**Hauptlücken:**
- ❌ Sorting & Pagination
- ❌ Projections
- ❌ Async/Stream Support
- ❌ Specifications API
- ❌ Case Insensitive Operatoren

**Nächster Fokus:**
Sorting & Ordering (OrderBy in method names, Sort parameter) und Pagination (Pageable, Top/First keywords).
