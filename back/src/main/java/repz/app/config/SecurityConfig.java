package repz.app.config;


import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import repz.app.exception.ErrorResponse;
import repz.app.message.Mensagens;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final Mensagens mensagens;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Rotas públicas para todos
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/health", "/health", "/actuator/health").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/users").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/users/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/ativar", "/api/users/*/desativar").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/academias/me").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/academias/me").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/academias/dashboard").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/academias").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/academias").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/academias/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/academias/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/academias/*/ativar", "/api/academias/*/desativar").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/personais/me", "/api/personais/me/alunos").hasRole("PERSONAL")
                        .requestMatchers(HttpMethod.PUT, "/api/personais/me").hasRole("PERSONAL")
                        .requestMatchers("/api/personais/**").hasAnyRole("ADMIN", "GERENTE")

                        .requestMatchers("/api/planos/**").hasAnyRole("ADMIN", "GERENTE")

                        .requestMatchers(HttpMethod.GET, "/api/alunos/me").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.PUT, "/api/alunos/me").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.GET, "/api/alunos").hasAnyRole("ADMIN", "GERENTE", "PERSONAL")
                        .requestMatchers(HttpMethod.GET, "/api/alunos/*").hasAnyRole("ADMIN", "GERENTE", "PERSONAL")
                        .requestMatchers(HttpMethod.PUT, "/api/alunos/*").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/alunos/*/inativar").hasAnyRole("ADMIN", "GERENTE")

                        .requestMatchers(HttpMethod.POST, "/api/checkins").hasAnyRole("ALUNO", "PERSONAL")
                        .requestMatchers(HttpMethod.GET, "/api/checkins/me").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.GET, "/api/checkins/alunos/inativos").hasAnyRole("PERSONAL", "GERENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/checkins/relatorio").hasAnyRole("GERENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/checkins/*").hasAnyRole("ALUNO", "PERSONAL", "GERENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/checkins").hasAnyRole("PERSONAL", "GERENTE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/checkins/*/ativar", "/api/checkins/*/desativar")
                                .hasAnyRole("PERSONAL", "GERENTE", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/avaliacoes").hasRole("PERSONAL")
                        .requestMatchers(HttpMethod.GET, "/api/avaliacoes/unidade").hasAnyRole("GERENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/avaliacoes/**")
                                .hasAnyRole("PERSONAL", "ALUNO", "GERENTE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/avaliacoes/*/ativar", "/api/avaliacoes/*/desativar")
                                .hasAnyRole("PERSONAL", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/treinos/me", "/api/treinos/me/historico").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.POST, "/api/treinos").hasRole("PERSONAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/treinos/*/ativar", "/api/treinos/*/desativar")
                                .hasAnyRole("PERSONAL", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/treinos/**")
                                .hasAnyRole("PERSONAL", "ALUNO", "GERENTE", "ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint())  // Erros de autenticação (ex: 401)
                        .accessDeniedHandler(accessDeniedHandler())  // Erros de autorização (ex: 403)
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    mensagens.get("erro.acesso.negado"),
                    accessDeniedException.getMessage());
            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        };
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    mensagens.get("erro.autenticacao"),
                    authException.getMessage());
            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://127.0.0.1:4200"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
