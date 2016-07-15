package io.opentracing.dropwizard;

import io.opentracing.Span;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

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