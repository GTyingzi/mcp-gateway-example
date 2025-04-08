package io.modelcontextprotocol.client.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.ClientMcpTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/**
 * @author yingzi
 * @date 2025/4/8:10:34
 */
public class WebFluxSseClientTransport implements ClientMcpTransport {
    private static final Logger logger = LoggerFactory.getLogger(WebFluxSseClientTransport.class);
    private static final String MESSAGE_EVENT_TYPE = "message";
    private static final String ENDPOINT_EVENT_TYPE = "endpoint";
    private static final String SSE_ENDPOINT = "/sse";
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<ServerSentEvent<String>>() {
    };
    private final WebClient webClient;
    protected ObjectMapper objectMapper;
    private Disposable inboundSubscription;
    private volatile boolean isClosing;
    protected final Sinks.One<String> messageEndpointSink;
    private BiConsumer<Retry.RetrySignal, SynchronousSink<Object>> inboundRetryHandler;

    public WebFluxSseClientTransport(WebClient.Builder webClientBuilder) {
        this(webClientBuilder, new ObjectMapper());
    }

    public WebFluxSseClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.isClosing = false;
        this.messageEndpointSink = Sinks.one();
        this.inboundRetryHandler = (retrySpec, sink) -> {
            if (this.isClosing) {
                logger.debug("SSE connection closed during shutdown");
                sink.error(retrySpec.failure());
            } else if (retrySpec.failure() instanceof IOException) {
                logger.debug("Retrying SSE connection after IO error");
                sink.next(retrySpec);
            } else {
                logger.error("Fatal SSE error, not retrying: {}", retrySpec.failure().getMessage());
                sink.error(retrySpec.failure());
            }
        };
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(webClientBuilder, "WebClient.Builder must not be null");
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
    }

    public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        Flux<ServerSentEvent<String>> events = this.eventStream();
        this.inboundSubscription = events.concatMap((event) -> {
            return Mono.just(event).<JSONRPCMessage>handle((e, s) -> {
                if ("endpoint".equals(event.event())) {
                    String messageEndpointUri = (String)event.data();
                    if (this.messageEndpointSink.tryEmitValue(messageEndpointUri).isSuccess()) {
                        s.complete();
                    } else {
                        s.error(new McpError("Failed to handle SSE endpoint event"));
                    }
                } else if ("message".equals(event.event())) {
                    try {
                        JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, (String)event.data());
                        s.next(message);
                    } catch (IOException var5) {
                        s.error(var5);
                    }
                } else {
                    s.error(new McpError("Received unrecognized SSE event type: " + event.event()));
                }

            }).transform(handler);
        }).subscribe();
        return this.messageEndpointSink.asMono().then();
    }

    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return this.messageEndpointSink.asMono().flatMap((messageEndpointUri) -> {
            if (this.isClosing) {
                return Mono.empty();
            } else {
                try {
                    String jsonText = this.objectMapper.writeValueAsString(message);
                    return ((WebClient.RequestBodySpec)this.webClient.post().uri(messageEndpointUri, new Object[0])).contentType(MediaType.APPLICATION_JSON).bodyValue(jsonText).retrieve().toBodilessEntity().doOnSuccess((response) -> {
                        logger.debug("Message sent successfully");
                    }).doOnError((error) -> {
                        if (!this.isClosing) {
                            logger.error("Error sending message: {}", error.getMessage());
                        }

                    });
                } catch (IOException var4) {
                    return !this.isClosing ? Mono.error(new RuntimeException("Failed to serialize message", var4)) : Mono.empty();
                }
            }
        }).then();
    }

    protected Flux<ServerSentEvent<String>> eventStream() {
        return this.webClient.get().uri("/sse", new Object[0]).accept(new MediaType[]{MediaType.TEXT_EVENT_STREAM}).retrieve().bodyToFlux(SSE_TYPE).retryWhen(Retry.from((retrySignal) -> {
            return retrySignal.handle(this.inboundRetryHandler);
        }));
    }

    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            this.isClosing = true;
            if (this.inboundSubscription != null) {
                this.inboundSubscription.dispose();
            }

        }).then().subscribeOn(Schedulers.boundedElastic());
    }

    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return this.objectMapper.convertValue(data, typeRef);
    }
}
