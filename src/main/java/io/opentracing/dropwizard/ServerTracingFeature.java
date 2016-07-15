package io.opentracing.dropwizard;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import io.opentracing.dropwizard.DropWizardTracer;
import io.opentracing.dropwizard.ServerRequestTracingFilter;
import io.opentracing.dropwizard.ServerResponseTracingFilter;
import io.opentracing.dropwizard.Trace;

@Provider
public class ServerTracingFeature implements DynamicFeature {

    private final DropWizardTracer tracer;

    public ServerTracingFeature(DropWizardTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(Trace.class) != null) {
            context.register(new ServerRequestTracingFilter(this.tracer));
            context.register(new ServerResponseTracingFilter(this.tracer));
        }
    }
}