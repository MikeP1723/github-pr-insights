package com.mikep.ghinsights.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

// Returned by the individual PR endpoint — includes additions/deletions for size analysis.
public record GitHubPRDetail(
        int number,
        String title,
        String state,
        GitHubUser user,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("merged_at") Instant mergedAt,
        int additions,
        int deletions,
        @JsonProperty("changed_files") int changedFiles
) {}
