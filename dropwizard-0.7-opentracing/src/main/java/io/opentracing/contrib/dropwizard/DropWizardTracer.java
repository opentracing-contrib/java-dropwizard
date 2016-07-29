package io.opentracing.contrib.dropwizard;

import com.sun.jersey.spi.container.ContainerRequest;

import io.opentracing.Tracer;
import io.opentracing.Span;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import com.sun.jersey.api.client.ClientRequest;
/**
 * 
 *
 */
public class DropWizardTracer {

    private Tracer tracer;
    private Map<Request, Span> serverSpans;
    private Map<ClientRequest, Span> clientSpans;

    /**
     * Create a tracer for DropWizard applications.
     * @param tracer an io.opentracing.Tracer to trace requests with
     */
    public DropWizardTracer(Tracer tracer) {
        this.serverSpans = new ConcurrentHashMap<Request, Span>();
        this.clientSpans = new ConcurrentHashMap<ClientRequest, Span>();
        this.tracer = tracer;
    }

    /**
     * @return the underlying io.opentracing.Tracer
     */
    public Tracer getTracer() {
        return this.tracer;
    }

    /**
     * @param request for which we want to find the associated span 
     * @return the span for this server request, if it exists and isn't 
     *  finished. Otherwise returns null.
     */
    public Span getSpan(Request request) {
        return this.serverSpans.get(request);
    }

    /**
     * @param request for which we want to find the associated span
     * @return the span for this client request, if it exists and is 
     *  not finished. Otherwise returns null.
     */
    public Span getSpan(ClientRequest request) {
        return this.clientSpans.get(request);
    }

    protected void addServerSpan(Request request, Span span) {
        this.serverSpans.put(request, span);
    }

    protected void addClientSpan(ClientRequest requestCtx, Span span) {
        this.clientSpans.put(requestCtx, span);
    }

    protected void finishServerSpan(Request request) {
        Span span = this.serverSpans.get(request);
        if (span != null) {
            this.serverSpans.remove(request);
            span.finish();
        }
    }

    protected void finishClientSpan(ClientRequest requestCtx) {
        Span span = this.clientSpans.get(requestCtx);
        if (span != null) {
            this.clientSpans.remove(requestCtx);
            span.finish();
        }
    }
}
