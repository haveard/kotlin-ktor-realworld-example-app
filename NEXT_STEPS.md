# NEXT_STEPS.md

Handoff document for future sessions. Describes the current state of the codebase and all outstanding work.

---

## Session Ended: 2026-07-01

### Current State

The repository is a **working, building** Kotlin + Ktor backend implementing the [RealWorld spec](https://github.com/gothinkster/realworld). The build is green (`./gradlew clean build` passes) after a dependency and toolchain modernization performed this session.

**Current versions:**
| Component | Version |
|-----------|---------|
| Java | 21 (Amazon Corretto LTS) |
| Gradle | 8.7 |
| Kotlin | 1.6.21 |
| Ktor | 1.6.8 (latest 1.x) |
| Exposed | 0.41.1 (modular) |
| Kodein | 6.1.0 |
| H2 | 2.2.224 |
| HikariCP | 5.1.0 |

### What Was Done This Session

1. Created agent documentation scaffold (`AGENTS.md`, `PROCESS.md`, `LESSONS_LEARNED.md`, `NEXT_STEPS.md`)
2. Modernized the build toolchain (Gradle, Java, Kotlin, Ktor, Exposed)
3. Migrated Exposed API usage to the modular 0.41.x API
4. Fixed Ktor 1.6 opt-in annotation requirements

See `PROCESS.md` for full step-by-step details and `LESSONS_LEARNED.md` for root-cause analysis of each issue.

### Outstanding Work / Suggested Next Steps

- [ ] **Run the RealWorld API spec tests** to verify runtime behavior:
  ```bash
  ./gradlew run &
  APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh
  ```
- [ ] **Article, Comment, and Profile CRUD is stubbed** — the controllers exist but the domain services return placeholder data (no `ArticleRepository` or `CommentRepository` exists). This is the main feature gap.
- [ ] **Ktor 2.x migration** — Ktor 1.6.x is end-of-life. Ktor 2.x is a significant rewrite (plugins vs features, new module structure). This is non-trivial work.
- [ ] **Clean up deprecated warnings**:
  - Remove `@KtorExperimentalAPI` annotations (no longer needed in Ktor 1.6)
  - Address `Cipher.sign()` Java deprecation
- [ ] **Kodein 7.x migration** — Kodein was renamed/restructured in v7; the current code uses the old `org.kodein.di.generic.instance` import

### Known Risks / Open Decisions

- **H2 is in-memory only** — all data is lost on restart; keeps tests hermetic but not suitable for production.
- **JWT secret is hardcoded** in `JwtProvider.kt` — would need secrets management before any production use.
- **Article/Comment/Profile functionality is incomplete** — the RealWorld spec compliance is partial; spec tests will likely fail for those endpoints.
- **Gradle 9.0 deprecation warnings** — the build uses some features deprecated in Gradle 8 that will break on Gradle 9. Not urgent, but worth addressing before upgrading Gradle further.

### How to Resume

1. Clone/pull `haveard/kotlin-ktor-realworld-example-app`
2. Confirm baseline: `./gradlew clean build` → should be `BUILD SUCCESSFUL`
3. Read `PROCESS.md` for session history
4. Read `LESSONS_LEARNED.md` for known pitfalls before touching dependencies
5. Pick up from the outstanding work list above
