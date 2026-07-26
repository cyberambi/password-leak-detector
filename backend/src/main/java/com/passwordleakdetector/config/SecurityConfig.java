package com.passwordleakdetector.config;

import com.passwordleakdetector.security.JwtAuthenticationFilter;
import com.passwordleakdetector.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT API: no server-side session, so classic session-riding CSRF
                // does not apply to the Authorization-header-authenticated endpoints. The only
                // cookie-authenticated endpoints are /auth/refresh and /auth/logout, where the
                // residual CSRF surface is mitigated by SameSite=Strict on the refresh cookie
                // (see AuthController) rather than a separate CSRF token scheme.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/passwords/check-breach",
                                "/api/v1/passwords/analyze-strength",
                                "/api/v1/passwords/generate").permitAll()
                        // Only present when the frontend is bundled into this app's static
                        // resources (the Render deployment build - see Dockerfile.render and
                        // SpaForwardController). The SPA shell itself must load for anonymous
                        // visitors; it's the SPA's own ProtectedRoute (backed by the real
                        // /api/v1/passwords/** auth checks above) that gates the protected
                        // pages client-side.
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/icons.svg",
                                "/register", "/login", "/dashboard", "/history").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(restAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
