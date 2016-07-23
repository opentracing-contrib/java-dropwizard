######################
DropWizard-Opentracing
######################

This package enables distributed tracing in DropWizard projects via `The OpenTracing Project`_. Once a production system contends with real concurrency or splits into many services, crucial (and formerly easy) tasks become difficult: user-facing latency optimization, root-cause analysis of backend errors, communication about distinct pieces of a now-distributed system, etc. Distributed tracing follows a request on its journey from inception to completion from mobile/browser all the way to the microservices. 

As core services and libraries adopt OpenTracing, the application builder is no longer burdened with the task of adding basic tracing instrumentation to their own code. In this way, developers can build their applications with the tools they prefer and benefit from built-in tracing instrumentation. OpenTracing implementations exist for major distributed tracing systems and can be bound or swapped with a one-line configuration change.

If you want to learn more about the underlying Java API, visit the Java `source code`_.

.. _The OpenTracing Project: http://opentracing.io/
.. _source code: https://github.com/opentracing/opentracing-java

************
Installation
************

Artifacts are available on Maven Central, so you can add this dependency to your to your project as follows:

Maven
=====
.. code-block:: 

    <dependency>
        <groupId>io.opentracing.contrib.dropwizard</groupId>
        <artifactId>dropwizard-opentracing</artifactId>
        <version>0.1.0</version>
    </dependency>

Gradle
======
.. code-block::

    dependencies {
        compile 'io.opentracing.contrib.dropwizard:dropwizard-opentracing:0.1.0'
    }

If you're using a different build tool, use their syntax.

*****
Usage 
*****

Initialize a Tracer
===================

You can use any implementation of an OpenTracing tracer. In your application file, in the run method, instantiate an instance of this tracer wrapped with the DropWizardTracer. You can pass this tracer to resources and around your application either by explicitly passing them in the resource constructor, or using your own implementation of a global context.

.. code-block:: java

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);
    }

Trace Requests to Server
========================

You can trace all requests to your application by registering `ServerTracingFeature` to jersey.* This feature uses the builder pattern, which enables you to integrate tracing with a variety of options as follows:

.. code-block:: java

    import io.opentracing.contrib.dropwizard.DropWizardTracer;
    import io.opentracing.contrib.dropwizard.ServerAttribute; //optional
    import io.opentracing.contrib.dropwizard.ServerTracingFeature;

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);    
        
        // registers filters for tracing
        environment.jersey().register(new ServerTracingFeature
            .Builder(tracer)
            .withTraceAnnotations()
            .withTracedAttributes(someSetOfServerAttributes)
            .withTracedProperties(someSetOfStringPropertyNames)
            .build());
    }

- `withTraceAnnotations` turns on tracing annotations. By default, all requests to your application are traced. However, if tracing annotations are enabled, then only resource methods annotated with @Trace will be traced.

- `withTracedAttributes(Set<ServerAttribute>)` allows you to specify attributes of the request that you wish to be logged or tagged to your spans. All attributes available for tracing are enumerated in `io.opentracing.contrib.dropwizard.ServerAttribute`.

- `withTracedProperties(Set<String>)` allows you to trace custom properties of the request. It takes in a set of property namesthat you wish to trace, and sets tags on the span.

DropWizard 0.7 and older
------------------------

If you're using a version of DropWizard older than 0.8, then the ServerTracingFeature is not supported. Instead, use the ServerRequestTracingFilter and ServerResponseTracingFilter as follows:

.. code-block:: java

    import io.opentracing.contrib.dropwizard.DropWizardTracer;
    import io.opentracing.contrib.dropwizard.ServerAttribute; // optional
    import io.opentracing.contrib.dropwizard.ServerRequestTracingFilter;
    import io.opentracing.contrib.dropwizard.ServerResponseTracingFilter;

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final DropWizardTracer tracer = new DropWizardTracer(someOpenTracingTracer);    
        
        // register the filter for incoming requests
        environment.jersey().register(new ServerRequestTracingFilter
            .Builder(tracer)
            .withTraceAnnotations()
            .withTracedAttributes(someSetOfServerAttributes)
            .withTracedProperties(someSetOfStringPropertyNames)
            .build());

        // register the filter for outgoing responses
        environment.jersey().register(new ServerResponseTracingFilter
            .Builder(tracer)
            .build());
    }

