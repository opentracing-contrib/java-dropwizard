package io.opentracing.contrib.dropwizard;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import io.opentracing.Span;

/**
 * When registered to a DropWizard application, this feature
 * registers filters to trace the any requests to the application.
 * 
 * This feature is configured and built using ServerTracingFeature.Builder
 */
@Provider
public class ServerTracingFeature implements DynamicFeature {

    private final DropWizardTracer tracer;
    private final Set<ServerAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private final boolean traceAll;
    private final String operationName;
    private final RequestSpanDecorator decorator;
    static ThreadLocal<Span> threadLocalRequestSpan = new ThreadLocal<Span>();

    private ServerTracingFeature(
        DropWizardTracer tracer, 
        String operationName,
        Set<ServerAttribute> tracedAttributes, 
        Set<String> tracedProperties,
        boolean traceAll,
        RequestSpanDecorator decorator
    ) {
        this.tracer = tracer;
        this.operationName = operationName;
        this.tracedAttributes = tracedAttributes;
        this.tracedProperties = tracedProperties;
        this.traceAll = traceAll;
        this.decorator = decorator;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Trace annotation = resourceInfo.getResourceMethod().getAnnotation(Trace.class);
        String operationName = this.operationName;
        if (annotation != null) {
            if (!annotation.operationName().equals("")) {
                operationName = annotation.operationName();
            }
            context.register(new ServerRequestTracingFilter(this.tracer, operationName,
                this.tracedAttributes, this.tracedProperties, this.decorator));
            context.register(new ServerResponseTracingFilter(this.tracer));  
        } else {
            if (traceAll) {
                context.register(new ServerRequestTracingFilter(this.tracer, operationName,
                    this.tracedAttributes, this.tracedProperties, this.decorator));
                context.register(new ServerResponseTracingFilter(this.tracer));
            } 
        }
    }

    /**
     * Returns the Span associated with the active DropWizard request.
     *
     * NOTE: this may return null when there is no active DropWizard request or that request is not traced; moreover,
     * if request processing moves from thread to thread, this mechanism may return null or perhaps even the wrong
     * Span. Use with some caution. If it is possible to explicitly propagate a Span, that will always be more rigorous
     * (though of course it's also often not practical or expedient).
     *
     * @return The Span associated with the active DropWizard request.
     */
    public static Span getThreadLocalRequestSpan() {
        return ServerTracingFeature.threadLocalRequestSpan.get();
    }

    /**
     * Use this class to configure and build a ServerTracingFeature
     */
    public static class Builder {

        private final DropWizardTracer tracer;
        private Set<ServerAttribute> tracedAttributes;
        private Set<String> tracedProperties;
        private boolean traceAll;
        private String operationName;
        private RequestSpanDecorator decorator;

        /**
         * @param tracer to use to trace requests to the server
         */
        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
            this.tracedAttributes = new HashSet<ServerAttribute>();
            this.tracedProperties = new HashSet<String>();
            this.traceAll = true;
            this.operationName = "";
        }

        /**
         * @param tracedAttributes a set of ServerAttributes that you want to tag to spans
         *  created for requests to the server
         * @return Builder configured with added traced attributes
         */
        public Builder withTracedAttributes(Set<ServerAttribute> tracedAttributes) {
            this.tracedAttributes = tracedAttributes;
            return this;
        }

        /**
         * @param properties a set of request properties of the client request to tag 
         *  to spans created for client requests
         * @return Builder configured with added traced properties
         */
        public Builder withTracedProperties(Set<String> properties) {
            this.tracedProperties = properties;
            return this;
        }

        /**
         * By default, all requests to the server all traced. However, if you configure
         * your ServerTracingFeature with trace annotations, then only requests to resource
         * methods annotated with @Trace will be traced.
         * @return Builder configured to use trace annotations
         */
        public Builder withTraceAnnotations() {
            this.traceAll = false;
            return this;
        }

        /**
         * @param decorator an (optional) RequestSpanDecorator which is applied to each [Request, Span] pair.
         */
        public Builder withRequestSpanDecorator(RequestSpanDecorator decorator) {
            this.decorator = decorator;
            return this;
        }

        public Builder withOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        /**
         * @return ServerTracingFeature with the configuration of this Builder
         */
        public ServerTracingFeature build() {
            return new ServerTracingFeature(this.tracer, this.operationName, 
                this.tracedAttributes, this.tracedProperties, this.traceAll, this.decorator);
        }
    }
}
