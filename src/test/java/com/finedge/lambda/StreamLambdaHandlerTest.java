package com.finedge.lambda;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StreamLambdaHandler}.
 * Verifies that the Spring Boot Lambda container handler initializes
 * successfully. (The old dashboard-endpoint proxy test was removed in M2
 * with the legacy business controllers; endpoint coverage returns with
 * the reconstructed API in M3.)
 */
class StreamLambdaHandlerTest {

    @Test
    @DisplayName("Handler should initialize without error")
    void testHandlerInitialization() {
        SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler =
                StreamLambdaHandler.getHandler();
        assertNotNull(handler, "Handler should not be null after static initialization");
    }
}
