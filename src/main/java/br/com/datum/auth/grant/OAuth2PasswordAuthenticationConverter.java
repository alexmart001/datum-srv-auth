package br.com.datum.auth.grant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lê uma requisição POST /oauth2/token com grant_type=password e extrai
 * username/password/scope do corpo do formulário.
 */
public class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!OAuth2PasswordAuthenticationToken.GRANT_TYPE.getValue().equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        MultiValueMap<String, String> parameters = getParameters(request);

        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username) || parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
            throwInvalidRequest(OAuth2ParameterNames.USERNAME);
        }

        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        if (!StringUtils.hasText(password) || parameters.get(OAuth2ParameterNames.PASSWORD).size() != 1) {
            throwInvalidRequest(OAuth2ParameterNames.PASSWORD);
        }

        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        Set<String> scopes = StringUtils.hasText(scope)
                ? new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")))
                : Collections.emptySet();

        return new OAuth2PasswordAuthenticationToken(clientPrincipal, username, password, scopes);
    }

    private static void throwInvalidRequest(String parameterName) {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_REQUEST,
                "OAuth 2.0 Parameter: " + parameterName,
                "https://datatracker.ietf.org/doc/html/rfc6749#section-4.3.2");
        throw new OAuth2AuthenticationException(error);
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }
}
