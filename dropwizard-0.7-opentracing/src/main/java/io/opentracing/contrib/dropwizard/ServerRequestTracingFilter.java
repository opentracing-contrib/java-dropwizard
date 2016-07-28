package io.opentracing.contrib.dropwizard;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.TextMapReader;

import io.opentracing.contrib.dropwizard.DropWizardTracer;
import io.opentracing.contrib.dropwizard.ServerAttribute;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.ws.rs.core.MultivaluedMap;

/**
 * When registered to a DropWizard service along with a ServerResponseTracingFilter,
 * this filter creates a span for any requests to the service.
 */
public class ServerRequestTracingFilter implements ContainerRequestFilter {

    private final DropWizardTracer tracer;
    private final Set<ServerAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private final String operationName;

    /**
     * @param tracer to trace requests with
     * @param tracedAttributes any ServiceAttributes to log to spans
     * @param tracedProperties any request properties to log to spans
     */
    private ServerRequestTracingFilter(
        DropWizardTracer tracer,
        String operationName,
        Set<ServerAttribute> tracedAttributes, 
        Set<String> tracedProperties
    ) {
        this.tracer = tracer;
        this.operationName = operationName;
        this.tracedProperties = tracedProperties;
        this.tracedAttributes = tracedAttributes;
    }

    public static class Builder {

        private final DropWizardTracer tracer;
        private Set<ServerAttribute> tracedAttributes = new HashSet<ServerAttribute>();
        private Set<String> tracedProperties = new HashSet<String>();
        private String operationName = "";

        public Builder(DropWizardTracer tracer) {
            this.tracer = tracer;
        }

        public Builder withTracedAttributes(Set<ServerAttribute> attributes) {
            this.tracedAttributes = attributes;
            return this;
        }

        public Builder withTracedProperties(Set<String> properties) {
            this.tracedProperties = properties;
            return this;
        }

        public Builder withOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        public ServerRequestTracingFilter build() {
            return new ServerRequestTracingFilter(this.tracer, this.operationName,
                this.tracedAttributes, this.tracedProperties);
        }
    }
    
    @Override
    public ContainerRequest filter(ContainerRequest request) {
            // for(Object resource : request.getUriInfo().getMatchedResources()) {
            //     this.operationName += resource.getClass().getSimpleName() + " ";
            // }
        String operationName;
        if(this.operationName == "") {
            operationName = request.getRequestUri().toString();
        } else {
            operationName = this.operationName;
        }
        // format the headers for extraction
        Span span;
        MultivaluedMap<String, String> rawHeaders = request.getRequestHeaders();
        final HashMap<String, String> headers = new HashMap<String, String>();
        for(String key : rawHeaders.keySet()){
            headers.put(key, rawHeaders.get(key).get(0));
        }

        // extract the client span
        try {
            SpanContext parentSpan = tracer.getTracer().extract(new TextMapReader() {
                public Iterator<Map.Entry<String, String>> getEntries() {
                    return headers.entrySet().iterator();
                }
            });
            if(parentSpan == null){
                span = tracer.getTracer().buildSpan(operationName).start();
            } else {
                span = tracer.getTracer().buildSpan(operationName).asChildOf(parentSpan).start();
            }
        } catch(IllegalArgumentException e) {
            span = tracer.getTracer().buildSpan(operationName).start();
        }

        // trace attributes
        for(ServerAttribute attribute : this.tracedAttributes) {
            switch(attribute) {
                case ABSOLUTE_PATH: 
                    try { span.setTag("Absolute Path", request.getAbsolutePath().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case ACCEPTABLE_LANGUAGES: 
                    try { span.setTag("Acceptable Languages", request.getAcceptableLanguages().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case ACCEPTABLE_MEDIA_TYPES: 
                    try { span.setTag("Acceptable Media Types", request.getAcceptableMediaTypes().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                 case AUTHENTICATION_SCHEME: 
                    try { span.setTag("Authentication Scheme", request.getAuthenticationScheme()); }
                    catch(NullPointerException npe) {}
                    break;
                case BASE_URI: 
                    try { span.setTag("Base URI", request.getBaseUri().toString()); }
                    catch(NullPointerException npe) {}
                    break;
               case COOKIES:
                    try { span.setTag("Cookies", request.getCookies().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case HEADERS:
                    try { span.setTag("Headers", request.getRequestHeaders().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case IS_SECURE: 
                    try { span.setTag("Is Secure", request.isSecure()); }
                    catch(NullPointerException npe) {}
                    break;
                case LANGUAGE:
                    try { span.setTag("Language", request.getLanguage().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case METHOD:
                    try { span.setTag("Method", request.getMethod()); }
                    catch(NullPointerException npe) {}
                    break;
                case MEDIA_TYPE: 
                    try { span.setTag("Media Type", request.getMediaType().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case PATH:
                    try { span.setTag("Property Names", request.getPath().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                 case QUERY_PARAMETERS: 
                    try { span.setTag("Query Paramters", request.getQueryParameters().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case SECURITY_CONTEXT:
                    try { span.setTag("Security Context", request.getSecurityContext().getAuthenticationScheme()); }
                    catch(NullPointerException npe) {}
                    break;
                case URI:
                    try { span.setTag("URI", request.getRequestUri().toString()); }
                    catch(NullPointerException npe) {}
                    break;
                case USER_PRINCIPAL: 
                    try { span.setTag("User Principal", request.getUserPrincipal().getName()); }
                    catch(NullPointerException npe) {}
                    break;
            }
        }

        // trace properties
        for(String propertyName : this.tracedProperties) {
            Object property = request.getProperties().get(propertyName);
            if(property != null) {
                span.log(propertyName, property);
            }
        }

        // add the new span to the trace
        tracer.addServerSpan(request, span);
        return request;
    }
}