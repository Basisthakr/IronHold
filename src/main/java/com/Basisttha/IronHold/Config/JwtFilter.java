package com.Basisttha.IronHold.Config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.Basisttha.IronHold.Repository.RevokedTokenRepository;
import com.Basisttha.IronHold.Repository.UserRepository;
import com.Basisttha.IronHold.Service.JwtService;

import io.jsonwebtoken.lang.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final RevokedTokenRepository revokedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws IOException, ServletException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Reject if token is invalid
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Reject if token has been revoked (logged out)
        if (revokedTokenRepository.existsByToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token has been revoked");
            return;
        }

        // Set authentication in context if not already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String userId = jwtService.extractUserId(token);
            if (userId != null) {
                userRepo.findById(UUID.fromString(userId)).ifPresent(user -> {

                    // reject tokens issued before key rotation
                    if (user.getKeyRotatedAt() != null) {
                        Date issuedAt = jwtService.extractIssuedAt(token);
                        LocalDateTime tokenIssuedAt = issuedAt.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                        if (tokenIssuedAt.isBefore(user.getKeyRotatedAt())) {
                            return; // token predates key rotation, reject silently
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken
                            = new UsernamePasswordAuthenticationToken(
                                    user, null, Collections.emptyList());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
