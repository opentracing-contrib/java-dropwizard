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

@Provider
public class ClientTracingFeature {

    private final DropWizardTracer tracer;
    private final Request request;
    private final Set<ClientAttribute> tracedAttributes;
    private final Set<String> tracedProperties;

    private ClientTracingFeature(
        DropWizardTracer tracer, 
        Request request, 
        Set<ClientAttribute> tracedAttributes, 
        Set<String> tracedProperties
    ) {
        this.tracer = tracer;
        this.request = request;
        this.tracedAttributes = tracedAttributes;
        this.tracedProperties = tracedProperties;
    }

    public void registerTo(Client client) {
        client.register(new ClientRequestTracingFilter(this.tracer, this.request, 
            this.tracedAttributes, this.tracedProperties));
        client.register(new ClientResponseTracingFilter(this.tracer));
    }

    public void registerTo(WebTarget target) {
        target.register(new ClientRequestTracingFilter(this.tracer, this.request, 
            this.tracedAttributes, this.tracedProperties));
        target.register(new ClientResponseTracingFilter(this.tracer));
    }

    public static class Builder {

        private final DropWizardTracer tracer;
        private Request request;
        private Set<ClientAttribute> tracedAttributes;
        private Set<String> tracedProperties; 

        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
            this.request = null;
            this.tracedAttributes = new HashSet<ClientAttribute>();
            this.tracedProperties = new HashSet<String>();
        }

        public Builder withTracedAttributes(Set<ClientAttribute> tracedAttributes) {
            this.tracedAttributes = tracedAttributes;
            return this;
        }

        public Builder withRequest(Request request) {
            this.request = request;
            return this;
        }

        public Builder withTracedProperties(Set<String> properties) {
            this.tracedProperties = properties;
            return this;
        }

        public ClientTracingFeature build() {
            return new ClientTracingFeature(this.tracer, this.request, 
                this.tracedAttributes, this.tracedProperties);
        }
    }
}