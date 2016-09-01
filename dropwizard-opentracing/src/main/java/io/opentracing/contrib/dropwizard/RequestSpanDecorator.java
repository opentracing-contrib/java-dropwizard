package io.opentracing.contrib.dropwizard;

import io.opentracing.Span;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * RequestSpanDecorator allows its user to make arbitrary changes (e.g., Span tags or logs) to the Span associated with
 * a DropWizard request.
 */
public interface RequestSpanDecorator {
    /**
     * Implementations make arbitrary changes to the Span associated with a RequestContext.
     *
     * @param requestContext An incoming request's context
     * @param span The Span already associated with the given RequestContext
     */
    void decorate(ContainerRequestContext requestContext, Span span);
}
