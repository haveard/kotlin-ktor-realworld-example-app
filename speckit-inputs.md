# Spec Kit inputs

Paste-ready arguments for the Spec Kit prompts, written against the verified
baseline in `AGENTS.md` / `NEXT_STEPS.md` (User + Tag layers work; Article /
Comment / Profile are stubbed; all controller tests are `@Ignore`d).

---

## `/speckit.specify` — Popular articles feed

> Add a **Popular articles feed** to the RealWorld API: an authenticated
> `GET /api/articles/feed/popular` endpoint that returns articles ranked by how
> many times they have been favorited (most-favorited first), with `limit` /
> `offset` pagination, in the standard RealWorld multiple-articles response
> shape (`{ "articles": [...], "articlesCount": N }`).
>
> **Baseline constraint (in scope):** this repo is a scaffold, not a working
> API. Only the User and Tag layers are implemented. `ArticleController` (and
> Comment/Profile) are stubs — their service-injected constructors are
> commented out and handlers return placeholder DTOs — and there are no
> `ArticleRepository`, `ArticleService`, `CommentRepository`, or
> `CommentService` classes. All five controller test classes are `@Ignore`d.
> This feature therefore depends on first building the **article + favorites
> persistence and service layer** needed to support it: an `Articles` table, an
> `ArticleFavorites` join table, and working **create-article** and
> **favorite/unfavorite-article** behavior (required both to serve the ranking
> and to produce test data), following the existing `UserRepository` /
> `UserService` Exposed pattern. Treat that groundwork as part of this feature.
>
> **User stories (prioritized):**
> - **P1 — Discover most-favorited articles.** As an authenticated reader, I GET
>   the popular feed and receive articles ordered by favorites count descending.
> - **P2 — Page through the feed.** As a client, I use `limit` and `offset` to
>   page through results.
> - **P3 — Supporting data operations.** As a user, I can create an article and
>   favorite/unfavorite an article, so the feed has data to rank. (Needed to
>   build and test P1.)
>
> **Acceptance scenarios:**
> 1. Given several articles favorited different numbers of times, when I GET the
>    popular feed with a valid token, then I receive 200 and articles sorted by
>    `favoritesCount` descending.
> 2. Given no `Authorization` header, when I GET the popular feed, then I receive
>    401.
> 3. Given no articles exist, when I GET the popular feed, then I receive 200
>    with an empty `articles` array and `articlesCount` 0 (not a 404).
> 4. Given a non-numeric pagination param (e.g. `?limit=abc`), when I GET the
>    popular feed, then the API responds consistently with existing param
>    handling — mirror whatever `ArticleController.findBy` does (422 or a safe
>    default).
> 5. Given `limit` and `offset`, when I page, then results respect the window and
>    ordering stays stable.
>
> **Out of scope:** comments, profile follow, article update/delete beyond what
> the feed needs, and un-`@Ignore`ing unrelated test suites.
>
> **Success criteria:** popular feed returns correct favorite-count ordering;
> endpoint requires auth; pagination works; new integration tests (happy path +
> 401 + invalid-param) pass while pre-existing `@Ignore`d suites stay skipped.
