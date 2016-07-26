package io.opentracing.contrib.dropwizard;

import io.opentracing.contrib.dropwizard.DropWizardTracer;
import io.opentracing.Span;

import java.io.IOException;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

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
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        tracer.finishServerSpan(request);
        return response;
    }
}