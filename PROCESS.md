# PROCESS.md

Step-by-step log of actions taken during this session. Updated incrementally as work progresses.

---

## Session: 2026-07-01

### Step 1 — Repository exploration
- Reviewed repository structure: Kotlin + Ktor RealWorld example app.
- Inspected `README.md`, `build.gradle`, and source tree under `src/`.
- Identified key packages: `config`, `domain`, `web`, `utils`, `ext`.
- Reviewed recent git history to understand change velocity and maintenance state.

### Step 2 — Agent documentation scaffolding
- Created `AGENTS.md` — describes agent documentation standards and repo conventions.
- Created `PROCESS.md` (this file) — running log of session steps.
- Created `LESSONS_LEARNED.md` — issues and resolutions log.
- Created `NEXT_STEPS.md` — handoff document for future sessions.

### Step 3 — `./gradlew clean build` (first attempt)
- **Result:** FAILED
- Dependency resolution failed: `org.jetbrains.exposed:exposed:0.14.1` could not be found.
- Root cause: JCenter was shut down in 2022; the old monolithic Exposed artifact predates the modular split and never made it to Maven Central.

### Step 4 — Dependency and toolchain upgrades
- **Gradle:** 4.10 → 8.7 (Java 21 requires Gradle 8.5+; Java 19 unavailable via Homebrew as discontinued; Java 21 LTS adopted)
- **Kotlin:** `1.3.+` → `1.6.21`
- **Ktor:** `1.2.3` → `1.6.8` (latest 1.x, compatible with Kotlin 1.6)
- **Exposed:** `0.14.1` (monolith, dead on JCenter) → `0.41.1` (modular: `exposed-core`, `exposed-dao`, `exposed-jdbc`; lowest Maven Central version compatible with Kotlin 1.6)
- **Repositories:** removed dead `jcenter()` from both `buildscript` and `dependencies` blocks
- **Dependency scope:** `api` → `implementation`, `testCompile` → `testImplementation`

### Step 5 — Exposed API migration (source changes)
- `UserRepository.kt`: Switched `Users` and `Follows` from `LongIdTable` to plain `Table` with explicit `id: Column<Long>` + `override val primaryKey`. Changed `insertAndGetId { }.value` → `insert { }[Users.id]` and `row[Users.id].value` → `row[Users.id]`. Added compound PK to `Follows` via `PrimaryKey(user, follower)`.
- `TagRepository.kt`: Updated `LongIdTable` import path: `org.jetbrains.exposed.dao` → `org.jetbrains.exposed.dao.id`.
- `deleteWhere` fix: In Exposed 0.37.3+, the lambda changed to `T.(ISqlExpressionBuilder) -> Op<Boolean>`, making `eq` out of scope by default. Fixed by wrapping with `with(it) { ... }`.

### Step 6 — Ktor 1.6 API migration (source changes)
- `App.kt`: Added `@OptIn(EngineAPI::class, KtorExperimentalAPI::class)` to `main()` — Kotlin 1.6 requires explicit opt-in for `@RequiresOptIn`-annotated APIs.
- `AppRule.kt` (tests): Same opt-in fix + updated `stop(500, 500, TimeUnit.MILLISECONDS)` → `stop(500L, 500L)` — `TimeUnit` overload removed in Ktor 1.6.

### Step 7 — `./gradlew clean build` (final)
- **Result:** BUILD SUCCESSFUL ✓
- Warnings only (deprecated `@KtorExperimentalAPI` annotations, unused variables in stub controllers) — no errors.

---

_Append new steps below as work continues._
