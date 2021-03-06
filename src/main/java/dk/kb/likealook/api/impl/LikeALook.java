package dk.kb.likealook.api.impl;

import dk.kb.likealook.api.LikeALookApi;
import dk.kb.likealook.config.ServiceConfig;
import dk.kb.likealook.model.CollectionDto;
import dk.kb.likealook.model.SimilarResponseDto;
import dk.kb.likealook.model.SubjectDto;
import dk.kb.likealook.util.JSONArrayStream;
import dk.kb.webservice.exception.InternalServiceException;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import dk.kb.webservice.exception.ServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// TODO: Accept JPEG & PNG, directly or base64. See teams for sample of base64
// TODO: Consider using https://github.com/tzolov/mtcnn-java for face detection
// TODO: Inspiraton: https://medium.com/clique-org/how-to-create-a-face-recognition-model-using-facenet-keras-fd65c0b092f1
// TODO: Inspiration: https://machinelearningmastery.com/how-to-develop-a-face-recognition-system-using-facenet-in-keras-and-an-svm-classifier/
/**
 * The real implementation. Copy changes after openapi.yaml-updates from LikeALookAPiServiceImpl.
 */
public class LikeALook implements LikeALookApi {
    private Logger log = LoggerFactory.getLogger(this.toString());

    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    @Context
    private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;

    public static final String DEFAULTCOLLECTION_KEY = ".likealook.similar.collections.default";
    public static final String DEFAULTCOLLECTION_DEFAULT = "daner_mock";

