# LESSONS_LEARNED.md

Documents issues encountered during sessions and the solutions found. Helps future agents avoid repeating known pitfalls.

---

## Session: 2026-07-01

### JCenter shutdown breaks `exposed:0.14.1` dependency

**Symptom:**
`./gradlew clean build` fails immediately with:
```
Could not find org.jetbrains.exposed:exposed:0.14.1.
Searched in: mavenCentral, jcenter
```

**Root Cause:**
JCenter (jcenter.bintray.com) was permanently shut down in February 2022. The monolithic `org.jetbrains.exposed:exposed` artifact (pre-0.17.x) was only ever published to JCenter and never migrated to Maven Central. Starting from version 0.17.x, Exposed switched to modular artifacts (`exposed-core`, `exposed-dao`, `exposed-jdbc`) on Maven Central — but the lowest version actually present in Maven Central is **0.30.1**.

**Resolution:**
1. Removed `jcenter()` from both `buildscript.repositories` and `repositories` blocks in `build.gradle`.
2. Upgraded Exposed to `0.41.1` (modular), replacing the single dependency with three:
   ```groovy
   implementation "org.jetbrains.exposed:exposed-core:$exposed_version"
   implementation "org.jetbrains.exposed:exposed-dao:$exposed_version"
   implementation "org.jetbrains.exposed:exposed-jdbc:$exposed_version"
   ```

**Files Affected:**
- `build.gradle`

---

### Gradle 4.10 / 7.6.4 incompatible with Java 21

**Symptom:**
```
BUG! exception in phase 'semantic analysis' ... Unsupported class file major version 65
```
(class file major version 65 = Java 21)

**Root Cause:**
Gradle 4.10 predates Java 21 by years. Gradle 7.6.4 only supports up to Java 19. The machine runs Java 21 (Amazon Corretto LTS). Java 19 is a discontinued non-LTS release and is unavailable via Homebrew.

**Resolution:**
Upgraded Gradle wrapper to **8.7** (first version series with full Java 21 support):
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-all.zip
```
Adopted Java 21 as the project JVM target (next LTS after Java 17).

**Files Affected:**
- `gradle/wrapper/gradle-wrapper.properties`

---

### Exposed 0.30.x+ requires Kotlin 1.4+; 0.41.x requires 1.6+

**Symptom:**
Build would fail with Kotlin API incompatibilities if versions are mismatched.

**Root Cause:**
The Maven Central Exposed series has minimum Kotlin requirements:
- 0.30.x–0.34.x → Kotlin 1.4+
- 0.35.x–0.39.x → Kotlin 1.5+
- 0.40.x–0.43.x → Kotlin 1.6+

The project was on Kotlin `1.3.+`.

**Resolution:**
Upgraded Kotlin to `1.6.21` and Ktor to `1.6.8` (latest 1.x, designed for Kotlin 1.6). Chose Exposed `0.41.1` as the target (compatible with Kotlin 1.6, available on Maven Central).

**Files Affected:**
- `build.gradle`

---

### Exposed 0.26+ moved `LongIdTable` to a new package

**Symptom:**
```
Unresolved reference: LongIdTable
```

**Root Cause:**
In Exposed 0.26.1, `LongIdTable` (and related `IdTable` classes) moved from `org.jetbrains.exposed.dao` to `org.jetbrains.exposed.dao.id`.

**Resolution:**
- `TagRepository.kt`: Updated import to `org.jetbrains.exposed.dao.id.LongIdTable`.
- `UserRepository.kt`: Switched `Users` to a plain `Table` (avoiding `EntityID<Long>` wrapper complications in JOIN conditions) with an explicit `id: Column<Long> = long("id").autoIncrement()` and `override val primaryKey = PrimaryKey(id)`.

**Files Affected:**
- `src/main/kotlin/io/realworld/app/domain/repository/UserRepository.kt`
- `src/main/kotlin/io/realworld/app/domain/repository/TagRepository.kt`

---

### Exposed 0.37.3+ changed `deleteWhere` lambda receiver

**Symptom:**
```
Unresolved reference: eq
```
inside `deleteWhere { ... }` lambda, even though `eq` works fine in `select { ... }`.

**Root Cause:**
In Exposed 0.37.3, the `deleteWhere` lambda type changed from:
```kotlin
SqlExpressionBuilder.() -> Op<Boolean>   // eq in scope as receiver member
```
to:
```kotlin
T.(ISqlExpressionBuilder) -> Op<Boolean> // eq is a param (it), not receiver
```
`eq` is a member of `ISqlExpressionBuilder`, so it's only in scope when that interface is the receiver, not when it's a parameter.

**Resolution:**
Wrapped the predicate with `with(it) { ... }` to bring `ISqlExpressionBuilder` into scope:
```kotlin
Follows.deleteWhere {
    with(it) {
        (Follows.user eq user.id!!) and (Follows.follower eq userToUnfollow.id!!)
    }
}
```

**Files Affected:**
- `src/main/kotlin/io/realworld/app/domain/repository/UserRepository.kt`

---

### Ktor 1.6 requires `@OptIn(EngineAPI::class)` at call sites

**Symptom:**
```
This is not general purpose API and should be only used in custom server engine implementations.
```
(errors, not warnings, in Kotlin 1.6)

**Root Cause:**
In Kotlin 1.6, `@RequiresOptIn` became stable. Ktor 1.6 annotates `BaseApplicationEngine.start()` and `stop()` with `@EngineAPI` (a `@RequiresOptIn` annotation). Callers must now explicitly opt in; in Kotlin 1.3–1.5 these were only warnings.

**Resolution:**
Added `@OptIn(EngineAPI::class, KtorExperimentalAPI::class)` to each call site:
- `fun main()` in `App.kt`
- `class AppRule` in `AppRule.kt`

**Files Affected:**
- `src/main/kotlin/io/realworld/app/App.kt`
- `src/test/kotlin/io/realworld/app/web/rules/AppRule.kt`

---

### Ktor 1.6 dropped `TimeUnit` overload of `stop()`

**Symptom:**
```
Too many arguments for public abstract fun stop(gracePeriodMillis: Long, timeoutMillis: Long)
```

**Root Cause:**
Ktor 1.2.x had `stop(gracePeriodMillis: Long, timeoutMillis: Long, timeUnit: TimeUnit)`. Ktor 1.6 removed the `TimeUnit` parameter; all durations are now in milliseconds.

**Resolution:**
```kotlin
// Before
app.stop(500, 500, TimeUnit.MILLISECONDS)
// After
app.stop(500L, 500L)
```

**Files Affected:**
- `src/test/kotlin/io/realworld/app/web/rules/AppRule.kt`

---

## Issue Template

Use this format when logging a new issue:

### [Short descriptive title]

**Symptom:**
What went wrong — error messages, unexpected behavior, test failures, etc.

**Root Cause:**
Why it happened — misconfiguration, API mismatch, dependency version, logic bug, etc.

**Resolution:**
What was done to fix it — config change, code edit, dependency update, workaround, etc.

**Files Affected:**
- `path/to/changed/file.kt`

---

_Append new entries above the Issue Template as issues are encountered._
