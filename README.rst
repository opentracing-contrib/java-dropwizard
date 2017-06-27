**DEPRECATED** Please use `OpenTracing Java JAX-RS instrumentation`_

.. _OpenTracing Java JAX-RS instrumentation: https://github.com/opentracing-contrib/java-jaxrs

######################
DropWizard-OpenTracing
######################

This package enables distributed tracing in DropWizard projects via `The OpenTracing Project`_. Once a production system contends with real concurrency or splits into many services, crucial (and formerly easy) tasks become difficult: user-facing latency optimization, root-cause analysis of backend errors, communication about distinct pieces of a now-distributed system, etc. Distributed tracing follows a request on its journey from inception to completion from mobile/browser all the way to the microservices. 

As core services and libraries adopt OpenTracing, the application builder is no longer burdened with the task of adding basic tracing instrumentation to their own code. In this way, developers can build their applications with the tools they prefer and benefit from built-in tracing instrumentation. OpenTracing implementations exist for major distributed tracing systems and can be bound or swapped with a one-line configuration change.

If you want to learn more about the underlying Java API, visit the Java `source code`_.

.. _The OpenTracing Project: http://opentracing.io/
.. _source code: https://github.com/opentracing/opentracing-java

******************
DropWizard Version
******************

DropWizard 0.8+ has upgraded to Jersey 2.0, so please choose the DropWizard-OpenTracing package that is compatible with your version of DropWizard.

- **Version < 0.8.0 :** dropwizard-0.7-opentracing
- **Version >= 0.8.0 :** dropwizard-opentracing

*******************
Further Information
*******************

If you’re interested in learning more about the OpenTracing standard, please visit `opentracing.io`_ or `join the mailing list`_. If you would like to implement OpenTracing in your project and need help, feel free to send us a note at `community@opentracing.io`_.

.. _opentracing.io: http://opentracing.io/
.. _join the mailing list: http://opentracing.us13.list-manage.com/subscribe?u=180afe03860541dae59e84153&id=19117aa6cd
.. _community@opentracing.io: community@opentracing.io

******************
Publishing/Deploying Maven artifacts
******************

Export OSSRH_USERNAME, OSSRH_PASSWORD, and GPG_PASSWORD environment variables, then run `mvn -s ../settings.xml deploy` from each of the pre- and post-0.7 subdirectories.
