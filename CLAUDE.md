# Projekt-Anweisungen für Claude Code

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
- **Lombok**:
  - `@Data` mit `@Accessors(chain = true)` verwenden
  - VERBOTEN: `@Builder`, `@Value`, `@SneakyThrows`, `@Wither`, `@NoArgsConstructor`, `@AllArgsConstructor`
  - Stattdessen: `@RequiredArgsConstructor` für Constructor Injection
  - Lombok fügt automatisch `@Generated` Annotation hinzu

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
- **Integration Tests**: Suffix `IT.java` (z.B. `HelloServiceIT.java`)
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

## Nx Cache

Folgende Operations sind cacheable:
- `build`, `test`, `lint`, `e2e`
- `build-BE`, `unittest-BE`, `integrationtest-BE`

## Häufige Aufgaben

### Neues Java Modul hinzufügen

1. Maven Modul in `pom.xml` registrieren
2. Nx `project.json` erstellen (falls benötigt)
3. Parent POM referenzieren

### Dependency aktualisieren

1. In `package.json` Version ändern
2. `yarn install` ausführen
3. Tests ausführen

### OpenAPI ändern

1. YAML-Datei in `libs/openapi/openapi-files/` bearbeiten
2. `yarn generate-api` ausführen
3. Generated Code committen

## Performance

- Nx führt nur betroffene Projekte aus (`affected`)
- Verwende `yarn *:affected` für schnellere Builds
- Cache wird in `.nx/cache/` gespeichert

## Hinweise für Claude

- Bei Java-Code immer Lombok-Konventionen beachten
- Vor größeren Änderungen: Tests ausführen
- OpenAPI-Generated Code NICHT manuell editieren
- Nx Dependency Graph berücksichtigen
- Security Audits ernst nehmen
