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

.. code-block:: java

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);
    }

Trace All Requests to Server
****************************

You can trace all requests to your application by registering `ServerRequestTracingFilter` and `ServerResponseTracingFilter` to jersey, as shown below.

.. code-block:: java

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

Trace Specific Requests to Server
*********************************

If you want to instead choose the requests to trace rather than tracing every request, then you can use the `@Trace` annotation along with our `ServerTracingFeature`, which dynamically adds server tracing filters to annotated resource classes.

Your application should have the following lines of code to trace specific requests:

.. code-block:: java
    
    import io.opentracing.dropwizard.ServerRequestTracingFilter;
    import io.opentracing.dropwizard.ServerResponseTracingFilter;
    import io.opentracing.dropwizard.DropWizardTracer;

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);    
        
        // register the dynamic feature that traces resources annotated with @Trace
        environment.jersey().register(new ServerTracingFeature(tracer));
    }   

To trace a resource, add the annotation @Trace to each method of the resource that you wish to trace:

.. code-block:: java
    
    import io.opentracing.dropwizard.Trace;

    @PATH('/some-path')
    @Produces(someType)
    public class SomeResource {

        @GET
        @Trace
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
        @Trace
        public void receiveSomething() {
            // do some other stuff
        }
    }

In this example, GET and POST requests to '/some-path' will be traced, but GET requests to '/some-path/some-sub-path' will not.

Trace Client Requests
*********************

If you want to trace outbound requests using Jersey clients, we provide ClientRequestTracingFilter and ClientResponseTracingFilter to do this. You can either register these filters on the client itself, or for specific WebTargets (see the `Jersey Client`_ documentation for more detailed instructions on registering client filters).

.. _Jersey Client: https://jersey.java.net/nonav/documentation/latest/user-guide.html#client

You must register both filters (for this example, we'll register them to the client) as follows:

.. code-block:: java

    @GET
    @Path("/make-request")
    @Trace
    public String someSubresource() {
        Client client = ClientBuilder.newClient()
            .register(new ClientRequestTracingFilter(tracer).withContinuedTrace(request))
            .register(new ClientResponseTracingFilter(tracer));
        WebTarget webtarget = client.target("http://target-site.com/some/request/path");
        Invocation.Builder invocationBuilder = webtarget.request();
        Response response = invocationBuilder.get();
        return formatOutput(response);
    }

The `ClientRequestTracingFilter` can be configured with `withContinuedTrace(request)` in order to link this client's spans with the current span. In this example, since someSubresource is annotated with `@Trace`, the filter must be configured to continue the current trace; otherwise, all client requests will start new traces. 

Accessing the Current Span
**************************

Sometimes you may want log, tag, or create a child span from the current span, which means that you need to be able to access the span. In order to do this, you can call `tracer.getSpan(request)` using the current request state. In order to perform OpenTracing Tracer operations, such as buildSpan(), you can call tracer.getTracer(), which will return the DropWizardTracer's underlying io.opentracing.Tracer.

One way that you can access the request state is by using injection to reset the request whenever the resource is called. To do so, add the following lines of code to your resource:

.. code-block:: java

    @Context
    private Request request = null;

And to perform operations on the current span:

.. code-block:: java

    @GET
    @Path('/some-request')
    public void someResourceFunc(){
        // get the span
        Span span = tracer.getSpan(request);

        // log something
        span.log("event", payload);

        // set a tag
        span.set_tag("tag", payload);

        // create a child span
        Span childSpan = tracer.getTracer()
            .buildSpan("some operation name")
            .withParent(span)
            .start();

        // remember to finish any spans that you manually create
        childSpan.finish();
    }

Note on Passing Tracers and Contexts
************************************

It's up to you to decide how you want to pass your tracer to the filters, but one method you might choose is to explicitly add the tracer to the constructor parameter for your resource class, and set it as a property of the resource. When you create your resources in your application's `run()` method, you'll initialize them with the tracer. **Note:** You'll only need to do this if you want to access the current span, or create a client with tracing filters.

For these purposes, you'll also often have to access the current request context. One way to do this is by using Jersey `injection`_ and the @Context annotation. There are several ways to do this, including the methods shown below:

.. code-block:: java
    
    @Path('/some-path')
    public class SomeResource() {

        // when this resource is initialized, request will be injected with the current request
        @Context
        private Request request = null

        // if you only need the current request in one subresource, you can pass it in directly
        public void someSubresource(@Context Request request) {
            ...
        }
    }

.. _injection: https://jersey.java.net/nonav/documentation/latest/user-guide.html#d0e2681
