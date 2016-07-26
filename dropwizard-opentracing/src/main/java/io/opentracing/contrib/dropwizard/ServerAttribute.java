package io.opentracing.contrib.dropwizard;

/*
 * Attributes that can be traced for Server Requests
 */
public enum ServerAttribute {
    ACCEPTABLE_LANGUAGES,
    ACCEPTABLE_MEDIA_TYPES,
    COOKIES,
    DATE,
    ENTITY_STREAM,
    HEADERS,
    LANGUAGE,
    CONTENT_LENGTH,
    METHOD,
    MEDIA_TYPE,
    PROPERTY_NAMES,
    SECURITY_CONTEXT,
    URI
}