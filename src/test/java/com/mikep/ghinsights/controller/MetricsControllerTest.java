package com.mikep.ghinsights.controller;

import com.mikep.ghinsights.model.*;
import com.mikep.ghinsights.service.PrMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsController.class)
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrMetricsService metricsService;

    @Test
    void getMetrics_returns200WithExpectedFields() throws Exception {
        RepoMetrics metrics = new RepoMetrics(
                "owner/repo", 100,
                new PrSummary(90, 10),
                new DurationStats("4h 30m", "2h 15m", "18h 0m"),
                new DurationStats("1d 6h 0m", "18h 0m", "4d 0h 0m")
        );

        when(metricsService.calculateMetrics("owner", "repo")).thenReturn(metrics);

        mockMvc.perform(get("/api/repos/owner/repo/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repository").value("owner/repo"))
                .andExpect(jsonPath("$.analyzedPrs").value(100))
                .andExpect(jsonPath("$.summary.merged").value(90))
                .andExpect(jsonPath("$.timeToFirstReview.average").value("4h 30m"))
                .andExpect(jsonPath("$.timeToMerge.p90").value("4d 0h 0m"));
    }

    @Test
    void getContributors_returnsRankedList() throws Exception {
        List<ContributorStats> stats = List.of(
                new ContributorStats("alice", 15, 14, "1d 2h 0m"),
                new ContributorStats("bob", 8, 7, "18h 0m")
        );

        when(metricsService.getContributorStats("owner", "repo")).thenReturn(stats);

        mockMvc.perform(get("/api/repos/owner/repo/contributors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("alice"))
                .andExpect(jsonPath("$[0].prCount").value(15))
                .andExpect(jsonPath("$[1].login").value("bob"));
    }

    @Test
    void getSizeDistribution_returnsDistributionWithSampleCount() throws Exception {
        SizeDistribution dist = new SizeDistribution("owner/repo", 50, 10, 20, 12, 5, 3);

        when(metricsService.getSizeDistribution("owner", "repo")).thenReturn(dist);

        mockMvc.perform(get("/api/repos/owner/repo/size-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampledPrs").value(50))
                .andExpect(jsonPath("$.s").value(20))
                .andExpect(jsonPath("$.xl").value(3));
    }
}
