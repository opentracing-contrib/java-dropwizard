package io.opentracing.contrib.dropwizard;

import com.sun.jersey.spi.container.ContainerRequest;
import io.opentracing.Span;

/**
 * RequestSpanDecorator allows its user to make arbitrary changes (e.g., Span tags or logs) to the Span associated with
 * a DropWizard request.
 */
public interface RequestSpanDecorator {
    /**
     * Implementations make arbitrary changes to the Span associated with a RequestContext.
     *
     * @param request An incoming request
     * @param span The Span already associated with the given RequestContext
     */
    void decorate(ContainerRequest request, Span span);
}
