# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Projekt-Anweisungen für Claude Code

## Quick Reference

```bash
# Entwicklungs-Workflow (nur betroffene Projekte)
yarn format-BE:affected          # Code formatieren
yarn build-BE:affected           # Build (ohne Tests)
yarn unittest-BE:affected        # Unit Tests
yarn integrationtest-BE:affected # Integration Tests

# Einzelnes Projekt
nx build-BE <projekt-name>
nx unittest-BE <projekt-name>
nx integrationtest-BE <projekt-name>

# Neo4j für Tests starten
podman compose -f compose/docker-compose.yaml up -d

# Nx Cache zurücksetzen
nx reset
```

## Projektübersicht

Dies ist ein Java/TypeScript Nx Monorepo-Projekt mit:

- **Backend**: Java 21, Spring Boot, Maven
- **Frontend**: Next.js 15, React 19, TypeScript
- **Build-System**: Nx 21.6.3 mit Maven-Integration
- **Package Manager**: Yarn 4.10.3

## Projekt-Struktur

```
monorepo/
├── apps/
│   └── hello-app/           # Spring Boot Anwendung
├── libs/
│   ├── java/
│   │   ├── parent/          # Maven Parent POMs
│   │   ├── global/          # Globale Annotationen
│   │   ├── archunit/        # ArchUnit Tests
│   │   └── my-lib1/         # Java Bibliothek
│   └── openapi/             # OpenAPI Definitionen & Generierung
├── pom.xml                  # Root Maven Aggregator
├── nx.json                  # Nx Konfiguration
├── lombok.config            # Lombok Konfiguration
└── package.json             # Node Dependencies
```

## Code-Stil & Konventionen

### Java

- **Java Version**: 21
- **Package Naming**: `com.example.*`
- **Package-Struktur**:
    - **Fachliche Packages** bevorzugen statt technischer
    - **SCHLECHT**: `service`, `persistence`, `controller`, `repository`
    - **GUT**: `save`, `find`, `delete`, `user`, `order`, etc.
    - Beispiel: `com.example.neo4j.nodeorm.save` mit `Neo4jSaveService` und `Neo4jSavePersistence`
    - Pro fachlichem Package: Service + Persistence + ggf. weitere Klassen
- **Konfiguration**: Immer `application.yml` verwenden, NIEMALS `*.properties` Dateien
- **Lombok**:
    - **lombok.config**: Zentrale Konfiguration in `lombok.config` im Root-Verzeichnis
        - `lombok.accessors.chain = true` ist GLOBAL aktiviert - NIEMALS `@Accessors(chain = true)` manuell hinzufügen!
        - `lombok.addLombokGeneratedAnnotation = true` - automatische `@Generated` Annotation
        - Verbotene Annotationen werden durch `flagUsage = ERROR` erzwungen
    - **Model-Klassen**: Nur `@Data` für Entities/DTOs (chain = true ist bereits global gesetzt)
    - **Service/Component-Klassen**: `@RequiredArgsConstructor` + entsprechende Spring-Annotation (`@Service`,
      `@Controller`, `@Repository`, etc.)
    - **Constructor Injection**: Immer `private final` Fields + `@RequiredArgsConstructor` verwenden
    - **VERBOTEN** (via lombok.config): `@Builder`, `@Value`, `@SneakyThrows`, `@NoArgsConstructor`,
      `@AllArgsConstructor`
    - **NIEMALS** manuell Constructors schreiben - Lombok nutzen!
    - **NIEMALS** `@Accessors(chain = true)` hinzufügen - ist bereits global gesetzt!
- **Klassen-Typen**:
    - Entweder Model-Klasse (`@Data`) ODER Component (`@Service`/`@Controller`/`@Repository`)
    - Niemals beides mischen!
- **Delegate Pattern für Interface-Implementierungen**:
    - Bei Implementierungen größerer Interfaces (z.B. `CrudRepository`, `JpaRepository`) immer Delegate Pattern
      verwenden
    - Die implementierende Klasse delegiert nur an weitere Services weiter
    - Je nach Shared Code: Entweder aggregierende Services ODER einzelne Services pro Methode
    - Beispiel: `Neo4jNodeRepositoryImpl` delegiert an `Neo4jPersistence`, `NodeValidator`, etc.
    - **Keine Geschäftslogik** in der Interface-Implementierung selbst
