package com.springboot.Identra.Config;

import com.springboot.Identra.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        filterChain = mock(FilterChain.class);

        filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateDisabledUser() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("testuser");

        UserDetails disabledUser = User.withUsername("testuser")
                .password("encoded-password")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                ))
                .disabled(true)
                .build();

        when(userDetailsService.loadUserByUsername("testuser"))
                .thenReturn(disabledUser);

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Disabled user must not be authenticated"
        );

        verify(filterChain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateEnabledUser() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("testuser");

        UserDetails enabledUser = User.withUsername("testuser")
                .password("encoded-password")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                ))
                .build();

        when(userDetailsService.loadUserByUsername("testuser"))
                .thenReturn(enabledUser);

        filter.doFilter(request, response, filterChain);

        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Enabled user should be authenticated"
        );

        assertEquals(
                "testuser",
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName()
        );

        verify(filterChain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateUserWithInvalidToken() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new io.jsonwebtoken.JwtException("Invalid token"));

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Invalid JWT must not authenticate the user"
        );

        verify(userDetailsService, never())
                .loadUserByUsername(anyString());

        verify(filterChain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAuthenticateUserWithExpiredToken() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Authorization", "Bearer expired-token");

        when(jwtService.extractUsername("expired-token"))
                .thenThrow(new io.jsonwebtoken.ExpiredJwtException(
                        null,
                        null,
                        "Token has expired"
                ));

        filter.doFilter(request, response, filterChain);

        assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Expired JWT must not authenticate the user"
        );

        verify(userDetailsService, never())
                .loadUserByUsername(anyString());

        verify(filterChain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }
}
