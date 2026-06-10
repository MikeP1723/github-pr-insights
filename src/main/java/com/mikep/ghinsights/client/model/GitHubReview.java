package com.mikep.ghinsights.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GitHubReview(
        long id,
        GitHubUser user,
        String state,
        @JsonProperty("submitted_at") Instant submittedAt
) {}
