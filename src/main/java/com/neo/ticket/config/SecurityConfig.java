package com.neo.ticket.config;

import com.neo.ticket.iam.infrastructure.security.JwtClaims;
import com.neo.ticket.iam.infrastructure.security.JwtProperties;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.web.ProblemDetailWriter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    private static final String[] PUBLIC_DOCUMENTATION_PATHS = {
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    static final int DEFAULT_BCRYPT_STRENGTH = 12;

    @Bean
    PasswordEncoder passwordEncoder(
            @Value("${neo.security.bcrypt-strength:" + DEFAULT_BCRYPT_STRENGTH + "}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    SecretKey jwtSigningKey(JwtProperties properties) {
        return new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtClaims.ROLES);
        authoritiesConverter.setAuthorityPrefix(Role.AUTHORITY_PREFIX);

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            ProblemDetailWriter problemDetailWriter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register", "/api/auth/login", "/api/auth/refresh")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/public").permitAll()
                        .requestMatchers(PUBLIC_DOCUMENTATION_PATHS).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/actuator/**").hasRole(Role.ADMIN.name())
                        // Default deny, so a new endpoint is protected the moment it is written.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, exception) ->
                                problemDetailWriter.write(response, ErrorCode.AUTHENTICATION_REQUIRED,
                                        "A valid access token is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                problemDetailWriter.write(response, ErrorCode.ACCESS_DENIED,
                                        "Your role does not permit this operation")))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                problemDetailWriter.write(response, ErrorCode.AUTHENTICATION_REQUIRED,
                                        "A valid access token is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                problemDetailWriter.write(response, ErrorCode.ACCESS_DENIED,
                                        "Your role does not permit this operation")))
                .build();
    }
}
