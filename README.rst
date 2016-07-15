######################
DropWizard-Opentracing
######################

This module provides functionality for tracing requests in DropWizard using the OpenTracing API. 

Installation
============

Artifacts can be found at...

Useage 
======

Initialize a Tracer
*******************

You can use any implementation of an OpenTracing tracer. In your application file, in the run method, instantiate an instance of this tracer wrapped with the DropWizardTracer.

.. code-block: java

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(new JRETracer(new Options("{your_lightstep_token}")));
    }

Trace All Requests
******************

You can trace all requests to your application by registering `ServerRequestTracingFilter` and `ServerResponseTracingFilter` to jersey, as shown below.

.. code-block: java

    import io.opentracing.dropwizard.ServerRequestTracingFilter;
    import io.opentracing.dropwizard.ServerResponseTracingFilter;
    import io.opentracing.dropwizard.DropWizardTracer;

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);    
        
        // register the tracing filters
        environment.jersey().register(new ServerRequestTracingFilter(tracer));
        environment.jersey().register(new ServerResponseTracingFilter(tracer));
    }

Trace Specific Requests
***********************

If you want to instead choose the requests to trace rather than tracing every request, then you can use the `@Trace` annotation along with our `ServerTracingFeature`, which dynamically adds server tracing filters to annotated resource classes.

Your application should have the following lines of code to trace specific requests:

.. code-block: java
    
    import io.opentracing.dropwizard.ServerRequestTracingFilter;
    import io.opentracing.dropwizard.ServerResponseTracingFilter;
    import io.opentracing.dropwizard.DropWizardTracer;

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);    
        
        // register the dynamic feature that traces resources annotated with @Trace
        environment.jersey().register(new ServerTracingFeature(tracer));
    }   

To trace a whole resource, add the following annotation to the resource:

.. code-block: java
    
    import io.opentracing.dropwizard.Trace;

    @PATH('/some-path')
    @Produces(someType)
    @Trace // traces all requests to this resource
    public class SomeResource {

        @GET
        public String basePath() {
            // do some stuff
            return someString
        }

        @GET 
        @Path('some-sub-path')
        public String subPath() {
            // do some stuff
            return someString
        }

        @POST
        public void receiveSomething() {
            // do some other stuff
        }
    }

However, to trace only certain subresources in the resource, add annotations to only those subresource methods. For example, to only trace `receiveSomething`, add the following annotation:

.. code-block: javas
    
    import io.opentracing.dropwizard.Trace;

    @PATH('/some-path')
    @Produces(someType)
    public class SomeResource {

        @GET
        public String basePath() {
            // do some stuff
            return someString
        }

        @GET 
        @Path('some-sub-path')
        public String subPath() {
            // do some stuff
            return someString
        }

        @POST
        @Trace // traces only post requests 
        public void receiveSomething() {
            // do some other stuff
        }
    }

Accessing the Current Span
**************************

Sometimes you may want log, tag, or create a child span from the current span, which means that you need to be able to access the span. In order to do this, you can call `tracer.getSpan(request)` using the current request state. 

One way that you can access the request state is by using injection to reset the request whenever the resource is called. To do so, add the following lines of code to your resource:

.. code-block: java

    @Context
    private Request request = null;
