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
-------------------

You can use any implementation of an OpenTracing tracer. In your application file, in the run method, instantiate an instance of this tracer wrapped with the DropWizardTracer. You can pass this tracer to resources and around your application either by explicitly passing them in the resource constructor, or using your own implementation of a global context.

.. code-block:: java

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);
    }

Trace Requests to Server
------------------------

You can trace all requests to your application by registering `ServerTracingFeature` to jersey. This feature uses the builder pattern, which enables you to integrate tracing with a variety of options as follows:

.. code-block:: java

    import io.opentracing.dropwizard.ServerTracingFeature;
    import io.opentracing.dropwizard.DropWizardTracer;

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);    
        
        // registers filters for tracing
        environment.jersey().register(new ServerTracingFeature
            .Builder(tracer)
            .withTraceAnnotations()
            .withTracedAttributes(someSetOfAttributes)
            .withTracedProperties(someSetOfPropertyNames)
            .build());
    }

- `withTraceAnnotations` turns on tracing annotations. By default, all requests to your application are traced. However, if tracing annotations are enabled, then only resource methods annotated with @Trace will be traced.
- `withTracedAttributes` allows you to specify attributes of the request that you wish to be logged or tagged to your spans. It takes in a `Set<ServerAttribute>`, and all attributes available for tracing are enumerated in `io.opentracing.contrib.dropwizard.ServerAttribute`.
- `withTracedProperties` allows you to trace custom properties of the request. It takes in a `Set<String>` with the names of the properties you wish to trace, and sets tags on the span.

Using @Trace Annotations
~~~~~~~~~~~~~~~~~~~~~~~~  

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
---------------------

If you want to trace outbound requests using Jersey clients, we provide a `ClientTracingFeature` class. This feature also follows the builder pattern. See below for example useage.

.. code-block:: java

    @GET
    @Path("/some-path")
    @Trace
    public String someSubresource() {
        WebTarget webTarget = client.target("http://localhost:8080/hello-world/");
        ClientTracingFeature feature = new ClientTracingFeature
            .Builder(tracer)
            .withRequest(request)
            .withTracedAttributes(new HashSet<ClientAttribute>(Arrays.asList(ClientAttribute.URI, ClientAttribute.LANGUAGE)))
            .withTracedProperties(new HashSet<String>(Arrays.asList("custom_tag")))
            .build();
        feature.registerTo(webTarget);
        Invocation.Builder invocationBuilder = webTarget.request();
        Response response = invocationBuilder.get();
        return "Result: " + response;
    }

The `ClientRequestTracingFilter` can be configured with `withRequest(request)` in order to link this client's spans with the current span. In this example, since someSubresource is annotated with `@Trace`, the filter must be configured to continue the current trace; otherwise, all client requests will start new traces. 

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

Note on Contexts
****************

Just like it's up to you to decide how you want to pass your tracer to the filters, you also are responsible for accessing the current request. One way to do this is by using Jersey `injection`_ and the @Context annotation. There are several ways to do this, including the methods shown below:

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

**Note:** You'll only need to do this if you want to access the current span, or build a ClientTracingFeature that can continue the current trace (instead of starting a new one).

.. _injection: https://jersey.java.net/nonav/documentation/latest/user-guide.html#d0e2681
