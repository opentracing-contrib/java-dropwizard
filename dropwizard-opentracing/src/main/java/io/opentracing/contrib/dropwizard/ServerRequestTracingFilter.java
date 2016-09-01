package io.opentracing.contrib.dropwizard;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

/**
 * When registered to a DropWizard service along with a ServerResponseTracingFilter,
 * this filter creates a span for any requests to the service.
 */
public class ServerRequestTracingFilter implements ContainerRequestFilter {

    private final DropWizardTracer tracer;
    private final Set<ServerAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private String operationName;
    private RequestSpanDecorator decorator;

    /**
     * @param tracer to trace requests with
     * @param operationName for any spans created by this filter
     * @param tracedAttributes any ServiceAttributes to log to spans
     * @param tracedProperties any request properties to log to spans
     * @param decorator an optional decorator for the request spans
     */
    public ServerRequestTracingFilter(
        DropWizardTracer tracer,
        String operationName,
        Set<ServerAttribute> tracedAttributes, 
        Set<String> tracedProperties,
        RequestSpanDecorator decorator
    ) {
        this.tracer = tracer;
        this.operationName = operationName;
        this.tracedProperties = tracedProperties;
        this.tracedAttributes = tracedAttributes;
        this.decorator = decorator;
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // set the operation name
        if (this.operationName.equals("")) {
            for (Object resource : requestContext.getUriInfo().getMatchedResources()) {
                this.operationName += resource.getClass().getSimpleName() + " ";
            }
        }

        // format the headers for extraction
        Span span;
        MultivaluedMap<String, String> rawHeaders = requestContext.getHeaders();
        final HashMap<String, String> headers = new HashMap<String, String>();
        for (String key : rawHeaders.keySet()){
            headers.put(key, rawHeaders.get(key).get(0));
        }

        // extract the client span
        try {
            SpanContext parentSpan = tracer.getTracer().extract(
                    Format.Builtin.HTTP_HEADERS,
                    new TextMapExtractAdapter(headers));
            if (parentSpan == null){
                span = tracer.getTracer().buildSpan(this.operationName).start();
            } else {
                span = tracer.getTracer().buildSpan(this.operationName).asChildOf(parentSpan).start();
            }
        } catch(IllegalArgumentException e) {
            span = tracer.getTracer().buildSpan(this.operationName).start();
        }

        // trace attributes
        for (ServerAttribute attribute : this.tracedAttributes) {
            switch(attribute) {
                case ACCEPTABLE_LANGUAGES: 
                    try { span.setTag("Acceptable Languages", requestContext.getAcceptableLanguages().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case ACCEPTABLE_MEDIA_TYPES: 
                    try { span.setTag("Acceptable Media Types", requestContext.getAcceptableMediaTypes().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case COOKIES:
                    try { span.setTag("Cookies", requestContext.getCookies().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case DATE:
                    try { span.setTag("Date", requestContext.getDate().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case ENTITY_STREAM:
                    try { span.log("Entity Stream", requestContext.getEntityStream()); }
                    catch(NullPointerException npe) {}
                    break;
                case HEADERS:
                    try { span.setTag("Headers", requestContext.getHeaders().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case LANGUAGE:
                    try { span.setTag("Language", requestContext.getLanguage().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case CONTENT_LENGTH:
                    try { span.setTag("Content Length", requestContext.getLength()); }
                    catch(NullPointerException npe) {}
                    break;
                case METHOD:
                    try { span.setTag("Method", requestContext.getMethod()); }
                    catch(NullPointerException npe) {}
                    break;
                case MEDIA_TYPE: 
                    try { span.setTag("Media Type", requestContext.getMediaType().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case PROPERTY_NAMES:
                    try { span.setTag("Property Names", requestContext.getPropertyNames().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case SECURITY_CONTEXT:
                    try { span.setTag("Security Context", requestContext.getSecurityContext().getAuthenticationScheme()); }
                    catch(NullPointerException npe) {}
                    break;
                case URI:
                    try { span.setTag("URI", requestContext.getUriInfo().getAbsolutePath().toString()); }
                    catch(NullPointerException npe) {}
                    break;
            }
        }

        // trace properties
        for (String propertyName : this.tracedProperties) {
            Object property = requestContext.getProperty(propertyName);
            if (property != null) {
                span.log(propertyName, property);
            }
        }

        if (this.decorator != null) {
            this.decorator.decorate(requestContext, span);
        }

        // add the new span to the trace
        tracer.addServerSpan(requestContext.getRequest(), span);
        ServerTracingFeature.threadLocalRequestSpan.set(span);
    }
}
