package br.com.datum.auth.grant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Collections;
import java.util.Set;

/**
 * Representa uma solicitação de token usando o grant customizado
 * "password": usuário/senha do usuário final, autenticados no contexto
 * do client OAuth2 já autenticado (client_id/client_secret).
 */
public class OAuth2PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType GRANT_TYPE = new AuthorizationGrantType("password");

    private final String username;
    private final String password;
    private final Set<String> scopes;

    public OAuth2PasswordAuthenticationToken(Authentication clientPrincipal,
                                              String username,
                                              String password,
                                              Set<String> scopes) {
        super(GRANT_TYPE, clientPrincipal, null);
        this.username = username;
        this.password = password;
        this.scopes = scopes != null ? scopes : Collections.emptySet();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}
