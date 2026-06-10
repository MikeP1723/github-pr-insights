# github-pr-insights

A Spring Boot REST API that surfaces pull request metrics for any public GitHub repository — review turnaround times, merge velocity, contributor productivity, and PR size distribution.

## Endpoints

### `GET /api/repos/{owner}/{repo}/metrics`
Aggregate PR metrics using the most recently closed pull requests.

```json
{
  "repository": "mikep1723/github-pr-insights",
  "analyzedPrs": 245,
  "summary": {
    "merged": 220,
    "closedWithoutMerge": 25
  },
  "timeToFirstReview": {
    "average": "4h 22m",
    "median": "2h 15m",
    "p90": "18h 30m"
  },
  "timeToMerge": {
    "average": "1d 6h 12m",
    "median": "18h 45m",
    "p90": "4d 2h 0m"
  }
}
```

### `GET /api/repos/{owner}/{repo}/contributors`
Per-contributor PR count, merge rate, and average merge time, ranked by activity.

```json
[
  { "login": "alice", "prCount": 42, "mergedCount": 40, "avgTimeToMerge": "1d 2h 0m" },
  { "login": "bob",   "prCount": 28, "mergedCount": 25, "avgTimeToMerge": "18h 30m" }
]
```

### `GET /api/repos/{owner}/{repo}/size-distribution`
PR size breakdown across a sample of the most recent merged PRs. Kept as a separate endpoint because it requires individual PR fetches (the list endpoint does not include line counts).

```json
{
  "repository": "owner/repo",
  "sampledPrs": 50,
  "xs": 10,
  "s": 18,
  "m": 12,
  "l": 7,
  "xl": 3
}
```

Size buckets follow GitHub's own label convention:

| Label | Lines changed |
|-------|--------------|
| XS    | < 10         |
| S     | 10 – 49      |
| M     | 50 – 149     |
| L     | 150 – 499    |
| XL    | 500+         |

## Running locally

**1. Set your GitHub token** (optional, but avoids unauthenticated rate limits of 60 req/hr):
```bash
export GITHUB_TOKEN=your_token_here
```

**2. Build and run:**
```bash
./gradlew bootRun
```

**3. Query a repo:**
```bash
curl http://localhost:8080/api/repos/spring-projects/spring-boot/metrics
curl http://localhost:8080/api/repos/spring-projects/spring-boot/contributors
curl http://localhost:8080/api/repos/spring-projects/spring-boot/size-distribution
```

## Running tests
```bash
./gradlew test
```

## Design notes

- **Caching** — GitHub API responses are cached for 10 minutes using Caffeine. A warm cache means repeated calls to the same repo are instant and don't consume rate limit quota.
- **Pagination** — the PR list endpoint is paginated up to 500 results (5 pages × 100). The size distribution endpoint samples the most recent 50 merged PRs to limit individual PR fetches.
- **Time to first review** — counts only reviews submitted *after* the PR was created, which filters out reviews carried over from force-pushed refs.
- **N+1 on size distribution** — this is intentional and documented. The list endpoint doesn't include line counts; fetching them requires individual PR calls. The 50-PR sample keeps this bounded. A production version would offload this to a background job and store the results.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Cache + Caffeine
- GitHub REST API v3
- JUnit 5 + Mockito + AssertJ
