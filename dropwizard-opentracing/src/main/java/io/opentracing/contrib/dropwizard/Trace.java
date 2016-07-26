package io.opentracing.contrib.dropwizard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to use on DropWizard resource methods 
 * that you wish to trace.
 *
 * If you pass in an operation name such as 
 *      Trace(operationName="some_name")
 *  then all spans created for requests to the
 *  annotated resource method will have the 
 *  specified operationName
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Trace {
    String operationName() default "";
}