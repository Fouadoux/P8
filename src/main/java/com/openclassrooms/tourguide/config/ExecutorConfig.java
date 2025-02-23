package com.openclassrooms.tourguide.config;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ExecutorConfig} class is a Spring configuration class responsible for managing
 * an {@link ExecutorService} bean. It provides a thread pool for executing asynchronous tasks.
 */
@Configuration
public class ExecutorConfig {

    /** The thread pool executor service. */
    private final ExecutorService executorService;

    /**
     * Constructs an {@code ExecutorConfig} with a fixed thread pool size.
     * The pool size can be configured via the {@code executor.pool.size} property.
     *
     * @param //poolSize the number of threads in the pool, defaults to 4 if not specified.
     */
    public ExecutorConfig() {
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Provides the {@link ExecutorService} bean for managing asynchronous tasks.
     *
     * @return the configured {@code ExecutorService} instance.
     */
    @Bean
    public ExecutorService executorService() {
        return executorService;
    }

    /**
     * Gracefully shuts down the {@code ExecutorService} when the application is stopping.
     * It first attempts an orderly shutdown and waits for tasks to complete.
     * If tasks do not terminate within 60 seconds, a forced shutdown is triggered.
     */
    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
