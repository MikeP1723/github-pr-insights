package com.mikep.ghinsights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GithubPrInsightsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubPrInsightsApplication.class, args);
    }
}
