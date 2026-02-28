package com.example.sfdcPlatformEvents;

import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class PlatformEventSubscriber {

    @Autowired
    private SalesforceOAuthService oauthService;

    @Value("${salesforce.api-version:65.0}")
    private String apiVersion;

    @EventListener(ContextRefreshedEvent.class)
    @Async
    public void subscribe() throws Exception {
        Map<String, String> auth = oauthService.getAccessToken();
        String accessToken = auth.get("access_token");
        String instanceUrl = auth.get("instance_url");

        BayeuxClient client = getBayeuxClient(instanceUrl, accessToken);

        client.handshake(reply -> {
            if (reply.isSuccessful()) {
                System.out.println("Connected to Salesforce!");
                client.getChannel("/event/Order_Event__e")
                        .subscribe((channel, message) -> {
                            System.out.println("Received: " + message.getData());
                        });
            } else {
                System.err.println("Handshake failed: " + reply);
            }
        });

        // ADD THIS: Keep the connection alive
        client.waitFor(Long.MAX_VALUE, BayeuxClient.State.DISCONNECTED);
    }

    private BayeuxClient getBayeuxClient(String instanceUrl, String accessToken) throws Exception {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        ClientConnector connector = new ClientConnector();
        connector.setSslContextFactory(sslContextFactory);

        HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector);
        HttpClient httpClient = new HttpClient(transport);
        httpClient.start();

        String cometdEndpoint = instanceUrl + "/cometd/" + apiVersion;

        Map<String, Object> options = new HashMap<>();
        // Use the string key directly instead of the constant
        options.put("headersHandlers", List.of(
                (BiConsumer<String, String>) (name, value) -> {}
        ));

        JettyHttpClientTransport cometdTransport = new JettyHttpClientTransport(options, httpClient) {
            @Override
            protected void customize(org.eclipse.jetty.client.Request request) {
                super.customize(request);
                request.headers(headers -> headers.add("Authorization", "Bearer " + accessToken));
            }
        };

        return new BayeuxClient(cometdEndpoint, cometdTransport);
    }
}