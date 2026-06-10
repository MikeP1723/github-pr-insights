package com.mikep.ghinsights.controller;

import com.mikep.ghinsights.model.ContributorStats;
import com.mikep.ghinsights.model.RepoMetrics;
import com.mikep.ghinsights.model.SizeDistribution;
import com.mikep.ghinsights.service.PrMetricsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos/{owner}/{repo}")
public class MetricsController {

    private final PrMetricsService metricsService;

    public MetricsController(PrMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public RepoMetrics getMetrics(
            @PathVariable String owner,
            @PathVariable String repo) {
        return metricsService.calculateMetrics(owner, repo);
    }

    @GetMapping("/contributors")
    public List<ContributorStats> getContributors(
            @PathVariable String owner,
            @PathVariable String repo) {
        return metricsService.getContributorStats(owner, repo);
    }

    // Separate endpoint: fetches individual PR details to get line counts.
    // Limited to the last 50 merged PRs to stay within GitHub API rate limits.
    @GetMapping("/size-distribution")
    public SizeDistribution getSizeDistribution(
            @PathVariable String owner,
            @PathVariable String repo) {
        return metricsService.getSizeDistribution(owner, repo);
    }
}