Using @Trace Annotations
------------------------  

To trace a resource, add the annotation @Trace to each method of the resource that you wish to trace. If you wish to set the operation name for a specific resource method (the default is the name of the resource class) then you can add a paramater to @Trace(operationName="New Operation Name"). See the `opentracing documentation`_ on choosing operation names for more information.

**Note:** The @Trace annotations can be used to set a resource method's operation name even when the ServerTracingFeature is configured without withTraceAnnotations. 

.. code-block:: java
    
    import io.opentracing.contrib.dropwizard.Trace;

    @PATH('/some-path')
    @Produces(someType)
    public class SomeResource {

        @GET
        @Trace()
        public String basePath() {
            // do some stuff
            return someString
        }
        
        @POST
        @Trace(operationName="custom_operation_name")
        public void receiveSomething() {
            // do some other stuff
        }

        @GET 
        @Path('some-sub-path')
        public String subPath() {
            // do some stuff
            return someString
        }
    }

In this example, GET and POST requests to '/some-path' will be traced, but GET requests to '/some-path/some-sub-path' will not. The operation name of the span created for the GET request is "SomeResource", while for the POST request is "custom_operation_name".

Trace Client Requests
=====================

If you want to trace outbound requests using Jersey clients, we provide a `ClientTracingFeature` class. This feature also follows the builder pattern. It should be registered to a client or webtarget, and if you want the feature to be able to continue a trace (rather than starting a new trace), then it must be registered within the scope of a resource. See below for example useage.

.. code-block:: java

    @GET
    @Path("/some-path")
    @Trace
    public String someSubresource() {
        WebTarget webTarget = client.target("http://some-url.com/some/request/path");

        ClientTracingFeature feature = new ClientTracingFeature
            .Builder(tracer)
            .withRequest(request)
            .withOperationName(someOperationName)
            .withTracedAttributes(someSetOfClientAttributes)
            .withTracedProperties(someSetOfStringPropertyNames)
            .build();

        feature.registerTo(webTarget);
        Invocation.Builder invocationBuilder = webTarget.request();
        Response response = invocationBuilder.get();
        return someHandler(response);
    }

- `withRequest(Request)` configures the `ClientRequestTracingFilter` continue any the current trace. In this example, since someSubresource is annotated with `@Trace`, the filter must be configured to link the current server span with the outgoing client span; otherwise, all client requests will start new traces. 

- `withOperationName(String)` builds the ClientTracingFeature with an operation name in order to set the name of all spans created by this client. Otherwise, the operation name will default to "Client".

- `withTracedAttributes(Set<ClientAttributes>)` and `withTracedProperties(Set<String>)` operate the same as they do on `ServerTracingFeature`

Accessing the Current Span
==========================

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

Requests and Contexts
=====================

Just like it's up to you to decide how to pass your tracer to the filters, you also are responsible for accessing the current request.  One way to do this is by using `Jersey injection`_ and the @Context annotation. There are several ways to do this, including the methods shown below:

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

**Note:** You'll only need to do this if you want to access the current span, or build a ClientTracingFeature that can continue the current trace.

.. _Jersey injection: https://jersey.java.net/nonav/documentation/latest/user-guide.html#d0e2681
.. _opentracing documentation: http://opentracing.io/spec/#operation-names

*******************
Further Information
*******************

If you’re interested in learning more about the OpenTracing standard, please visit `opentracing.io`_ or `join the mailing list`_. If you would like to implement OpenTracing in your project and need help, feel free to send us a note at `community@opentracing.io`_.

.. _opentracing.io: http://opentracing.io/
.. _join the mailing list: http://opentracing.us13.list-manage.com/subscribe?u=180afe03860541dae59e84153&id=19117aa6cd
.. _community@opentracing.io: community@opentracing.io