- **Eigene Annotationen** (aus `com.example.global.annotations`):
    - **`@Persistence`**: Für Klassen, die Cypher/DB-Aufrufe kapseln (wichtig für Jaeger Performance-Messung)
    - **`@Properties`**: Für Klassen, die Properties aus `application.yml` auslesen (`@Configuration`)
    - **`@Scheduler`**: Für Klassen, die Scheduling bereitstellen
    - **`@Validator`**: Für Klassen, die rein validieren
    - **`@IT`**: Für Integration Tests (statt `@SpringBootTest`)

### TypeScript/React

- **TypeScript Version**: 5.9.3
- **React Version**: 19.2.0
- **Next.js Version**: 15.5.4
- ESLint-Regeln befolgen
- Prettier für Code-Formatierung

## Wichtige Befehle

### Backend (Java/Maven)

```bash
# Build Backend (ohne Tests)
yarn build-BE

# Build nur betroffene Projekte
yarn build-BE:affected

# Unit Tests
yarn unittest-BE

# Integration Tests
yarn integrationtest-BE

# OWASP Dependency Check
yarn owasp-BE

# Code Formatierung
yarn format-BE
```

### OpenAPI

```bash
# OpenAPI Code generieren
yarn generate-api
yarn generate-api:affected
```

### Nx Befehle

```bash
# Affected Graph anzeigen
nx affected:graph

# Spezifisches Projekt builden
nx build-BE hello-app
nx build-BE neo4j-node-orm
nx build-BE my-lib1

# Einzelnes Projekt testen
nx unittest-BE hello-app
nx integrationtest-BE neo4j-node-orm
```

## Build-Targets (Nx)

- **build-BE**: Maven `clean install -DskipTests -DskipITs`
- **unittest-BE**: Maven `surefire:test arch-unit:arch-test -DskipITs`
- **integrationtest-BE**: Maven `failsafe:integration-test` mit JaCoCo
- **owasp-BE**: Maven `dependency-check:check`
- **generate-api**: OpenAPI Code-Generierung
- **format-BE**: Prettier Formatierung

## Testing

### Java Tests

- **Unit Tests**: Suffix `Test.java` (z.B. `HelloServiceTest.java`)
    - Nur für sehr komplexe Service-Logik
    - Vorzugsweise Integrationstests verwenden
- **Integration Tests**: Suffix `IT.java` (z.B. `HelloServiceIT.java`) - **BEVORZUGT**
    - Tatsächliche Interaktion zwischen Datenbank und Anwendung
    - Docker Compose für Datenbanken (z.B. Neo4j)
    - **KEINE Testcontainer** verwenden
    - **`@IT` Annotation verwenden** (eigene Annotation aus `com.example.global.annotations`)
    - **KEINE Constructor Injection** - stattdessen `@Autowired` auf Fields
- **Test-Konfiguration**: `application.yml` in `src/test/resources/`
- **ArchUnit Tests**: Separates Modul `libs/java/archunit/global/`
- **Test Coverage**: JaCoCo aktiviert, Reports in `target/jacoco.exec`

## Maven Konventionen

- **Parent POMs**: Zentrale Dependency-Verwaltung in `libs/java/parent/`
- **Version**: Unified Revision `${revision}` = `0.0.1-SNAPSHOT`
- **Local Repo**: `.m2/repository` (Nx-managed)
- **JaCoCo**: Automatisch aktiviert für Test-Coverage

## OpenAPI

- **Definitionen**: `libs/openapi/openapi-files/`
- **Generator**: `libs/openapi/pet/generator/`
- **Generated Code**: `libs/openapi/pet/server/src/main/java/`
- **Package**: `org.openapitools.api` und `org.openapitools.model`

## Sicherheit

### Dependency Management

- Regelmäßig `yarn npm audit` ausführen
- OWASP Dependency Check mit `yarn owasp-BE`
- Keine kompromittierten npm-Packages verwenden (z.B. Shai-Hulud Wurm)

### Spring Security

- SecurityConfig in Bibliotheken/Apps implementieren
- JWT-basierte Authentifizierung verwenden

## Branch & Git

- **Main Branch**: `master`
- **Commit-Stil**: Konventionell (z.B. "fix:", "feat:", "refactor:")
- Vor Commit: Tests und Build erfolgreich

## Docker Services

