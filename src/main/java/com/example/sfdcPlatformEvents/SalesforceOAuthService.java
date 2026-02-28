package com.example.sfdcPlatformEvents;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Map;

@Service
public class SalesforceOAuthService {

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @Value("${salesforce.client-id}")
    private String clientId;

    @Value("${salesforce.client-secret}")
    private String clientSecret;

    public @Nullable Map getAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = loginUrl + "/services/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Using MultiValueMap prevents special characters from breaking the request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);


        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try{
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return null;
    }
}
