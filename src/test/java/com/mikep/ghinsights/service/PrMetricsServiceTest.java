package com.mikep.ghinsights.service;

import com.mikep.ghinsights.client.GitHubClient;
import com.mikep.ghinsights.client.model.GitHubPRSummary;
import com.mikep.ghinsights.client.model.GitHubReview;
import com.mikep.ghinsights.client.model.GitHubUser;
import com.mikep.ghinsights.model.RepoMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrMetricsServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @InjectMocks
    private PrMetricsService service;

    @Test
    void calculateMetrics_countsMergedAndClosedCorrectly() {
        Instant now = Instant.now();
        GitHubUser user = new GitHubUser("alice");

        List<GitHubPRSummary> prs = List.of(
                pr(1, user, now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS)),
                pr(2, user, now.minus(3, ChronoUnit.DAYS), null)
        );

        when(gitHubClient.getPullRequests("owner", "repo")).thenReturn(prs);
        when(gitHubClient.getReviews("owner", "repo", 1)).thenReturn(List.of());

        RepoMetrics metrics = service.calculateMetrics("owner", "repo");

        assertThat(metrics.summary().merged()).isEqualTo(1);
        assertThat(metrics.summary().closedWithoutMerge()).isEqualTo(1);
        assertThat(metrics.analyzedPrs()).isEqualTo(2);
    }

    @Test
    void calculateMetrics_computesTimeToFirstReview() {
        Instant now = Instant.now();
        Instant created = now.minus(10, ChronoUnit.HOURS);
        Instant merged = now.minus(1, ChronoUnit.HOURS);
        Instant reviewed = now.minus(8, ChronoUnit.HOURS); // 2 hours after creation

        GitHubUser user = new GitHubUser("bob");
        GitHubPRSummary pr = pr(1, user, created, merged);
        GitHubReview review = new GitHubReview(1L, user, "APPROVED", reviewed);

        when(gitHubClient.getPullRequests("owner", "repo")).thenReturn(List.of(pr));
        when(gitHubClient.getReviews("owner", "repo", 1)).thenReturn(List.of(review));

        RepoMetrics metrics = service.calculateMetrics("owner", "repo");

        assertThat(metrics.timeToFirstReview().average()).isEqualTo("2h 0m");
    }

    @Test
    void calculateMetrics_ignoresReviewsSubmittedBeforePrCreation() {
        Instant now = Instant.now();
        Instant created = now.minus(5, ChronoUnit.HOURS);
        Instant merged = now;
        Instant reviewedBeforeCreation = now.minus(10, ChronoUnit.HOURS);

        GitHubUser user = new GitHubUser("carol");
        GitHubPRSummary pr = pr(1, user, created, merged);
        GitHubReview staleReview = new GitHubReview(1L, user, "APPROVED", reviewedBeforeCreation);

        when(gitHubClient.getPullRequests("owner", "repo")).thenReturn(List.of(pr));
        when(gitHubClient.getReviews("owner", "repo", 1)).thenReturn(List.of(staleReview));

        RepoMetrics metrics = service.calculateMetrics("owner", "repo");

        assertThat(metrics.timeToFirstReview().average()).isEqualTo("N/A");
    }

    @Test
    void calculateMetrics_handlesRepoWithNoMergedPrs() {
        GitHubUser user = new GitHubUser("dave");
        Instant now = Instant.now();

        when(gitHubClient.getPullRequests("owner", "repo")).thenReturn(
                List.of(pr(1, user, now.minus(1, ChronoUnit.DAYS), null))
        );

        RepoMetrics metrics = service.calculateMetrics("owner", "repo");

        assertThat(metrics.timeToMerge().average()).isEqualTo("N/A");
        assertThat(metrics.timeToFirstReview().average()).isEqualTo("N/A");
    }

    @Test
    void calculateMetrics_computesP90TimeToMerge() {
        Instant now = Instant.now();
        GitHubUser user = new GitHubUser("eve");

        // 10 PRs with merge times 1h through 10h — p90 should be ~9h
        List<GitHubPRSummary> prs = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Instant created = now.minus(i + 1, ChronoUnit.HOURS);
            Instant merged = now.minus(1, ChronoUnit.HOURS);
            prs.add(pr(i, user, created, merged));
            when(gitHubClient.getReviews("owner", "repo", i)).thenReturn(List.of());
        }

        when(gitHubClient.getPullRequests("owner", "repo")).thenReturn(prs);

        RepoMetrics metrics = service.calculateMetrics("owner", "repo");

        assertThat(metrics.timeToMerge().p90()).isNotEqualTo("N/A");
        assertThat(metrics.summary().merged()).isEqualTo(10);
    }

    private GitHubPRSummary pr(int number, GitHubUser user, Instant createdAt, Instant mergedAt) {
        return new GitHubPRSummary(number, "PR #" + number, "closed", user, createdAt, mergedAt, mergedAt);
    }
}
