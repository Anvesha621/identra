package com.springboot.Identra.Config;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {

    private LoginRateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    private AtomicReference<Instant> currentTime;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicReference<>(
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };

        rateLimitFilter = new LoginRateLimitFilter(clock);
        filterChain = mock(FilterChain.class);
    }

    private MockHttpServletRequest createLoginRequest(String ip) {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/auth/login");

        request.setServletPath("/api/auth/login");
        request.setRemoteAddr(ip);

        return request;
    }

    @Test
    void firstFiveRequestsShouldReachFilterChain() throws Exception {

        for (int i = 0; i < 5; i++) {

            MockHttpServletRequest request =
                    createLoginRequest("192.168.1.10");

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            rateLimitFilter.doFilter(request, response, filterChain);

            assertNotEquals(429, response.getStatus());
        }

        verify(filterChain, times(5))
                .doFilter(any(), any());
    }

    @Test
    void sixthRequestShouldReturn429() throws Exception {

        for (int i = 0; i < 6; i++) {

            MockHttpServletRequest request =
                    createLoginRequest("192.168.1.20");

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            rateLimitFilter.doFilter(request, response, filterChain);

            if (i == 5) {
                assertEquals(429, response.getStatus());
                assertTrue(
                        response.getContentAsString()
                                .contains("Too Many Requests")
                );
            }
        }

        verify(filterChain, times(5))
                .doFilter(any(), any());
    }

    @Test
    void differentIpShouldHaveSeparateRateLimit() throws Exception {

        for (int i = 0; i < 5; i++) {

            MockHttpServletRequest request =
                    createLoginRequest("192.168.1.30");

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            rateLimitFilter.doFilter(request, response, filterChain);
        }

        MockHttpServletRequest anotherIpRequest =
                createLoginRequest("192.168.1.31");

        MockHttpServletResponse anotherIpResponse =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                anotherIpRequest,
                anotherIpResponse,
                filterChain
        );

        assertNotEquals(429, anotherIpResponse.getStatus());
    }

    @Test
    void nonLoginEndpointShouldNotBeRateLimited() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/profile");

        request.setRemoteAddr("192.168.1.40");
        request.setServletPath("/api/profile");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        assertNotEquals(429, response.getStatus());

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void requestsShouldBeAllowedAgainAfterWindowExpires() throws Exception {

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request =
                    createLoginRequest("192.168.1.50");

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            rateLimitFilter.doFilter(request, response, filterChain);

            assertNotEquals(429, response.getStatus());
        }

        // Sixth request in the same window should be rejected.
        MockHttpServletResponse limitedResponse =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                createLoginRequest("192.168.1.50"),
                limitedResponse,
                filterChain
        );

        assertEquals(429, limitedResponse.getStatus());

        // Advance time beyond the 60-second window.
        currentTime.set(currentTime.get().plusSeconds(61));

        MockHttpServletResponse resetResponse =
                new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                createLoginRequest("192.168.1.50"),
                resetResponse,
                filterChain
        );

        assertNotEquals(429, resetResponse.getStatus());
    }
}
