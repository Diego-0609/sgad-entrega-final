package com.sgad.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Registra a Google como proveedor OAuth2 / OpenID Connect.
 *
 * Solo se activa si existe SGAD_GOOGLE_CLIENT_ID. Sin esa variable la aplicación funciona igual
 * (login normal + JWT) y simplemente no muestra el botón "Continuar con Google".
 *
 * El Client ID y el Client Secret se crean en Google Cloud Console (ver README_JWT_OAUTH_DOCKER.md)
 * y NUNCA se escriben en el código ni se suben a GitHub.
 */
@Configuration
@ConditionalOnExpression("!'${sgad.google.client-id:}'.isBlank()")
public class GoogleOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${sgad.google.client-id}") String clientId,
            @Value("${sgad.google.client-secret:}") String clientSecret,
            @Value("${sgad.google.redirect-uri:}") String redirectUri) {

        // Plantilla de CommonOAuth2Provider.GOOGLE: ya trae las URLs de Google (autorización, token, JWK, userinfo).
        ClientRegistration.Builder google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email");

        // Por defecto Google vuelve a {baseUrl}/login/oauth2/code/google (se deduce del dominio con que se entró).
        if (!redirectUri.isBlank()) {
            google.redirectUri(redirectUri);
        }
        return new InMemoryClientRegistrationRepository(google.build());
    }
}
