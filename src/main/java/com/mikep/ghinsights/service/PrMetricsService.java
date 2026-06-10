package com.mikep.ghinsights.service;

import com.mikep.ghinsights.client.GitHubClient;
import com.mikep.ghinsights.client.model.GitHubPRDetail;
import com.mikep.ghinsights.client.model.GitHubPRSummary;
import com.mikep.ghinsights.client.model.GitHubReview;
import com.mikep.ghinsights.model.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrMetricsService {

    // Limit for individual PR fetches to stay within unauthenticated rate limits.
    private static final int SIZE_SAMPLE_LIMIT = 50;

    private final GitHubClient gitHubClient;

    public PrMetricsService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    public RepoMetrics calculateMetrics(String owner, String repo) {
        List<GitHubPRSummary> prs = gitHubClient.getPullRequests(owner, repo);

        List<GitHubPRSummary> merged = prs.stream()
                .filter(pr -> pr.mergedAt() != null)
                .toList();

        int closedWithoutMerge = prs.size() - merged.size();

        List<Duration> timeToMerges = merged.stream()
                .map(pr -> Duration.between(pr.createdAt(), pr.mergedAt()))
                .toList();

        List<Duration> timeToFirstReviews = merged.stream()
                .map(pr -> firstReviewDelay(owner, repo, pr))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return new RepoMetrics(
                owner + "/" + repo,
                prs.size(),
                new PrSummary(merged.size(), closedWithoutMerge),
                computeStats(timeToFirstReviews),
                computeStats(timeToMerges)
        );
    }

    public List<ContributorStats> getContributorStats(String owner, String repo) {
        List<GitHubPRSummary> prs = gitHubClient.getPullRequests(owner, repo);

        Map<String, List<GitHubPRSummary>> byAuthor = prs.stream()
                .collect(Collectors.groupingBy(pr -> pr.user().login()));

        return byAuthor.entrySet().stream()
                .map(entry -> {
                    List<GitHubPRSummary> authorPrs = entry.getValue();
                    List<GitHubPRSummary> authorMerged = authorPrs.stream()
                            .filter(pr -> pr.mergedAt() != null)
                            .toList();

                    List<Duration> mergeTimes = authorMerged.stream()
                            .map(pr -> Duration.between(pr.createdAt(), pr.mergedAt()))
                            .toList();

                    return new ContributorStats(
                            entry.getKey(),
                            authorPrs.size(),
                            authorMerged.size(),
                            mergeTimes.isEmpty() ? "N/A" : formatDuration(median(mergeTimes))
                    );
                })
                .sorted(Comparator.comparingInt(ContributorStats::prCount).reversed())
                .toList();
    }

    public SizeDistribution getSizeDistribution(String owner, String repo) {
        List<GitHubPRSummary> prs = gitHubClient.getPullRequests(owner, repo);

        List<GitHubPRSummary> sample = prs.stream()
                .filter(pr -> pr.mergedAt() != null)
                .limit(SIZE_SAMPLE_LIMIT)
                .toList();

        int xs = 0, s = 0, m = 0, l = 0, xl = 0;
        for (GitHubPRSummary summary : sample) {
            GitHubPRDetail detail = gitHubClient.getPRDetail(owner, repo, summary.number());
            int lines = detail.additions() + detail.deletions();
            if (lines < 10) xs++;
            else if (lines < 50) s++;
            else if (lines < 150) m++;
            else if (lines < 500) l++;
            else xl++;
        }

        return new SizeDistribution(owner + "/" + repo, sample.size(), xs, s, m, l, xl);
    }

    private Optional<Duration> firstReviewDelay(String owner, String repo, GitHubPRSummary pr) {
        return gitHubClient.getReviews(owner, repo, pr.number()).stream()
                .filter(r -> r.submittedAt() != null && r.submittedAt().isAfter(pr.createdAt()))
                .min(Comparator.comparing(GitHubReview::submittedAt))
                .map(r -> Duration.between(pr.createdAt(), r.submittedAt()));
    }

    private DurationStats computeStats(List<Duration> durations) {
        if (durations.isEmpty()) {
            return new DurationStats("N/A", "N/A", "N/A");
        }
        List<Duration> sorted = durations.stream().sorted().toList();
        return new DurationStats(
                formatDuration(average(sorted)),
                formatDuration(median(sorted)),
                formatDuration(percentile(sorted, 90))
        );
    }

    private Duration average(List<Duration> durations) {
        long totalSeconds = durations.stream().mapToLong(Duration::getSeconds).sum();
        return Duration.ofSeconds(totalSeconds / durations.size());
    }

    private Duration median(List<Duration> sorted) {
        return sorted.get(sorted.size() / 2);
    }

    private Duration percentile(List<Duration> sorted, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private String formatDuration(Duration d) {
        long days = d.toDaysPart();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);
        if (hours > 0) return String.format("%dh %dm", hours, minutes);
        return String.format("%dm", minutes);
    }
}
