package com.mikep.ghinsights.model;

// PR size buckets follow GitHub's own label convention.
// XS: <10 lines  S: 10-49  M: 50-149  L: 150-499  XL: 500+
public record SizeDistribution(
        String repository,
        int sampledPrs,
        int xs,
        int s,
        int m,
        int l,
        int xl
) {}
