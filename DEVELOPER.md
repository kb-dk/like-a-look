# Developer documentation

This project is build from the [java webapp template](https://sbprojects.statsbiblioteket.dk/stash/projects/ARK/repos/like-a-look-template/browse)
from the Royal Danish Library.

The information in this document is aimed at developers that are not proficient in the java webapp template, Jetty, 
Tomcat deployment or OpenAPI.

## Initial use

After a fresh checkout or after the `openapi.yaml` specification has changed, the `api` and the `model` files 
must be (re)generated. This is done by calling 
```
mvn package
```

Jetty is enabled, so testing the webservice can be done by running
Start a Jetty web server with the application:
```
mvn jetty:run
```

The default port is 8080 and the default Hello World service can be accessed at
<http://localhost:8080/like-a-look/api/hello>
where "like-a-look" is your artifactID from above.

The Swagger-UI is available at <http://localhost:8080/like-a-look/api/api-docs?url=openapi.json>
which is the location that <http://localhost:8080/like-a-look/api/> will redirect to.

## java webapp template structure

Configuration of the project is handled with [YAML](https://en.wikipedia.org/wiki/YAML). During development it is
located at `conf/like-a-look.yaml` (with the `like-a-look`-part being replaced with the application ID) and when
the project is started using Jetty, this is the configuration that will be used.

Access to the configuration is through the static class at `src/main/java/dk/kb/template/config/ServiceConfig.java`.



## Jetty

Jetty is a servlet container (like Tomcat) that is often used for testing during development.

This project can be started with `mvn jetty:run`, which will expose a webserver with the implemented service at port 8080.
If it is started in debug mode from an IDE (normally IntelliJ IDEA), breakpoints and all the usual debug functionality
will be available.

## Tomcat

Tomcat is the default servlet container for the Royal Danish Library and as deployment to Tomcat must be tested before
delivering the project to Operations. As of 2021, Tomcat 9 is used for Java 11 applications.

A [WAR](https://en.wikipedia.org/wiki/WAR_(file_format))-file is generated with `mvn package` and can be deployed
directly into Tomcat, although this will log to `catalina.out` and use the developer configuration YAML.

Proper deployment is done by

* Creating and adjusting production specific copies of `conf/<application-ID>.yaml` and `conf/ocp/logback.xml`, 
  preferably in `$HOME/conf/`.
* Adjusting the the paths to `docBase`, `<application-ID>.yaml` and `logback.xml` in `conf/ocp/<application-ID>.xml`
  and copying `conf/ocp/<application-ID>.xml` to the Tomcat folder `conf/Catalina/localhost/`

## OpenAPI 1.3 (aka Swagger)

[OpenAPI 1.3](https://swagger.io/specification/) generates interfaces and skeleton code for webservices.
It also generates online documentation, which includes sample calls and easy testing of the endpoints.

Everything is defined centrally in the file [src/main/openapi/openapi.yaml](src/main/openapi/openapi.yaml).
IntelliJ IDEA has a plugin for editing OpenAPI files that provides a semi-live preview of the generated GUI and
the online [Swagger Editor](https://editor.swagger.io/) can be used by copy-pasting the content of `openapi.yaml`.


The interfaces and models generated from the OpenAPI definition are stored in `target/generated-sources/`.
They are recreated on each `mvn package`.

Skeleton classes are added to `/src/main/java/${project.package}/api/impl/` but only if they are not already present. 
A reference to the classes must be added manually to `/src/main/java/${project.package}/webservice/Application` or its equivalent.

A common pattern during initial definition of the `openapi.yaml` is to delay implementation and recreate the skeleton
implementation files on each build. This can be done by setting `generateOperationBody` in the `pom.xml` to `true`.

**Tip:** If the `openapi.yaml` is changed a lot during later development of the application, it might be better to have 
`<generateOperationBody>true</generateOperationBody>` in `pom.xml` and add the implementation code to manually created
classed (initially copied from the OpenAPI-generated skeleton impl classes). When changes to `openapi.yaml` results in
changed skeleton implementation classes, the changes can be manually ported to the real implementation classes.

**Note:** The classes in `/src/main/java/${project.package}api/impl/` will be instantiated for each REST-call.
Persistence between calls must be handled as statics or outside of the classes.

### OpenAPI and exceptions

When an API end point shall return anything else than the default response (HTTP response code 200),
this is done by throwing an exception.

See how we map exceptions to responsecodes in [ServiceExceptionMapper](./src/main/java/dk/kb/webservice/ServiceExceptionMapper.java) 

See [ServiceException](./src/main/java/dk/kb/webservice/exception/ServiceException.java) and its specializations for samples.



