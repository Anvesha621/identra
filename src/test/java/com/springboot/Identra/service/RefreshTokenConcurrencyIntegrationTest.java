package com.springboot.Identra.service;

import com.springboot.Identra.entity.RefreshToken;
import com.springboot.Identra.entity.User;
import com.springboot.Identra.exception.InvalidRefreshTokenException;
import com.springboot.Identra.repository.RefreshTokenRepository;
import com.springboot.Identra.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentRefreshRequestsShouldAllowOnlyOneRotation() throws Exception {

        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String username = "concurrency_" + uniqueId;
        String email = "concurrency_" + uniqueId + "@example.com";
        String rawToken = UUID.randomUUID().toString();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("test-password");
        user.setRole("USER");
        user.setEnabled(true);

        User savedUser = userRepository.saveAndFlush(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(rawToken);
        refreshToken.setUser(savedUser);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        refreshTokenRepository.saveAndFlush(refreshToken);

        int threadCount = 2;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<Callable<Void>> tasks = List.of(
                    createRefreshTask(rawToken, ready, start, successes, rejected),
                    createRefreshTask(rawToken, ready, start, successes, rejected)
            );

            List<Future<Void>> futures = tasks.stream()
                    .map(executor::submit)
                    .toList();

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both requests should become ready"
            );

            start.countDown();

            for (Future<Void> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }

            assertEquals(1, successes.get(),
                    "Exactly one concurrent refresh should succeed");

            assertEquals(1, rejected.get(),
                    "The other refresh should be rejected");

            TransactionTemplate transactionTemplate =
                    new TransactionTemplate(transactionManager);

            Boolean oldTokenRevoked = transactionTemplate.execute(status ->
                    refreshTokenRepository.findByToken(rawToken)
                            .map(RefreshToken::isRevoked)
                            .orElse(false)
            );

            assertEquals(Boolean.TRUE, oldTokenRevoked,
                    "The original refresh token should be revoked");

        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            TransactionTemplate transactionTemplate =
                    new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {
                refreshTokenRepository.deleteByUserId(savedUser.getId());
                userRepository.delete(savedUser);
            });
        }
    }

    private Callable<Void> createRefreshTask(
            String token,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successes,
            AtomicInteger rejected) {

        return () -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start");
            }

            try {
                refreshTokenService.rotateRefreshToken(token);
                successes.incrementAndGet();
            } catch (InvalidRefreshTokenException ex) {
                rejected.incrementAndGet();
            }

            return null;
        };
    }
}