package com.mikep.ghinsights.client;

import com.mikep.ghinsights.client.model.GitHubPRDetail;
import com.mikep.ghinsights.client.model.GitHubPRSummary;
import com.mikep.ghinsights.client.model.GitHubReview;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubClient {

    private static final String BASE_URL = "https://api.github.com";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 5;

    private final RestClient restClient;

    public GitHubClient(@Value("${github.token:}") String token) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (!token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        this.restClient = builder.build();
    }

    // Fetches up to MAX_PAGES * PAGE_SIZE closed PRs using the list endpoint.
    // The list endpoint does not include additions/deletions — use getPRDetail for those.
    @Cacheable("pullRequests")
    public List<GitHubPRSummary> getPullRequests(String owner, String repo) {
        List<GitHubPRSummary> all = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            List<GitHubPRSummary> results = restClient.get()
                    .uri("/repos/{owner}/{repo}/pulls?state=closed&per_page={size}&page={page}",
                            owner, repo, PAGE_SIZE, page)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (results == null || results.isEmpty()) break;
            all.addAll(results);
            if (results.size() < PAGE_SIZE) break;
        }

        return all;
    }

    // Fetches a single PR with full detail (additions, deletions, changed_files).
    // Used only for size distribution to avoid N+1 on the full PR list.
    @Cacheable("prDetails")
    public GitHubPRDetail getPRDetail(String owner, String repo, int prNumber) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}", owner, repo, prNumber)
                .retrieve()
                .body(GitHubPRDetail.class);
    }

    @Cacheable("reviews")
    public List<GitHubReview> getReviews(String owner, String repo, int prNumber) {
        List<GitHubReview> reviews = restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{number}/reviews", owner, repo, prNumber)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return reviews != null ? reviews : List.of();
    }
}
