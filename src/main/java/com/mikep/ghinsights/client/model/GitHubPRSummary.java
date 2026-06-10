package com.mikep.ghinsights.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

// Returned by the PR list endpoint — does not include additions/deletions.
public record GitHubPRSummary(
        int number,
        String title,
        String state,
        GitHubUser user,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("merged_at") Instant mergedAt,
        @JsonProperty("closed_at") Instant closedAt
) {}
