package io.opentracing.dropwizard;

import io.opentracing.Span;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class ServerRequestTracingFilter implements ContainerRequestFilter {

    private DropWizardTracer tracer;

    public ServerRequestTracingFilter(DropWizardTracer tracer) {
        this.tracer = tracer;
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // set the operation name
        String operationName = "";
        for(Object resource : requestContext.getUriInfo().getMatchedResources()) {
            operationName += resource.getClass().getSimpleName() + " ";
        }

        // create the new span
        Span span;
        try {
            Span parentSpan = tracer.getTracer().join(requestContext.getHeaders()).start();
            span = tracer.getTracer().buildSpan(operationName).withParent(parentSpan).start();
        } catch(Exception e) {
            span = tracer.getTracer().buildSpan(operationName).start();
        }

        // add the new span to the trace
        tracer.addSpan(requestContext.getRequest(), span);
    }
}