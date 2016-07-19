package io.opentracing.contrib.dropwizard;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import io.opentracing.contrib.dropwizard.DropWizardTracer;
import io.opentracing.contrib.dropwizard.ServerRequestTracingFilter;
import io.opentracing.contrib.dropwizard.ServerResponseTracingFilter;
import io.opentracing.contrib.dropwizard.Trace;

@Provider
public class ServerTracingFeature implements DynamicFeature {

    private final DropWizardTracer tracer;
    private final Set<ServerAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private final boolean traceAll;

    private ServerTracingFeature(
        DropWizardTracer tracer, 
        Set<ServerAttribute> tracedAttributes, 
        Set<String> tracedProperties,
        boolean traceAll
    ) {
        this.tracer = tracer;
        this.tracedAttributes = tracedAttributes;
        this.tracedProperties = tracedProperties;
        this.traceAll = traceAll;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (traceAll || resourceInfo.getResourceMethod().getAnnotation(Trace.class) != null) {
            context.register(new ServerRequestTracingFilter(this.tracer, this.tracedAttributes, this.tracedProperties));
            context.register(new ServerResponseTracingFilter(this.tracer));
        }
    }

    public static class Builder {

        private final DropWizardTracer tracer;
        private Set<ServerAttribute> tracedAttributes;
        private Set<String> tracedProperties;
        private boolean traceAll;

        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
            this.tracedAttributes = new HashSet<ServerAttribute>();
            this.tracedProperties = new HashSet<String>();
            this.traceAll = true;
        }

        public Builder withTracedAttributes(Set<ServerAttribute> tracedAttributes) {
            this.tracedAttributes = tracedAttributes;
            return this;
        }

        public Builder withTracedProperties(Set<String> tracedProperties) {
            this.tracedProperties = tracedProperties;
            return this;
        }

        public Builder withTraceAnnotations() {
            this.traceAll = false;
            return this;
        }

        public ServerTracingFeature build() {
            return new ServerTracingFeature(this.tracer, this.tracedAttributes,this.tracedProperties, this.traceAll);
        }
    }
}