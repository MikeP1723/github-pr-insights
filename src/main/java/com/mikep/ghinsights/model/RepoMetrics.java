package com.mikep.ghinsights.model;

public record RepoMetrics(
        String repository,
        int analyzedPrs,
        PrSummary summary,
        DurationStats timeToFirstReview,
        DurationStats timeToMerge
) {}
