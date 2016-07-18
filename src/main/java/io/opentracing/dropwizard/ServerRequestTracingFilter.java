package io.opentracing.dropwizard;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.TextMapReader;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;


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

        // format the headers for extraction
        Span span;
        MultivaluedMap<String, String> rawHeaders = requestContext.getHeaders();
        final HashMap<String, String> headers = new HashMap<String, String>();
        for(String key : rawHeaders.keySet()){
            headers.put(key, rawHeaders.get(key).get(0));
        }

        // extract the client span
        try {
            SpanContext parentSpan = tracer.getTracer().extract(new TextMapReader() {
                public Iterator<Map.Entry<String, String>> getEntries() {
                    return headers.entrySet().iterator();
                }
            });
            if(parentSpan == null){
                span = tracer.getTracer().buildSpan(operationName).start();
            } else {
                span = tracer.getTracer().buildSpan(operationName).asChildOf(parentSpan).start();
            }
        } catch(IllegalArgumentException e) {
            span = tracer.getTracer().buildSpan(operationName).start();
        }

        // add the new span to the trace
        tracer.addServerSpan(requestContext.getRequest(), span);
    }
}