package br.com.datum.auth.grant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test puro (sem contexto Spring, sem banco, sem RabbitMQ) da lógica
 * central por trás de POST /oauth2/token com grant_type=password:
 * AuthenticationManager, OAuth2AuthorizationService e OAuth2TokenGenerator
 * são todos mockados.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2PasswordAuthenticationProviderTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OAuth2AuthorizationService authorizationService;

    @Mock
    private OAuth2TokenGenerator<Jwt> tokenGenerator;

    private OAuth2PasswordAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OAuth2PasswordAuthenticationProvider(authenticationManager, authorizationService, tokenGenerator);

        // Em produção, esse contexto é preenchido pelo filtro do
        // Authorization Server antes de chegar no provider. Aqui, fora de
        // uma requisição real, precisamos simular manualmente.
        AuthorizationServerSettings settings = AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return settings.getIssuer();
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return settings;
            }
        });
    }

    @AfterEach
    void tearDown() {
        AuthorizationServerContextHolder.resetContext();
    }

    private RegisteredClient registeredClient(AuthorizationGrantType... grantTypes) {
        RegisteredClient.Builder builder = RegisteredClient.withId("client-id")
                .clientId("postman-client")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        for (AuthorizationGrantType grantType : grantTypes) {
            builder.authorizationGrantType(grantType);
        }
        return builder.build();
    }

    private OAuth2ClientAuthenticationToken authenticatedClient(RegisteredClient registeredClient) {
        return new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
    }

    private Jwt jwtGerado() {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .claim("sub", "admin")
                .claim("roles", Set.of("ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();
    }

    @Test
    void clientNaoAutenticado_lancaInvalidClient() {
        Authentication clientNaoAutenticado = new UsernamePasswordAuthenticationToken("nao-e-client", "x");
        OAuth2PasswordAuthenticationToken token =
                new OAuth2PasswordAuthenticationToken(clientNaoAutenticado, "admin", "admin123", Set.of());

        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void clientSemGrantPassword_lancaUnauthorizedClient() {
        RegisteredClient client = registeredClient(AuthorizationGrantType.CLIENT_CREDENTIALS);
        OAuth2PasswordAuthenticationToken token =
                new OAuth2PasswordAuthenticationToken(authenticatedClient(client), "admin", "admin123", Set.of());

        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT));

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void credenciaisInvalidas_lancaInvalidGrantSemSalvarAutorizacao() {
        RegisteredClient client = registeredClient(OAuth2PasswordAuthenticationToken.GRANT_TYPE);
        OAuth2PasswordAuthenticationToken token =
                new OAuth2PasswordAuthenticationToken(authenticatedClient(client), "admin", "senha-errada", Set.of());

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo(OAuth2ErrorCodes.INVALID_GRANT));

        verify(authorizationService, never()).save(any());
    }

    @Test
    void credenciaisValidas_emiteAccessTokenESalvaAutorizacao() {
        RegisteredClient client = registeredClient(OAuth2PasswordAuthenticationToken.GRANT_TYPE);
        OAuth2PasswordAuthenticationToken token =
                new OAuth2PasswordAuthenticationToken(authenticatedClient(client), "admin", "admin123", Set.of());

        Authentication usuarioAutenticado = new UsernamePasswordAuthenticationToken(
                "admin", "admin123", AuthorityUtils.createAuthorityList("ADMIN"));
        when(authenticationManager.authenticate(any())).thenReturn(usuarioAutenticado);
        when(tokenGenerator.generate(any())).thenReturn(jwtGerado());

        Authentication result = provider.authenticate(token);

        assertThat(result).isInstanceOf(OAuth2AccessTokenAuthenticationToken.class);
        OAuth2AccessTokenAuthenticationToken accessTokenAuth = (OAuth2AccessTokenAuthenticationToken) result;
        assertThat(accessTokenAuth.getAccessToken().getTokenValue()).isEqualTo("token-value");

        verify(authorizationService).save(any(OAuth2Authorization.class));
    }

    @Test
    void geradorNaoConsegueGerarToken_lancaServerError() {
        RegisteredClient client = registeredClient(OAuth2PasswordAuthenticationToken.GRANT_TYPE);
        OAuth2PasswordAuthenticationToken token =
                new OAuth2PasswordAuthenticationToken(authenticatedClient(client), "admin", "admin123", Set.of());

        Authentication usuarioAutenticado = new UsernamePasswordAuthenticationToken(
                "admin", "admin123", AuthorityUtils.createAuthorityList("ADMIN"));
        when(authenticationManager.authenticate(any())).thenReturn(usuarioAutenticado);
        when(tokenGenerator.generate(any())).thenReturn(null);

        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
                        .isEqualTo(OAuth2ErrorCodes.SERVER_ERROR));
    }

    @Test
    void supports_apenasOAuth2PasswordAuthenticationToken() {
        assertThat(provider.supports(OAuth2PasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }
}
