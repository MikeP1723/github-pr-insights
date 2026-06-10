package com.mikep.ghinsights.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    public ProblemDetail handleNotFound(HttpClientErrorException.NotFound ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "Repository not found on GitHub");
    }

    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public ProblemDetail handleForbidden(HttpClientErrorException.Forbidden ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "GitHub API rate limit exceeded or invalid token. Set GITHUB_TOKEN to increase limits.");
    }
}
