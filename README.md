# like-a-look 

Finding similar images using machine learning

## Current status

Very much under development.
             
## Requirements

 * Java 11
 * Maven 3

## Using the project

A war file is generated with the standard 
```
mvn package
```
use `conf/ocp/like-a-look.xml` as template for the setup.

Jetty is enabled, so testing the webservice can be done by running
```
mvn jetty:run
```

The default port is 8080 and the ping can be accessed at
<http://localhost:8080/like-a-look/api/ping>

The Swagger-UI is available at <http://localhost:8080/like-a-look/api/>.

See the file [DEVELOPER.md](DEVELOPER.md) if you are a developer and want to work with this.
