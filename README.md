# like-a-look 

Finding similar images using machine learning

## Current status

Very much under initial development!

It is possible to issue a call to a service, which will respond with dummy data.
             
## Roadmap

 * First version is intended to be your basic "Generate fingerprints from images using ImageNet,
   then find similary images using these fingerprints".
 * Second version aims for more flexibility in the workflow by preprocessing the images (might be used for 
   the internal project DANER).
 * Third version is about finding details on images: Mark an area on an image and find other images where part
   of the image is similar to the marked area.

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
