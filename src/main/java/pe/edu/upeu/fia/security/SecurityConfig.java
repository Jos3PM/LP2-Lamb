package pe.edu.upeu.fia.security;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
public class SecurityConfig {
    @Bean @ConditionalOnProperty(name="app.security.mode", havingValue="keycloak")
    JwtDecoder keycloakDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains("fia-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "La audiencia del token no incluye fia-api", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean InMemoryUserDetailsManager adminUser(
            PasswordEncoder encoder,
            @Value("${app.security.admin.username:admin}") String username,
            @Value("${app.security.admin.password}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(encoder.encode(password)).roles("ADMIN").build());
    }

    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.enabled:true}") boolean enabled,
            @Value("${app.security.mode:local}") String mode) throws Exception {
        if (enabled && !"keycloak".equalsIgnoreCase(mode)) {
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        } else {
            http.csrf(csrf -> csrf.disable());
        }
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.sendError(401, "Inicia sesión para continuar"))
                .accessDeniedHandler((request, response, exception) -> response.sendError(403, "No tienes permiso para esta operación")));
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/datos.json", "/planes/**", "/portal/**",
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll();
            if (!enabled) auth.anyRequest().permitAll();
            else {
                auth.requestMatchers("/api/v1/auth/login", "/api/v1/auth/status").permitAll();
                auth.requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/estado", "/api/v1/planes", "/api/v1/cursos/**", "/api/v1/tarifas-matricula").permitAll();
                auth.requestMatchers("/api/v1/auth/register", "/api/v1/auth/accounts/**").hasRole("ADMIN");
                auth.requestMatchers("/api/v1/alumno/**").hasRole("STUDENT");
                auth.requestMatchers("/api/v1/profesor/**").hasRole("TEACHER");
                auth.requestMatchers("/api/v1/profesores/**").hasRole("ADMIN");
                auth.requestMatchers("/actuator/**").hasRole("ADMIN");
                auth.requestMatchers("/api/v1/**").hasRole("ADMIN").anyRequest().permitAll();
            }
        });
        http.sessionManagement(session -> session.sessionCreationPolicy(
                "keycloak".equalsIgnoreCase(mode) ? org.springframework.security.config.http.SessionCreationPolicy.STATELESS : org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED));
        if (enabled && "keycloak".equalsIgnoreCase(mode)) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter())));
        } else {
            http.httpBasic(basic -> basic.disable()).formLogin(form -> form.disable());
        }
        return http.build();
    }

    private JwtAuthenticationConverter keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::keycloakAuthorities);
        return converter;
    }

    private Collection<org.springframework.security.core.GrantedAuthority> keycloakAuthorities(Jwt jwt) {
        Set<org.springframework.security.core.GrantedAuthority> authorities = new HashSet<>();
        Object realm = jwt.getClaims().get("realm_access");
        if (realm instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            roles.stream().map(Object::toString).map(String::toUpperCase)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(authorities::add);
        }
        Object resource = jwt.getClaims().get("resource_access");
        if (resource instanceof Map<?, ?> resources) {
            Object client = resources.get("fia-api");
            if (client instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof Collection<?> roles)
                roles.stream().map(Object::toString).map(String::toUpperCase)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(authorities::add);
        }
        return authorities;
    }
}







