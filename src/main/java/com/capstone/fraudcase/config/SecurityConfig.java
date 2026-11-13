package com.capstone.fraudcase.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Two chains: a stateless HTTP Basic API (no cookies, so CSRF tokens are not applicable) and a session-based UI with
 * form login and CSRF protection. Role-based authorisation guards every case endpoint.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String[] INTAKE = {"AGENT", "INVESTIGATOR", "LEAD"};
    private static final String[] INVESTIGATE = {"INVESTIGATOR", "LEAD"};
    private static final String[] FOLLOW_UP = {"INVESTIGATOR", "REVIEWER", "LEAD"};
    private static final String[] REVIEW = {"REVIEWER", "LEAD"};
    private static final String[] STAFF = {"AGENT", "INVESTIGATOR", "REVIEWER", "LEAD", "AUDITOR"};

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/cases").hasAnyRole(INTAKE)
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/investigations").hasAnyRole(INVESTIGATE)
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/messages").hasAnyRole(FOLLOW_UP)
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/reviews").hasAnyRole(REVIEW)
                        .requestMatchers(HttpMethod.GET, "/api/cases/*/audit").hasAnyRole("AUDITOR", "LEAD", "REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(STAFF)
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain uiChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/css/**", "/js/**", "/login", "/error", "/actuator/health/**", "/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("LEAD")
                        .requestMatchers(HttpMethod.POST, "/ui/cases").hasAnyRole(INTAKE)
                        .requestMatchers(HttpMethod.POST, "/ui/cases/*/investigate").hasAnyRole(INVESTIGATE)
                        .requestMatchers(HttpMethod.POST, "/ui/cases/*/conversation").hasAnyRole(FOLLOW_UP)
                        .requestMatchers(HttpMethod.POST, "/ui/cases/*/review", "/ui/cases/*/review/start").hasAnyRole(REVIEW)
                        .requestMatchers("/ui/cases/*/audit").hasAnyRole("AUDITOR", "LEAD", "REVIEWER")
                        .anyRequest().hasAnyRole(STAFF))
                .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/ui/cases", true).permitAll())
                .logout(l -> l.logoutSuccessUrl("/login?logout"))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; form-action 'self'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(CapstoneProperties props, PasswordEncoder encoder) {
        String password = props.getSecurity().getDemoPassword();
        if (password == null || password.isBlank()) {
            byte[] bytes = new byte[12];
            new SecureRandom().nextBytes(bytes);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            log.warn("No CAPSTONE_DEMO_PASSWORD set. Generated training password for synthetic users: {}", password);
        }
        String encoded = encoder.encode(password);
        List<CapstoneProperties.DemoUser> configured = props.getSecurity().getUsers();
        List<UserDetails> users = configured.stream()
                .map(u -> User.withUsername(u.getUsername()).password(encoded).roles(u.getRoles().toArray(String[]::new)).build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }
}