    /**
     * List the available collections
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<CollectionDto> collectionsGet() throws ServiceException {
        enableCORS();
        return Arrays.asList(
                new CollectionDto().id("daner_mock").description(
                        "Used for testing calls to the similar-service. " +
                        "Delivers randomly selected profiles from the DANER collection"),
                new CollectionDto().id("daner_v1").description(
                        "Finds the most similar portraits in the DANER collection. " +
                        "Uses the Wolfram engine through the Wolfram service framework for face detection, feature extraction and and similarity distance"),
                new CollectionDto().id("daner_v2").description(
                        "Finds the most similar portraits in the DANER collection. " +
                        "Uses the Wolfram engine called directly from Java for face detection, feature extraction and and similarity distance")
        );
    }

    /**
     * Request images similar to the uploaded image
     * 
     * @param imageDetail: The image to use as source for the similarity search. JPEG only
     * 
     * @param collection: The collection to search for similar images. If none is specified, the default collection will be used
     * 
     * @param sourceID: Optional ID for the image, used for tracking &amp; debugging
     * 
     * @param maxMatches: The maximum number of similar images to return
     * 
     * @return <ul>
      *   <li>code = 200, message = "An array of metadata for similar images, including URLs for the images", response = WholeImageDto.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public SimilarResponseDto findSimilarWhole( Attachment imageDetail, String collection, String sourceID, Integer maxMatches) throws ServiceException {
        if (collection == null || collection.isBlank() || "daner".equals(collection)) { // daner for backwards compatibility
            collection = ServiceConfig.getConfig().getString(DEFAULTCOLLECTION_KEY, DEFAULTCOLLECTION_DEFAULT);
        }

        enableCORS();
        switch (collection) {
            case "daner_mock":
                return DANERService.findSimilar(collection, null, sourceID == null || sourceID.isBlank() ? "mockSource" : sourceID, maxMatches);
            case "daner_v1":
            case "daner_v2": {
                InputStream imageStream;
                try {
                    imageStream = imageDetail.getDataHandler().getInputStream();
                } catch (IOException e) {
                    String message = "findSimilarDANER encountered IOException while getting InputStream for image";
                    log.warn(message, e);
                    throw new InvalidArgumentServiceException(message, e);
                }

                return DANERService.findSimilar(collection, imageStream, sourceID, maxMatches);
            }
            default: throw new InvalidArgumentServiceException(
                    "The collection '" + collection + "' is unsupported. " +
                    "Valid collections are daner_mock, daner_v1 and daner_v2");
        }
    }

    /**
     * Detect human faces in the uploaded image
     *
     * @param imageDetail: The image to use as source for face detection
     *
     * @param method: The method used for face detecton
     *
     * @param sourceID: Optional ID for the image, used for tracking &amp; debugging
     *
     * @return <ul>
      *   <li>code = 200, message = "An array of boxes for the detected faces", response = BoxDto.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput detectFaces( Attachment imageDetail, String method, String sourceID, String response) throws ServiceException {
        sourceID = sourceID != null ? sourceID :
                imageDetail.getContentDisposition() != null ? imageDetail.getContentDisposition().getFilename() :
                        null;
        FaceHandler.METHOD realMethod = FaceHandler.METHOD.valueOfWithDefault(method);
        FACE_RESPONSE realResponse = FACE_RESPONSE.valueOfWithDefault(response);

        try {
            enableCORS();
            switch (realResponse) {
                case jpeg: {
                    byte[] faceImage;
                    faceImage = FaceHandler.faceOverlay(imageDetail.getDataHandler().getInputStream(), realMethod, sourceID);
                    return (out) -> IOUtils.copy(new ByteArrayInputStream(faceImage), out);
                }
                case json: {
                    return new JSONArrayStream(FaceHandler.detectFaces(
                            imageDetail.getDataHandler().getInputStream(), realMethod, sourceID));
                }
                default: throw new InvalidArgumentServiceException("The method '" + method + "' is not supported");
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    public enum FACE_RESPONSE {json, jpeg;
        public static FACE_RESPONSE valueOfWithDefault(String response) {
            return response == null || response.isEmpty() ? json : valueOf(response);
        }
    }

    /**
     * Detect what the subject of the image, e.g. a picture with a cat on a couch should return \&quot;cat\&quot; and \&quot;couch\&quot;
     *
     * @param imageDetail: The image to use as source for subject detection
     *
     * @param method: The method used for subject detection. \\\&quot;Inception3\\\&quot; is Tensorflow Inception 3 trained on ImageNet data
     *
     * @param sourceID: Optional ID for the image, used for tracking &amp; debugging
     *
     * @param maxMatches: The maximum number of detected subjects to return
     *
     * @return <ul>
      *   <li>code = 200, message = "The detected subjects together with the calculated confidence of correct detection", response = SubjectDto.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<SubjectDto> detectSubjects( Attachment imageDetail, String method, String sourceID, Integer maxMatches)
            throws ServiceException {
        sourceID = sourceID != null ? sourceID :
                imageDetail.getContentDisposition() != null ? imageDetail.getContentDisposition().getFilename() :
                        null;
        SubjectHandler.METHOD realMethod = SubjectHandler.METHOD.valueOfWithDefault(method);
        maxMatches = maxMatches == null ? 10 : maxMatches;

        try {
            enableCORS();
            return SubjectHandler.detectSubjects(
                    imageDetail.getDataHandler().getInputStream(), realMethod, sourceID, maxMatches);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * Deliver a static resource (typically an image)
     *
     * @param collection: The collection that the resource belongs to, e.g. \&quot;faces\&quot;.
     *
     * @param id: The ID of the resource, e.g. \&quot;image_34323.jpg\&quot;.
     *
     * @return <ul>
      *   <li>code = 200, message = "The requested resource", response = File.class</li>
      *   <li>code = 400, message = "Invalid Argument", response = String.class</li>
      *   <li>code = 404, message = "File Not Found", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput getResource(String collection, String id) throws ServiceException {
        log.debug("Resolving resource '" + collection + "/" + id + "'");
        try {
            InputStream resource = ResourceHandler.getResource(collection + "/" + id);
            if (resource == null) {
                throw new NotFoundException(
                        "The resource from collection '" + collection + "' with id '" + id + "' could not be located. " +
                        "Collection exists: " + ResourceHandler.hasCollection(collection));
            }
            if (id.toLowerCase(Locale.ROOT).endsWith(".jpg") || id.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                System.out.println("Setting JPEG");
                httpServletResponse.setHeader(HttpHeaders.CONTENT_TYPE, "image/jpeg");
            }
            enableCORS();
            //httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + id + "\"");
            return (out) -> IOUtils.copy(resource, out);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void enableCORS() {
        if (httpServletResponse == null) {
            return; // Unit testing does not initialise this
        }
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization");
    }

    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 406, message = "Not Acceptable", response = ErrorDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        enableCORS();
        return "pong";
    }


    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.webservice.ServiceExceptionMapper
    */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }

    @Override
    @GET
    @Path("/")
    public Response redirect(@Context MessageContext request){
        String path = request.get("org.apache.cxf.message.Message.PATH_INFO").toString();
        if (path != null && !path.endsWith("/")){
            path = path + "/";
        }
        return Response.temporaryRedirect(URI.create("api-docs?url=" + path + "openapi.yaml")).build();
    }

}
