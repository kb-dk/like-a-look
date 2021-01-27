package dk.kb.likealook.api.impl;

import dk.kb.likealook.api.LikeALookApi;
import dk.kb.likealook.model.WholeImageDto;
import dk.kb.webservice.exception.InternalServiceException;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import dk.kb.webservice.exception.ServiceException;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// TODO: Accept JPEG & PNG, directly or base64. See teams for sample of base64
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
    public List<String> collectionsGet() throws ServiceException {
        return Collections.singletonList("daner");
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
    public List<WholeImageDto> findSimilarWhole( Attachment imageDetail, String collection, String sourceID, Integer maxMatches) throws ServiceException {
        if (collection != null && !collection.isEmpty() && !"daner".equals(collection)) {
            throw new InvalidArgumentServiceException("The collection '" + collection + "' is unknown");
        }
        log.info("findSimilarWhole(..., collection=" + collection + ", sourceID=" + sourceID + ", maxMatches=" + maxMatches + ") called");
        List<WholeImageDto> response = new ArrayList<>();
        Random r = new Random(("" + sourceID).hashCode());
        double distance = r.nextDouble();
        for (int i = 0 ; i < (maxMatches == null ? 10 : maxMatches) ; i++) {
            WholeImageDto item = new WholeImageDto();
            distance += r.nextDouble();
            if (sourceID != null) {
                item.setSourceID(sourceID);
            }
            item.setImageID("SampleImage_" + i + "_" + distance);
            item.setDistance(distance);
            item.setUrl(String.format(Locale.ROOT, "https://placekitten.com/g/%d/%d", 200+((i+1)/2), 300+(i/2)));
            response.add(item);
        }
        return response;
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
