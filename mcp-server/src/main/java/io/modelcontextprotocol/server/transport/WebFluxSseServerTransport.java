package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.utils.ApplicationContextHolder;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import io.modelcontextprotocol.util.Assert;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.RestfulToolCallbacProvider;
import org.springframework.ai.tool.method.RestfulToolCallback;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * @author yingzi
 * @date 2025/4/8:13:51
 */
public class WebFluxSseServerTransport implements ServerMcpTransport {
    private static final Logger logger = LoggerFactory.getLogger(WebFluxSseServerTransport.class);
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    private final ObjectMapper objectMapper;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final RouterFunction<?> routerFunction;
    private final ConcurrentHashMap<String, ClientSession> sessions;

    private Map<String, String> headersMap;
    private volatile boolean isClosing;
    private Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> connectHandler;

    public WebFluxSseServerTransport(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
        this.sessions = new ConcurrentHashMap();
        this.isClosing = false;
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
        this.objectMapper = objectMapper;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.routerFunction = RouterFunctions.route().GET(this.sseEndpoint, this::handleSseConnection).POST(this.messageEndpoint, this::handleMessage).build();
    }

    public WebFluxSseServerTransport(ObjectMapper objectMapper, String messageEndpoint) {
        this(objectMapper, messageEndpoint, "/sse");
    }

    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        this.connectHandler = handler;
        return Mono.empty().then();
    }

    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        } else {
            return Mono.create((sink) -> {
                try {
                    String jsonText = this.objectMapper.writeValueAsString(message);
                    ServerSentEvent<Object> event = ServerSentEvent.builder().event("message").data(jsonText).build();
                    logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());
                    List<String> failedSessions = this.sessions.values().stream().filter((session) -> {
                        return session.messageSink.tryEmitNext(event).isFailure();
                    }).map((session) -> {
                        return session.id;
                    }).toList();
                    if (failedSessions.isEmpty()) {
                        logger.debug("Successfully broadcast message to all sessions");
                        sink.success();
                    } else {
                        String error = "Failed to broadcast message to sessions: " + String.join(", ", failedSessions);
                        logger.error(error);
                        sink.error(new RuntimeException(error));
                    }
                } catch (IOException var7) {
                    logger.error("Failed to serialize message: {}", var7.getMessage());
                    sink.error(var7);
                }

            });
        }
    }

    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return this.objectMapper.convertValue(data, typeRef);
    }

    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            this.isClosing = true;
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());
        }).then(Mono.when(this.sessions.values().stream().map((session) -> {
            String sessionId = session.id;
            return Mono.fromRunnable(() -> {
                session.close();
            }).then(Mono.delay(Duration.ofMillis(100L))).then(Mono.fromRunnable(() -> {
                this.sessions.remove(sessionId);
            }));
        }).toList())).timeout(Duration.ofSeconds(5L)).doOnSuccess((v) -> {
            logger.debug("Graceful shutdown completed");
        }).doOnError((e) -> {
            logger.error("Error during graceful shutdown: {}", e.getMessage());
        });
    }

    public RouterFunction<?> getRouterFunction() {
        return this.routerFunction;
    }

    public Map<String, String> getHeadersMap() {
        return headersMap;
    }

    private Mono<ServerResponse> handleSseConnection(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        } else {
            // 获取请求头
            Map<String, String> headers = request.headers().asHttpHeaders().toSingleValueMap();
            logger.debug("Received SSE connection request with headers: {}", headers);
            this.headersMap = headers;

            // 找到RestfulToolCallbacProvider组件，设置请求头
            RestfulToolCallbacProvider restfulToolCallbacProvider = ApplicationContextHolder.getBean(RestfulToolCallbacProvider.class);
            for (ToolCallback toolCallback : restfulToolCallbacProvider.getToolCallbacks()) {
                if (toolCallback instanceof RestfulToolCallback) {
                    ((RestfulToolCallback) toolCallback).setHeadersMap(headersMap);
                }
            }

            String sessionId = UUID.randomUUID().toString();
            logger.debug("Creating new SSE connection for session: {}", sessionId);
            ClientSession session = new ClientSession(sessionId);
            this.sessions.put(sessionId, session);
            return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(Flux.create((sink) -> {
                logger.debug("Sending initial endpoint event to session: {}", sessionId);
                sink.next(ServerSentEvent.builder().event("endpoint").data(this.messageEndpoint).build());
                Flux var10000 = session.messageSink.asFlux().doOnSubscribe((s) -> {
                    logger.debug("Session {} subscribed to message sink", sessionId);
                }).doOnComplete(() -> {
                    logger.debug("Session {} completed", sessionId);
                    this.sessions.remove(sessionId);
                }).doOnError((error) -> {
                    logger.error("Error in session {}: {}", sessionId, error.getMessage());
                    this.sessions.remove(sessionId);
                }).doOnCancel(() -> {
                    logger.debug("Session {} cancelled", sessionId);
                    this.sessions.remove(sessionId);
                });
                Consumer var10001 = (event) -> {
                    logger.debug("Forwarding event to session {}: {}", sessionId, event);
                    sink.next((ServerSentEvent<?>) event);
                };
                Objects.requireNonNull(sink);
                var10000.subscribe(
                        var10001,
                        error -> sink.error((Throwable) error),
                        sink::complete
                );
                sink.onCancel(() -> {
                    logger.debug("Session {} cancelled", sessionId);
                    this.sessions.remove(sessionId);
                });
            }), ServerSentEvent.class);
        }
    }


    private Mono<ServerResponse> handleMessage(ServerRequest request) {
        return this.isClosing ? ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down") : request.bodyToMono(String.class).flatMap((body) -> {
            try {
                McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, body);
                return Mono.just(message).transform(this.connectHandler).flatMap((response) -> {
                    return ServerResponse.ok().build();
                }).onErrorResume((error) -> {
                    logger.error("Error processing message: {}", error.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(new McpError(error.getMessage()));
                });
            } catch (IOException | IllegalArgumentException var3) {
                logger.error("Failed to deserialize message: {}", var3.getMessage());
                return ServerResponse.badRequest().bodyValue(new McpError("Invalid message format"));
            }
        });
    }

    private static class ClientSession {
        private final String id;
        private final Sinks.Many<ServerSentEvent<?>> messageSink;

        ClientSession(String id) {
            this.id = id;
            WebFluxSseServerTransport.logger.debug("Creating new session: {}", id);
            this.messageSink = Sinks.many().replay().latest();
            WebFluxSseServerTransport.logger.debug("Session {} initialized with replay sink", id);
        }

        void close() {
            WebFluxSseServerTransport.logger.debug("Closing session: {}", this.id);
            Sinks.EmitResult result = this.messageSink.tryEmitComplete();
            if (result.isFailure()) {
                WebFluxSseServerTransport.logger.warn("Failed to complete message sink for session {}: {}", this.id, result);
            } else {
                WebFluxSseServerTransport.logger.debug("Successfully completed message sink for session {}", this.id);
            }

        }
    }
}