### Neo4j (für Integration Tests)

```bash
# Neo4j starten
podman compose -f compose/docker-compose.yaml up -d

# Neo4j stoppen
podman compose -f compose/docker-compose.yaml down

# Logs ansehen
podman compose -f compose/docker-compose.yaml logs -f neo4j

# Status prüfen
podman compose -f compose/docker-compose.yaml ps
```

**Zugriff**:

- Browser UI: http://localhost:7474
- Bolt Protocol: bolt://localhost:7687
- Credentials: `neo4j / password` (siehe `compose/docker-compose.yaml`)

### Nx Remote Cache (optional, für Team-Entwicklung)

Der Nx Remote Cache ermöglicht das Teilen von Build-Caches zwischen Entwicklern und CI/CD.

```bash
# Nx Remote Cache starten (MinIO + nx-cache-server)
podman compose -f compose/docker-compose-nxcache.yaml up -d

# Nx Remote Cache stoppen
podman compose -f compose/docker-compose-nxcache.yaml down

# Logs ansehen
podman compose -f compose/docker-compose-nxcache.yaml logs -f

# Status prüfen
podman compose -f compose/docker-compose-nxcache.yaml ps
```

**Zugriff**:

- Nx Cache Server: http://localhost:3000
- MinIO Console: http://localhost:9001
- MinIO S3 API: http://localhost:9000
- Credentials: `minioadmin / minioadmin`

**Konfiguration in nx.json**:

```json
{
    "tasksRunnerOptions": {
        "default": {
            "runner": "@nx/workspace/tasks-runners/default",
            "options": {
                "cacheableOperations": ["build-BE", "unittest-BE", "integrationtest-BE"],
                "remoteCache": {
                    "url": "http://localhost:3000",
                    "token": "my-secret-token"
                }
            }
        }
    }
}
```

## Nx Cache

Folgende Operations sind cacheable:

- `build`, `test`, `lint`, `e2e`
- `build-BE`, `unittest-BE`, `integrationtest-BE`

**Cache zurücksetzen**: `nx reset`

## Häufige Aufgaben

### Neues Java Modul hinzufügen

1. Maven Modul in `pom.xml` registrieren
2. Nx `project.json` erstellen (falls benötigt)
3. Parent POM referenzieren

### Dependencies aktualisieren

**JavaScript/TypeScript**:

```bash
# Interaktive Auswahl
yarn upgrade-interactive

# Alle Dependencies aktualisieren
yarn upgrade
```

**Maven**:

```bash
# Maven Dependencies aktualisieren (empfohlen)
./scripts/update-maven-dependencies.sh

# Manuell: Parent POM Versionen prüfen
mvn versions:display-parent-updates

# Manuell: Property-Versionen prüfen
mvn versions:display-property-updates
```

**Nx**:

```bash
# Nx auf neueste Version migrieren
nx migrate latest

# Migrations ausführen
nx migrate --run-migrations
```

### OpenAPI ändern

1. YAML-Datei in `libs/openapi/openapi-files/` bearbeiten
2. `yarn generate-api` ausführen
3. Generated Code committen

## Performance

- Nx führt nur betroffene Projekte aus (`affected`)
- Verwende `yarn *:affected` für schnellere Builds
- Cache wird in `.nx/cache/` gespeichert

## Troubleshooting

### Häufige Probleme

**Build schlägt fehl: "Cannot resolve dependency"**

```bash
# Lösung: Local Maven Repository löschen und neu builden
rm -rf .m2/repository
yarn build-BE
```

**Tests schlagen fehl: "Connection refused" (Neo4j)**

```bash
# Lösung: Podman Compose Neo4j starten
podman compose -f compose/docker-compose.yaml up -d
# Warten bis Neo4j bereit ist (ca. 10-30 Sekunden)
podman compose -f compose/docker-compose.yaml logs -f neo4j
```

**Nx Cache Probleme / Builds nicht aktuell**

```bash
# Lösung: Cache löschen und neu builden
nx reset
yarn build-BE:affected
```

**Lombok Annotationen funktionieren nicht in IDE**

- Stelle sicher, dass Lombok Plugin in IDE installiert ist
- Prüfe `lombok.config` im Root-Verzeichnis
- IDE Projekt neu laden / reimport
- Rebuild: `yarn build-BE`

