package io.opentracing.dropwizard;

import io.opentracing.Span;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Context;

import java.util.Map;
import java.util.HashMap;

public class ClientRequestTracingFilter implements ClientRequestFilter {

    private Request request;
    private DropWizardTracer tracer;

    public ClientRequestTracingFilter(DropWizardTracer tracer) {
        this.tracer = tracer;
        this.request = null;
    }
    
    private void setRequest(@Context Request request) {
        this.request = request;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        // set the operation name
        String operationName = requestContext.getUri().toString();

        // create the new span
        setRequest(null);
        Span span = null;
        if(this.request != null) {
            Span parentSpan = this.tracer.getSpan(request);
            if(parentSpan != null) {
                span = this.tracer.getTracer().buildSpan(operationName).withParent(parentSpan).start();
            }
            else {
                span = this.tracer.getTracer().buildSpan(operationName).start();
            }
        } else {
            span = this.tracer.getTracer().buildSpan(operationName).start();
        }


        // add the new span to the tracer
        tracer.addClientSpan(requestContext, span);

        // add the span to the headers
        Map<String, String> carrier = new HashMap<String, String>();
        tracer.getTracer().inject(span, carrier);
        for(String key : carrier.keySet()) {
            requestContext.getHeaders().add(key, carrier.get(key));
        }
    }
}