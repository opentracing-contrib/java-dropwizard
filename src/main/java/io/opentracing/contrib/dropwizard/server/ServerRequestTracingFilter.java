package io.opentracing.contrib.dropwizard.server;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.TextMapReader;

import io.opentracing.contrib.dropwizard.DropWizardTracer;
import io.opentracing.contrib.dropwizard.server.ServerAttribute;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;


public class ServerRequestTracingFilter implements ContainerRequestFilter {

    private final DropWizardTracer tracer;
    private final Set<ServerAttribute> tracedAttributes;
    private final Set<String> tracedProperties;
    private String operationName;

    public ServerRequestTracingFilter(
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
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // set the operation name
        if(this.operationName.equals("")) {
            for(Object resource : requestContext.getUriInfo().getMatchedResources()) {
                this.operationName += resource.getClass().getSimpleName() + " ";
            }
        }

        // format the headers for extraction
        Span span;
        MultivaluedMap<String, String> rawHeaders = requestContext.getHeaders();
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
                span = tracer.getTracer().buildSpan(this.operationName).start();
            } else {
                span = tracer.getTracer().buildSpan(this.operationName).asChildOf(parentSpan).start();
            }
        } catch(IllegalArgumentException e) {
            span = tracer.getTracer().buildSpan(this.operationName).start();
        }

        // trace attributes
        for(ServerAttribute attribute : this.tracedAttributes) {
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
        for(String propertyName : this.tracedProperties) {
            Object property = requestContext.getProperty(propertyName);
            if(property != null) {
                span.log(propertyName, property);
            }
        }

        // add the new span to the trace
        tracer.addServerSpan(requestContext.getRequest(), span);
    }
}