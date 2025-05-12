//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.util.Assert;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class WebFluxSseServerTransportProvider implements McpServerTransportProvider {
    private static final Logger logger = LoggerFactory.getLogger(WebFluxSseServerTransportProvider.class);
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    public static final String DEFAULT_BASE_URL = "";
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final RouterFunction<?> routerFunction;
    private McpServerSession.Factory sessionFactory;
    private final ConcurrentHashMap<String, McpServerSession> sessions;
    private volatile boolean isClosing;

    public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint) {
        this(objectMapper, messageEndpoint, "/sse");
    }

    public WebFluxSseServerTransportProvider(String baseUrl, ObjectMapper objectMapper, String messageEndpoint) {
        this(objectMapper, baseUrl, messageEndpoint, "/sse");
    }

    public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
        this(objectMapper, "", messageEndpoint, sseEndpoint);
    }

    public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String baseUrl, String messageEndpoint, String sseEndpoint) {
        this.sessions = new ConcurrentHashMap();
        this.isClosing = false;
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(baseUrl, "Message base path must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.routerFunction = RouterFunctions.route().GET(this.sseEndpoint, this::handleSseConnection).POST(this.messageEndpoint, this::handleMessage).build();
    }

    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Mono<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        } else {
            logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());
            return Flux.fromIterable(this.sessions.values()).flatMap((session) -> {
                return session.sendNotification(method, params).doOnError((e) -> {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }).onErrorComplete();
            }).then();
        }
    }

    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(this.sessions.values()).doFirst(() -> {
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());
        }).flatMap(McpServerSession::closeGracefully).then();
    }

    public RouterFunction<?> getRouterFunction() {
        return this.routerFunction;
    }

    private Mono<ServerResponse> handleSseConnection(ServerRequest request) {
        return this.isClosing ? ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down") : ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(Flux.create((sink) -> {
            WebFluxMcpSessionTransport sessionTransport = new WebFluxMcpSessionTransport(sink);
            McpServerSession session = this.sessionFactory.create(sessionTransport);
            String sessionId = session.getId();
            logger.debug("Created new SSE connection for session: {}", sessionId);
            this.sessions.put(sessionId, session);
            logger.debug("Sending initial endpoint event to session: {}", sessionId);
            sink.next(ServerSentEvent.builder().event("endpoint").data(this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId).build());
            sink.onCancel(() -> {
                logger.debug("Session {} cancelled", sessionId);
                this.sessions.remove(sessionId);
            });
        }), ServerSentEvent.class);
    }

    private Mono<ServerResponse> handleMessage(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        } else if (request.queryParam("sessionId").isEmpty()) {
            return ServerResponse.badRequest().bodyValue(new McpError("Session ID missing in message endpoint"));
        } else {
            McpServerSession session = (McpServerSession)this.sessions.get(request.queryParam("sessionId").get());
            return session == null ? ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(new McpError("Session not found: " + (String)request.queryParam("sessionId").get())) : request.bodyToMono(String.class).flatMap((body) -> {
                try {
                    McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, body);
                    return session.handle(message).flatMap((response) -> {
                        return ServerResponse.ok().build();
                    }).onErrorResume((error) -> {
                        logger.error("Error processing  message: {}", error.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(new McpError(error.getMessage()));
                    });
                } catch (IOException | IllegalArgumentException var4) {
                    logger.error("Failed to deserialize message: {}", var4.getMessage());
                    return ServerResponse.badRequest().bodyValue(new McpError("Invalid message format"));
                }
            });
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ObjectMapper objectMapper;
        private String baseUrl = "";
        private String messageEndpoint;
        private String sseEndpoint = "/sse";

        public Builder() {
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder basePath(String baseUrl) {
            Assert.notNull(baseUrl, "basePath must not be null");
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder messageEndpoint(String messageEndpoint) {
            Assert.notNull(messageEndpoint, "Message endpoint must not be null");
            this.messageEndpoint = messageEndpoint;
            return this;
        }

        public Builder sseEndpoint(String sseEndpoint) {
            Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
            this.sseEndpoint = sseEndpoint;
            return this;
        }

        public WebFluxSseServerTransportProvider build() {
            Assert.notNull(this.objectMapper, "ObjectMapper must be set");
            Assert.notNull(this.messageEndpoint, "Message endpoint must be set");
            return new WebFluxSseServerTransportProvider(this.objectMapper, this.baseUrl, this.messageEndpoint, this.sseEndpoint);
        }
    }

    private class WebFluxMcpSessionTransport implements McpServerTransport {
        private final FluxSink<ServerSentEvent> sink;

        public WebFluxMcpSessionTransport(FluxSink<ServerSentEvent> sink) {
            this.sink = sink;
        }

        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromSupplier(() -> {
                try {
                    return WebFluxSseServerTransportProvider.this.objectMapper.writeValueAsString(message);
                } catch (IOException var3) {
                    throw Exceptions.propagate(var3);
                }
            }).doOnNext((jsonText) -> {
                ServerSentEvent<Object> event = ServerSentEvent.builder().event("message").data(jsonText).build();
                this.sink.next(event);
            }).doOnError((e) -> {
                Throwable exception = Exceptions.unwrap(e);
                this.sink.error(exception);
            }).then();
        }

        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return WebFluxSseServerTransportProvider.this.objectMapper.convertValue(data, typeRef);
        }

        public Mono<Void> closeGracefully() {
            FluxSink var10000 = this.sink;
            Objects.requireNonNull(var10000);
            return Mono.fromRunnable(var10000::complete);
        }

        public void close() {
            this.sink.complete();
        }
    }
}
