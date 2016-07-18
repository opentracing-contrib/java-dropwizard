package io.opentracing.dropwizard;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapWriter;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import java.util.Map;
import java.util.HashMap;

public class ClientRequestTracingFilter implements ClientRequestFilter {

    private Request request;
    private DropWizardTracer tracer;

    public ClientRequestTracingFilter(DropWizardTracer tracer) {
        this.tracer = tracer;
        this.request = null;
    }

    public ClientRequestTracingFilter withContinuedTrace(Request request) {
        this.request = request;
        return this;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        
        // set the operation name
        String operationName = requestContext.getUri().toString();

        // create the new span
        Span span = null;
        if(this.request != null) {
            Span parentSpan = this.tracer.getSpan(request);
            if(parentSpan == null) {
                span = this.tracer.getTracer().buildSpan(operationName).start();
            } else {
                span = this.tracer.getTracer().buildSpan(operationName).asChildOf(parentSpan.context()).start();
            }
        } else {
            span = this.tracer.getTracer().buildSpan(operationName).start();
        }

        // add the new span to the tracer
        tracer.addClientSpan(requestContext, span);

        // add the span to the headers
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        tracer.getTracer().inject(span.context(), new TextMapWriter() {
            public void put(String key, String value) {
                headers.add(key, value);
            }
        });
    }
}