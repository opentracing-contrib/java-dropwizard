package io.opentracing.contrib.dropwizard;

/*
 * Attributes that can be traced for Server Requests
 */
public enum ServerAttribute {
    ABSOLUTE_PATH,
    ACCEPTABLE_LANGUAGES,
    ACCEPTABLE_MEDIA_TYPES,
    AUTHENTICATION_SCHEME,
    BASE_URI,
    COOKIES,
    HEADERS,
    IS_SECURE,
    LANGUAGE,
    METHOD,
    MEDIA_TYPE,
    PATH,
    QUERY_PARAMETERS,
    SECURITY_CONTEXT,
    URI,
    USER_PRINCIPAL
}