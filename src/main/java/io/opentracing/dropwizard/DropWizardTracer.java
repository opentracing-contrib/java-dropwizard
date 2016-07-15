package io.opentracing.dropwizard;

import io.opentracing.Tracer;
import io.opentracing.Span;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

public class DropWizardTracer {

    private Tracer tracer;
    private Map<Request, Span> currentSpans;

    public DropWizardTracer(Tracer tracer) {
        this.currentSpans = new ConcurrentHashMap<Request, Span>();
        this.tracer = tracer;
    }

    public Tracer getTracer() {
        return this.tracer;
    }

    public Span getSpan(Request request) {
        return this.currentSpans.get(request);
    }

    protected void addSpan(Request request, Span span) {
        this.currentSpans.put(request, span);
    }

    protected void finishSpan(Request request) {
        Span span = this.currentSpans.get(request);
        if(span != null) {
            this.currentSpans.remove(request);
            span.finish();
        }
    }

    protected Map<Request,Span> getCurrentSpans() {
        return this.currentSpans;
    }
}