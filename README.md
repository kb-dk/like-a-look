# like-a-look 

Finding similar images using machine learning

Very much under initial development!
             
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

After a fresh checkout or after the `openapi.yaml` specification has changes, the `api` and the `model` files 
must be generated. This is done by calling 
```
mvn package
```

Jetty is enabled, so testing the webservice can be done by running
Start a Jetty web server with the application:
```
mvn jetty:run
```

The default port is 8080 and the default Hello World service can be accessed at
<http://localhost:8080/java-webapp/api/hello>
where "java-webapp" is your artifactID from above.

The Swagger-UI is available at <http://localhost:8080/java-webapp/api/api-docs?url=openapi.json>
which is the location that <http://localhost:8080/java-webapp/api/> will redirect to.

## Developer information

See the file [DEVELOPER.md](DEVELOPER.md).
