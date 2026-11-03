package com.finedge.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.finedge.FinEdgeApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AWS Lambda RequestStreamHandler implementation for Spring Boot 3.
 * Intercepts incoming API Gateway REST / HTTP API proxy events and routes
 * them through the Spring Boot WebMVC dispatcher pipeline.
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(StreamLambdaHandler.class);

    private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            log.info("Initializing SpringBootLambdaContainerHandler for FinEdgeApplication...");
            long startTime = System.currentTimeMillis();

            // Initialize SpringBoot 3 lambda container handler
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(FinEdgeApplication.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("SpringBootLambdaContainerHandler initialized successfully in {} ms", duration);
        } catch (ContainerInitializationException e) {
            log.error("Fatal error: Could not initialize Spring Boot application container", e);
            throw new RuntimeException("Could not initialize Spring Boot application container in AWS Lambda", e);
        }
    }

    public static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getHandler() {
        return handler;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
