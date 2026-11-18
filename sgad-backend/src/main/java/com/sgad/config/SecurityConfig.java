package com.sgad.config;

import com.sgad.security.CustomUserDetailsService;
import com.sgad.security.GoogleOidcUserService;
import com.sgad.security.JwtAuthFilter;
import com.sgad.security.JwtService;
import com.sgad.security.SeguridadHandlers;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Seguridad del backend SGAD. Dos cadenas de filtros:
 *  - @Order(1) JWT sin sesión, para /api/** con "Authorization: Bearer".
 *  - @Order(2) sesión/cookie: formulario de login, Google (OAuth2) y la API sin token.
 *
 * Permisos de la API (GET / escritura):
 *  escuelas, entrenadores .... cualquier usuario / ADMIN
 *  deportistas, entrenamientos, asistencias .... cualquier usuario / ADMIN, ENTRENADOR
 *  materiales, reportes ........ ADMIN, ENTRENADOR (lectura y escritura)
 *  /api/mi/** .................. solo DEPORTISTA, siempre sobre sus propios datos
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final SeguridadHandlers seguridadHandlers;
    private final JwtService jwtService;

    @Value("${sgad.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${sgad.session.max-por-usuario:1}")
    private int maxSesionesPorUsuario;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Cadena 1: JWT sin sesión
    @Bean
    @Order(1)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http,
                                              CustomUserDetailsService userDetailsService) throws Exception {
        http
            .securityMatcher(this::esPeticionBearerDeApi)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())   // sin cookie no hay CSRF: el token no viaja solo
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // Un JWT no inicia sesión: si cambiara el id de JSESSIONID, el siguiente F5 sacaría al usuario.
                .sessionFixation(fixation -> fixation.none()))
            .authorizeHttpRequests(auth -> {
                // Un JWT no sirve para pedir otro token.
                auth.requestMatchers("/api/auth/token").denyAll();
                auth.requestMatchers("/api/auth/**").authenticated();
                permisosDeLaApi(auth);
                auth.anyRequest().denyAll();
            })
            .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService, seguridadHandlers),
                    UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(seguridadHandlers)
                .accessDeniedHandler(seguridadHandlers));
        return http.build();
    }

    private boolean esPeticionBearerDeApi(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        return cabecera != null
                && cabecera.regionMatches(true, 0, "Bearer ", 0, 7)
                && request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    /** Permisos de la API, iguales para cookie y JWT. Gana la primera regla que coincide. */
    private void permisosDeLaApi(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers("/api/mi/**").hasRole("DEPORTISTA");
        auth.requestMatchers("/api/deportistas/**", "/api/materiales/**", "/api/entrenamientos/**",
                "/api/asistencias/**", "/api/reportes/**").hasAnyRole("ADMIN", "ENTRENADOR");
        auth.requestMatchers(HttpMethod.GET, "/api/**").authenticated();
        auth.requestMatchers("/api/escuelas/**", "/api/entrenadores/**").hasRole("ADMIN");
        auth.requestMatchers("/api/**").denyAll();   // cualquier otra ruta de API no prevista
    }

    // Cadena 2: sesión/cookie
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ObjectProvider<ClientRegistrationRepository> clientesOAuth,
                                           // Parámetro y no campo: evita un ciclo con el PasswordEncoder de esta clase.
                                           GoogleOidcUserService googleOidcUserService) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF solo en los formularios; la API se protege con CORS y cookie SameSite=Lax.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                auth.requestMatchers("/", "/publico", "/login", "/registro", "/registro/google", "/css/**", "/js/**", "/webjars/**", "/error").permitAll();
                auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();   // flujo de Google

                auth.requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll();   // login por API -> entrega un JWT
                auth.requestMatchers("/api/auth/**").authenticated();
                permisosDeLaApi(auth);

                auth.requestMatchers("/admin/**").hasRole("ADMIN");
                auth.requestMatchers("/entrenador/**").hasAnyRole("ADMIN", "ENTRENADOR");
                auth.requestMatchers("/panel").authenticated();
                auth.anyRequest().authenticated();
            })
            .formLogin((FormLoginConfigurer<HttpSecurity> form) -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl(frontendUrl, true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/logout", "GET"),
                        new AntPathRequestMatcher("/logout", "POST")))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
                .invalidSessionStrategy(seguridadHandlers)
                .maximumSessions(maxSesionesPorUsuario)
                    .maxSessionsPreventsLogin(false)   // la sesión más antigua es la que se cierra
                    .sessionRegistry(sessionRegistry())
                    .expiredSessionStrategy(seguridadHandlers)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(puntoDeEntrada())
                .accessDeniedHandler(seguridadHandlers)
            );

        // Google solo se activa si existe SGAD_GOOGLE_CLIENT_ID
        if (clientesOAuth.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(info -> info.oidcUserService(googleOidcUserService))
                .defaultSuccessUrl(frontendUrl, true)
                .failureHandler(fallaGoogle()));
        }

        return http.build();
    }

    /** Un correo de Google sin cuenta va al registro para elegir su rol; cualquier otro fallo, al login. */
    private AuthenticationFailureHandler fallaGoogle() {
        return (request, response, exception) -> {
            boolean sinCuenta = exception instanceof OAuth2AuthenticationException oauth
                    && GoogleOidcUserService.SIN_CUENTA.equals(oauth.getError().getErrorCode());
            response.sendRedirect(request.getContextPath() + (sinCuenta ? "/registro?sinCuenta=true" : "/login?oauthError=true"));
        };
    }

    /** Sin sesión: la API responde 401 en JSON y las páginas redirigen al login. */
    private AuthenticationEntryPoint puntoDeEntrada() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> porRuta = new LinkedHashMap<>();
        porRuta.put(new AntPathRequestMatcher("/api/**"), seguridadHandlers);
        DelegatingAuthenticationEntryPoint delegado = new DelegatingAuthenticationEntryPoint(porRuta);
        delegado.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return delegado;
    }
}
