package com.mikep.ghinsights.model;

public record ContributorStats(
        String login,
        int prCount,
        int mergedCount,
        String avgTimeToMerge
) {}