**Integration Tests schlagen fehl: "Schema mismatch"**

```bash
# Lösung: Neo4j Datenbank zurücksetzen
podman compose -f compose/docker-compose.yaml down -v
podman compose -f compose/docker-compose.yaml up -d
```

**Port bereits belegt (7687, 7474)**

```bash
# Neo4j Ports prüfen
podman compose -f compose/docker-compose.yaml ps
lsof -i :7687
lsof -i :7474

# Container stoppen
podman compose -f compose/docker-compose.yaml down
```

## Projekt-Dokumentation

### CLAUDE.md Dateien

Das Monorepo nutzt ein hierarchisches Dokumentationssystem:

- **Root `CLAUDE.md`** (diese Datei): Globale Konventionen für alle Projekte
- **Projekt-spezifische `CLAUDE.md`**: Architektur und Patterns einzelner Libraries

**Existierende projekt-spezifische Dokumentationen**:

- `libs/java/neo4j-node-orm/CLAUDE.md` - Neo4j ORM Framework

**WICHTIG**: Bei Anpassungen an Projekten:

1. Prüfe, ob eine projekt-spezifische `CLAUDE.md` existiert
2. Aktualisiere die `CLAUDE.md` bei strukturellen/architektonischen Änderungen
3. Halte die Dokumentation synchron mit dem Code
4. Bei neuen Konventionen/Patterns: Dokumentiere sie in der jeweiligen `CLAUDE.md`

**Workflow für neue Anweisungen**:

Wenn der User eine grundsätzliche Anweisung gibt (z.B. "programmiere grundsätzlich nach Muster X"):

1. **Globale Anweisungen** → In diese Root-`CLAUDE.md` einfügen
    - Betrifft alle Projekte im Monorepo
    - Allgemeine Konventionen, Code-Stil, Workflows

2. **Projekt-spezifische Anweisungen** → In die entsprechende Projekt-`CLAUDE.md` einfügen
    - Betrifft nur ein spezifisches Projekt/Bibliothek
    - Architektur, Pattern, projektspezifische Regeln

## Hinweise für Claude

- Bei Java-Code immer Lombok-Konventionen beachten
- OpenAPI-Generated Code NICHT manuell editieren
- Nx Dependency Graph berücksichtigen
- Security Audits ernst nehmen
- **CLAUDE.md Dateien aktuell halten**: Bei Änderungen an Projekten die entsprechende Dokumentation aktualisieren

### Test-First Workflow (VERPFLICHTEND)

**Bei JEDER Code-Änderung** MUSS folgender Workflow eingehalten werden:

1. **Anforderung implementieren**: Code-Änderung durchführen
2. **Tests AUTOMATISCH schreiben**:
    - **NIEMALS darauf warten**, dass der User Tests anfordert
    - **IMMER selbstständig** Tests für neue Funktionalität schreiben
    - Integration Tests bevorzugen (Suffix `IT.java`)
    - Tests müssen das neue Verhalten vollständig abdecken
3. **Build ausführen**: `yarn build-BE` oder `nx build-BE <projekt-name>`
4. **Unit Tests ausführen**: `yarn unittest-BE` oder `nx unittest-BE <projekt-name>`
5. **Integration Tests ausführen**: `yarn integrationtest-BE` oder `nx integrationtest-BE <projekt-name>`
6. **Bei Fehlschlägen**:
    - **Test anpassen**: Wenn die fachliche Änderung korrekt ist und der Test veraltet
    - **Code fixen**: Wenn die Implementierung fehlerhaft ist
    - **Wiederhole Schritte 3-5** bis alle Tests grün sind
7. **Dokumentation aktualisieren**: CLAUDE.md bei neuen Konventionen/Patterns aktualisieren

**WICHTIG**:

- **Tests sind PFLICHT**: Jede neue Anforderung benötigt automatisch Tests - OHNE zu fragen!
- Änderungen NIEMALS ohne erfolgreiche Tests committen
- Affected-Targets verwenden für schnellere Feedbackzyklen: `yarn build-BE:affected`
- Bei Neo4j-Projekten: Podman Compose starten (`podman compose -f compose/docker-compose.yaml up -d`)
- Formatierung vor Tests: `yarn format-BE`
- **CLAUDE.md aktualisieren**: Bei neuen Konventionen immer die entsprechende CLAUDE.md anpassen
