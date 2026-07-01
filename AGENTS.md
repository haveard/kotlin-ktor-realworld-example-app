# AGENTS.md

This file describes the conventions and documentation structure used by AI agents (e.g., GitHub Copilot) working in this repository.

## Repository Overview

A Kotlin + Ktor scaffold of the [RealWorld](https://github.com/gothinkster/realworld) spec. The routing, auth, and DTO layers are in place, but it is a **partial implementation**, not a finished API (see Baseline State below). Stack:

- **Kotlin** — primary language
- **Ktor** — web framework
- **Kodein** — dependency injection
- **Exposed** — SQL ORM
- **H2** — embedded database
- **JUnit + Unirest** — testing

## Baseline State (verified against source)

Do not assume the article/comment APIs work — they do not yet.

- **Implemented:** `User` and `Tag` layers (repository + service + controller) with JWT auth. These are the reference pattern to copy.
- **Stubbed:** `ArticleController`, `CommentController`, and `ProfileController` have their real service-injected constructors commented out; handler methods return placeholder DTOs (e.g. `ArticlesDTO(listOf(), 1)`). There are **no** `ArticleRepository`, `ArticleService`, `CommentRepository`, or `CommentService` classes.
- **Tests:** all five controller test classes are annotated `@Ignore`, so `./gradlew test` compiles and *skips* them — "green" does not mean the article/comment endpoints are covered.
- **Implication:** any feature touching articles, comments, or profiles must first build the missing persistence + service layer, following the `UserRepository`/`UserService` pattern.

## Agent Documentation Standards

When working in this repository across a session, agents **must** maintain the following documentation files at the repo root:

| File | Purpose |
|------|---------|
| [`PROCESS.md`](./PROCESS.md) | Step-by-step log of actions taken during the session |
| [`LESSONS_LEARNED.md`](./LESSONS_LEARNED.md) | Issues encountered and their resolutions |
| [`NEXT_STEPS.md`](./NEXT_STEPS.md) | Handoff doc summarizing outstanding work for future sessions |

### PROCESS.md

- Record each meaningful step as it happens, in chronological order.
- Include: what was done, which files were changed, and why.
- Use a numbered list or timestamped entries.

### LESSONS_LEARNED.md

- Document every non-trivial issue encountered (build failures, config issues, logic bugs, test flakiness, etc.).
- For each issue: describe the symptom, root cause, and the fix applied.
- This helps future agents avoid repeating the same mistakes.

### NEXT_STEPS.md

- Summarize the current state of the codebase/feature at session end.
- List all remaining work items with enough context to resume without re-investigation.
- Note any known risks, blockers, or decisions that still need to be made.

## Build & Test Commands

```bash
# Build
./gradlew clean build

# Run the server (port 8080)
./gradlew run

# Run API integration tests (server must be running)
APIURL=http://localhost:8080 ./spec-api/run-api-tests.sh
```

## Code Structure

```
src/main/kotlin/io/realworld/app/
├── App.kt                  # Entry point
├── config/                 # Ktor, Kodein, DB setup
├── domain/
│   ├── repository/         # Persistence layer (Exposed tables)
│   └── service/            # Business logic
├── ext/                    # Kotlin extensions (e.g., email validation)
├── utils/                  # JWT, encryption
└── web/
    ├── controllers/        # Route handlers
    ├── Router.kt           # Route definitions
    └── ErrorExceptionMapping.kt
```
