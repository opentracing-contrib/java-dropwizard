package io.opentracing.contrib.dropwizard;

import io.opentracing.Span;
import io.opentracing.contrib.dropwizard.DropWizardTracer;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

/**
 * When registered to a client or webtarget along with a ClientResponseTracingFilter,
 * this filter ends the span for a client request when the request finishes.
 */
public class ClientResponseTracingFilter implements ClientResponseFilter {

    private DropWizardTracer tracer;

    public ClientResponseTracingFilter(DropWizardTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        this.tracer.finishClientSpan(requestContext);
    }
}