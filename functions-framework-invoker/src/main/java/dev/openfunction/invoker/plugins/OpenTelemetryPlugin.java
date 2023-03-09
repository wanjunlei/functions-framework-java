package dev.openfunction.invoker.plugins;

import dev.openfunction.functions.Context;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.Plugin;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class OpenTelemetryPlugin implements Plugin {
    private static final String Version = "v1-1.23.1";

    private final String name;
    private final Map<String, String> tags;
    private final Map<String, String> baggage;

    OpenTelemetry openTelemetry;
    TextMapGetter<HttpRequest> getter;
    Scope scope;

    public OpenTelemetryPlugin(String name, Map<String, String> tags, Map<String, String> baggage) {
        this.name = name;
        this.tags = tags;
        this.baggage = baggage;

        Resource resource = Resource.getDefault();

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
                .setResource(resource)
                .build();

        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
                .setResource(resource)
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setMeterProvider(sdkMeterProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        getter = new TextMapGetter<>() {
            @Override
            public String get(HttpRequest carrier, @NotNull String key) {
                if (carrier.getHeaders().containsKey(key)) {
                    return carrier.getHeaders().get(key).get(0);
                }
                return null;
            }

            @Override
            public Iterable<String> keys(@NotNull HttpRequest carrier) {
                return carrier.getHeaders().keySet();
            }
        };
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return Version;
    }

    @Override
    public Plugin init() {
        return this;
    }

    @Override
    public Error execPreHook(Context ctx) {
        Tracer tracer = openTelemetry.getTracer(ctx.getPodName()+"-"+ctx.getPodNamespace());
        HttpRequest request = ctx.getHttpRequest();

        // Extract the SpanContext and other elements from the request.
        io.opentelemetry.context.Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(io.opentelemetry.context.Context.current(), request, getter);

        // Automatically use the extracted SpanContext as parent.
        Span serverSpan = tracer.spanBuilder(ctx.getName())
                .setSpanKind(SpanKind.SERVER)
                .setParent(extractedContext)
                .startSpan();
        // Add the attributes defined in the Semantic Conventions
        serverSpan.setAttribute(SemanticAttributes.HTTP_METHOD, request.getMethod());
        serverSpan.setAttribute(SemanticAttributes.HTTP_URL, request.getPath());
        serverSpan.setAttribute(SemanticAttributes.FAAS_INVOKED_NAME, ctx.getName());
        serverSpan.setAttribute(SemanticAttributes.FAAS_INVOKED_PROVIDER, "OpenFunction");

        if (tags != null) {
            for (String key : tags.keySet()) {
                serverSpan.setAttribute(AttributeKey.stringKey(key), tags.get(key));
            }
        }

        if (baggage != null) {
            BaggageBuilder builder = Baggage.builder();
            for (String key : baggage.keySet()) {
                builder.put(key, baggage.get(key));
            }

            try(Scope ignored = serverSpan.makeCurrent()) {
                builder.build().storeInContext(io.opentelemetry.context.Context.current());
            }
        }

        // Make the serverSpan as the current active span, so user can create child span
        // in the function.
        scope = serverSpan.makeCurrent();

        return null;
    }

    @Override
    public Error execPostHook(Context ctx) {
        Span span = Span.current();
        int statusCode = ctx.getHttpResponse().getStatusCode();
        if (statusCode != 200 && statusCode != 201) {
            span.setStatus(StatusCode.valueOf(String.valueOf(statusCode)));
        }
        span.end();
        scope.close();

        return null;
    }

    @Override
    public Object getField(String fieldName) {
        return null;
    }
}
