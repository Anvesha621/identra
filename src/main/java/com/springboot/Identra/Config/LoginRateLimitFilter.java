package com.springboot.Identra.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, RequestCounter> requestCounts =
            new ConcurrentHashMap<>();

    private final Clock clock;

    public LoginRateLimitFilter() {
        this(Clock.systemUTC());
    }

    LoginRateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        long currentTime = clock.instant().getEpochSecond();

        RequestCounter counter = requestCounts.compute(clientIp, (ip, existing) -> {

            if (existing == null
                    || currentTime - existing.windowStart >= WINDOW_SECONDS) {
                return new RequestCounter(currentTime, 1);
            }

            existing.count++;
            return existing;
        });

        if (counter.count > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Too many login requests. Please try again later."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestCounter {

        private final long windowStart;
        private int count;

        private RequestCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanupExpiredCounters() {

        long currentTime = clock.instant().getEpochSecond();

        Iterator<Map.Entry<String, RequestCounter>> iterator =
                requestCounts.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, RequestCounter> entry = iterator.next();

            RequestCounter counter = entry.getValue();

            if (currentTime - counter.windowStart >= WINDOW_SECONDS) {
                requestCounts.remove(entry.getKey(), counter);
            }
        }
    }
}
