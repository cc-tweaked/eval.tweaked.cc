package cc.tweaked.eval.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.prometheus.PrometheusCollector;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.Objects;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

public class TelemetryConfiguration {
    public static final String NAME = "cct-eval";

    private static Tracer tracer;

    public static void setup() {
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.builder()
            .put(SERVICE_NAME, NAME)
            .build()
        ));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
            .setResource(resource)
            .build();

        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(JaegerPropagator.getInstance()))
            .buildAndRegisterGlobal();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder().setResource(resource).buildAndRegisterGlobal();
        PrometheusCollector.builder().setMetricProducer(meterProvider).buildAndRegister();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        tracer = GlobalOpenTelemetry.getTracer(TelemetryConfiguration.NAME);
    }

    public static Tracer tracer() {
        return Objects.requireNonNull(tracer);
    }
}
