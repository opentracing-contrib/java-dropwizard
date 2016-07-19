package io.opentracing.contrib.dropwizard;

import java.util.Set;
import java.util.HashSet;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.Request;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import io.opentracing.contrib.dropwizard.DropWizardTracer;
import io.opentracing.contrib.dropwizard.ServerRequestTracingFilter;
import io.opentracing.contrib.dropwizard.ServerResponseTracingFilter;
import io.opentracing.contrib.dropwizard.Trace;
import io.opentracing.contrib.dropwizard.ClientAttribute;

/**
 * When registered to a Client or WebTarget, this feature
 * registers filters to trace the client request.
 * 
 * This feature is configured and built using ClientTracingFeature.Builder
 */
@Provider
public class ClientTracingFeature {

    private final DropWizardTracer tracer;
    private final Request request;
    private final Set<ClientAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private final String operationName;

    private ClientTracingFeature(
        DropWizardTracer tracer, 
        Request request, 
        String operationName,
        Set<ClientAttribute> tracedAttributes, 
        Set<String> tracedProperties
    ) {
        this.tracer = tracer;
        this.request = request;
        this.operationName = operationName;
        this.tracedAttributes = tracedAttributes;
        this.tracedProperties = tracedProperties;
    }

    /**
     * @param Client to register tracing filters to
     */
    public void registerTo(Client client) {
        client.register(new ClientRequestTracingFilter(this.tracer, this.request, 
            this.operationName, this.tracedAttributes, this.tracedProperties));
        client.register(new ClientResponseTracingFilter(this.tracer));
    }

    /**
     * @param WebTarget to register tracing filters to
     */
    public void registerTo(WebTarget target) {
        target.register(new ClientRequestTracingFilter(this.tracer, this.request, 
            this.operationName, this.tracedAttributes, this.tracedProperties));
        target.register(new ClientResponseTracingFilter(this.tracer));
    }

    /**
     * Use this class to configure and build a ClientTracingFeature
     */
    public static class Builder {

        private final DropWizardTracer tracer;
        private Request request;
        private Set<ClientAttribute> tracedAttributes;
        private Set<String> tracedProperties; 
        private String operationName;

        /**
         * @param tracer the tracer to trace the client requests with
         */
        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
            this.request = null;
            this.tracedAttributes = new HashSet<ClientAttribute>();
            this.tracedProperties = new HashSet<String>();
            this.operationName = "";
        }

        /**
         * @param request to continue the trace from. This 
         *  is what allows you to link this client request to previous 
         *  requests, instead of creating an entirely new trace.
         */
        public Builder withRequest(Request request) {
            this.request = request;
            return this;
        }

        /**
         * @param a set of ClientAttributes that you want to tag to spans
         *  created for client requests
         */
        public Builder withTracedAttributes(Set<ClientAttribute> tracedAttributes) {
            this.tracedAttributes = tracedAttributes;
            return this;
        }

        /**
         * @param a set of properties of the client request to tag 
         *  to spans created for client requests
         */
        public Builder withTracedProperties(Set<String> properties) {
            this.tracedProperties = properties;
            return this;
        }

        /**
         * @param operationName for spans created by this feature
         */
        public Builder withOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        /**
         * @return ClientTracingFeature with the configuration of this Builder 
         */
        public ClientTracingFeature build() {
            return new ClientTracingFeature(this.tracer, this.request,
                this.operationName, this.tracedAttributes, this.tracedProperties);
        }
    }
}