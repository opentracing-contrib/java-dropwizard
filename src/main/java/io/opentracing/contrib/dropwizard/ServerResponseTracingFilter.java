package io.opentracing.contrib.dropwizard;

import io.opentracing.contrib.dropwizard.DropWizardTracer;
import io.opentracing.Span;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * When registered to a client or webtarget along with a ClientResponseTracingFilter,
 * this filter ends the span for a client request when the request finishes.
 */
public class ServerResponseTracingFilter implements ContainerResponseFilter {

    private DropWizardTracer tracer;

    public ServerResponseTracingFilter(DropWizardTracer tracer) {
        this.tracer = tracer;
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        tracer.finishServerSpan(requestContext.getRequest());
    }
}