package com.example.web.frontend;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.security.oauth2.client.feign.OAuth2FeignRequestInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class EmployeeOAuth2FeignRequestInterceptor extends OAuth2FeignRequestInterceptor {

    @Value("${security.oauth2.client.access-token-uri}")
    private String accessTokenUri;
    
    @Value("${security.oauth2.client.client-id}")
    private String clientId;
    
    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;

    private final OAuth2ClientContext oAuth2ClientContext;
    
    List<AccessTokenProvider> accessTokenProviders = Arrays.asList(
            new AuthorizationCodeAccessTokenProvider(),
            new ImplicitAccessTokenProvider(),
            new ResourceOwnerPasswordAccessTokenProvider(),
            new ClientCredentialsAccessTokenProvider()
        );
    
    private final AccessTokenProvider accessTokenProvider = new AccessTokenProviderChain(accessTokenProviders);
    
    public EmployeeOAuth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext,
            OAuth2ProtectedResourceDetails resource) {
        super(oAuth2ClientContext, resource);
        this.oAuth2ClientContext = oAuth2ClientContext;
    }

    public EmployeeOAuth2FeignRequestInterceptor(OAuth2ClientContext oAuth2ClientContext) {
        super(oAuth2ClientContext, null);
        this.oAuth2ClientContext = oAuth2ClientContext;
    }
    
    @Override
    protected OAuth2AccessToken acquireAccessToken() {
        return acquireAccessToken(resource());
    }
    
    protected OAuth2AccessToken acquireAccessToken(OAuth2ProtectedResourceDetails resource)
            throws UserRedirectRequiredException {
            AccessTokenRequest tokenRequest = oAuth2ClientContext.getAccessTokenRequest();
            if (tokenRequest == null) {
                throw new AccessTokenRequiredException(
                    "Cannot find valid context on request for resource '"
                        + resource.getId() + "'.",
                    resource);
            }
            String stateKey = tokenRequest.getStateKey();
            if (stateKey != null) {
                tokenRequest.setPreservedState(
                    oAuth2ClientContext.removePreservedState(stateKey));
            }
            OAuth2AccessToken existingToken = oAuth2ClientContext.getAccessToken();
            if (existingToken != null) {
                oAuth2ClientContext.setAccessToken(existingToken);
            }
            OAuth2AccessToken obtainableAccessToken;
            obtainableAccessToken = accessTokenProvider.obtainAccessToken(resource,
                tokenRequest);
            if (obtainableAccessToken == null || obtainableAccessToken.getValue() == null) {
                throw new IllegalStateException(
                    " Access token provider returned a null token, which is illegal according to the contract.");
            }
            oAuth2ClientContext.setAccessToken(obtainableAccessToken);
            return obtainableAccessToken;
        }

        private ClientCredentialsResourceDetails resource() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            ClientCredentialsResourceDetails resourceDetails = null;
            
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                resourceDetails = new ClientCredentialsResourceDetails();
                resourceDetails.setAccessTokenUri(accessTokenUri);
                resourceDetails.setClientId(clientId);
                resourceDetails.setClientSecret(clientSecret);
            }
            
            return resourceDetails;
        }
}
