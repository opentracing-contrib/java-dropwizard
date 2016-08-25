package io.opentracing.contrib.dropwizard;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import io.opentracing.Span;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * When registered to a client or webtarget along with a ClientResponseTracingFilter,
 * this filter creates a span for the client request when it is made.
 */
public class ClientTracingFilter extends ClientFilter {

    private final Request currentRequest;
    private final DropWizardTracer tracer;
    private final Set<ClientAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private final String operationName;

    /**
     * @param tracer to trace requests with
     * @param currentRequest the current request to be a parent span
     *      for any client spans created (null if none)
     * @param operationName for any spans created by this filter
     * @param tracedAttributes any ClientAttributes to log to the span
     * @param tracedProperties any request properties to log to the span
     */
    private ClientTracingFilter(
        DropWizardTracer tracer, 
        Request currentRequest, 
        String operationName,
        Set<ClientAttribute> tracedAttributes, 
        Set<String> tracedProperties
    ) {
        this.tracer = tracer;
        this.currentRequest = currentRequest;
        this.operationName = operationName;
        this.tracedAttributes = tracedAttributes;
        this.tracedProperties = tracedProperties;
    }

    public static class Builder {

        private final DropWizardTracer tracer;
        private Request currentRequest;
        private Set<ClientAttribute> tracedAttributes;
        private Set<String> tracedProperties; 
        private String operationName;

        /**
         * @param tracer the tracer to trace the client requests with
         */
        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
            this.currentRequest = null;
            this.tracedAttributes = new HashSet<ClientAttribute>();
            this.tracedProperties = new HashSet<String>();
            this.operationName = "";
        }

        /**
         * @param request to continue the trace from. This 
         *  is what allows you to link this client request to previous 
         *  requests, instead of creating an entirely new trace.
         * @return Builder configured with added request
         */
        public Builder withRequest(Request request) {
            this.currentRequest = request;
            return this;
        }

        /**
         * @param tracedAttributes a set of request attributes that you want 
         *  to tag to spans created for client requests
         * @return Builder configured with added tracedAttributes
         */
        public Builder withTracedAttributes(Set<ClientAttribute> tracedAttributes) {
            this.tracedAttributes = tracedAttributes;
            return this;
        }

        /**
         * @param properties properties of the client request to tag 
         *  to spans created for client requests
         * @return Builder configured with added traced properties
         */
        public Builder withTracedProperties(Set<String> properties) {
            this.tracedProperties = properties;
            return this;
        }

        /**
         * @param operationName for spans created by this feature
         * @return Builder configured with added operationName
         */
        public Builder withOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        /**
         * @return ClientTracingFeature with the configuration of this Builder 
         */
        public ClientTracingFilter build() {
            return new ClientTracingFilter(this.tracer, this.currentRequest,
                this.operationName, this.tracedAttributes, this.tracedProperties);
        }
    }

    @Override
    public ClientResponse handle(ClientRequest request) {
        // set the operation name
        String operationName;
        if (this.operationName == ""){
            operationName = "Client";
        } else {
            operationName = this.operationName;
        }

        // create the new span
        Span span = null;
        if (this.currentRequest != null) {
            Span parentSpan = this.tracer.getSpan(currentRequest);
            if (parentSpan == null) {
                span = this.tracer.getTracer().buildSpan(operationName).start();
            } else {
                span = this.tracer.getTracer().buildSpan(operationName).asChildOf(parentSpan.context()).start();
            }
        } else {
            span = this.tracer.getTracer().buildSpan(operationName).start();
        }

        // trace attributes
       for (ClientAttribute attribute : this.tracedAttributes) {
            switch(attribute) {
                case ENTITY:
                    try { span.log("Entity", request.getEntity()); }
                    catch (NullPointerException npe) {}
                    break;
                case HEADERS:
                    try { span.setTag("Headers", request.getHeaders().toString()); }
                    catch (NullPointerException npe) {}
                    break;
                case METHOD:
                    try { span.setTag("Method", request.getMethod()); }
                    catch (NullPointerException npe) {}
                    break;
                case URI:
                    try { span.setTag("URI", request.getURI().toString()); }
                    catch (NullPointerException npe) {}
                    break;
            }
        }

        // trace properties
        for (String propertyName : this.tracedProperties) {
            Object property = request.getProperties().get(propertyName);
            if (property != null) {
                span.log(propertyName, property);
            }
        }

        // add the new span to the tracer
        tracer.addClientSpan(request, span);

        // add the span to the headers
        final MultivaluedMap<String, Object> headers = request.getHeaders();
        tracer.getTracer().inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMap() {
            @Override
            public void put(String k, String v) {
                headers.putSingle(k, v);
            }

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("iterator should never be used with Tracer.inject()");
            }
        });
        ClientResponse response = getNext().handle(request);

        this.tracer.finishClientSpan(request);

        return response;
    }
}
