package dk.kb.likealook.api.impl;

import dk.kb.likealook.api.*;
import java.util.ArrayList;
import dk.kb.likealook.model.BoxDto;
import dk.kb.likealook.model.ErrorDto;
import java.io.File;
import dk.kb.likealook.model.ImageDto;
import java.util.List;
import java.util.Map;
import dk.kb.likealook.model.PersonDto;
import dk.kb.likealook.model.SimilarResponseDto;
import dk.kb.likealook.model.SubjectDto;

import dk.kb.webservice.exception.ServiceException;
import dk.kb.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * like-a-look
 *
 * <p>Experimental service for finding similar images in collections. 
 *
 */
public class LikeALookApiServiceImpl implements LikeALookApi {
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
        // TODO: Implement...
    
        
        try{ 
            List<String> response = new ArrayList<>();
        response.add("v9e0r0tz");
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Detect human faces in the uploaded image
     * 
     * @param image: The image to use as source for face detection
     * 
     * @param method: The method used for face detection
     * 
     * @param sourceID: Optional ID for the image, used for tracking &amp; debugging
     * 
     * @param response: The response format
     * 
     * @return <ul>
      *   <li>code = 200, message = "The detected faces either as a JSON structure with coordinates or the input image with overlays", response = BoxDto.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public javax.ws.rs.core.StreamingOutput detectFaces( Attachment imageDetail, String method, String sourceID, String response) throws ServiceException {
        // TODO: Implement...
    
        
        try{ 
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"filename.ext\"");
            return output -> output.write("Magic".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Detect what the subject of the image, e.g. a picture with a cat on a couch should return \&quot;cat\&quot; and \&quot;couch\&quot;
     * 
     * @param image: The image to use as source for subject detection
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
    public List<SubjectDto> detectSubjects( Attachment imageDetail, String method, String sourceID, Integer maxMatches) throws ServiceException {
        // TODO: Implement...
    
        
        try{ 
            List<SubjectDto> response = new ArrayList<>();
        SubjectDto item = new SubjectDto();
        item.setSubject("GV9Ip");
        item.setSourceID("guCe7wMQ");
        item.setConfidence(-1.25374e+38F);
        response.add(item);
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

    /**
     * Request images similar to the uploaded image
     * 
     * @param image: The image to use as source for the similarity search
     * 
     * @param collection: The collection to search for similar images. If none is specified, the default collection will be used
     * 
     * @param sourceID: Optional ID for the image, used for tracking &amp; debugging
     * 
     * @param maxMatches: The maximum number of similar images to return
     * 
     * @return <ul>
      *   <li>code = 200, message = "An array of metadata for similar images, including URLs for the images", response = SimilarResponseDto.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<SimilarResponseDto> findSimilarWhole( Attachment imageDetail, String collection, String sourceID, Integer maxMatches) throws ServiceException {
        // TODO: Implement...
    
        
        try{ 
            List<SimilarResponseDto> response = new ArrayList<>();
        SimilarResponseDto item = new SimilarResponseDto();
        item.setSourceID("XV3Pz95");
        item.setDistance(1134865941435173175.4674843810170087D);
        item.setUrl("Te932");
        ImageDto similarImage = new ImageDto();
        similarImage.setId("r3oXQ");
        similarImage.setMicroURL("i43Pa");
        similarImage.setTinyURL("Lp5IFG0");
        similarImage.setMediumURL("ruZMd6W");
        similarImage.setFullURL("FdJ6PJ");
        similarImage.setRawURL("S0iX6Svc3z9");
        similarImage.setIiifURL("RJy5J");
        similarImage.setCreationDate("DgM5GM8");
        similarImage.setDataURL("sPmO3");
        item.setSimilarImage(similarImage);
        PersonDto similarPerson = new PersonDto();
        similarPerson.setFirstName("AwiPqD");
        similarPerson.setLastName("Dpqkr");
        similarPerson.setBirthday("iEbbK");
        similarPerson.setDeathday("IK1DloZ");
        similarPerson.setOccupation("T1izgd6");
        item.setSimilarPerson(similarPerson);
        List<PersonDto> imageCreators = new ArrayList<>();
        PersonDto imageCreators2 = new PersonDto();
        imageCreators2.setFirstName("d88Yy");
        imageCreators2.setLastName("se8UU");
        imageCreators2.setBirthday("uCVkAs33");
        imageCreators2.setDeathday("jEHTf");
        imageCreators2.setOccupation("L5D7LyQ");
        imageCreators.add(imageCreators2);
        item.setImageCreators(imageCreators);
        response.add(item);
        return response;
        } catch (Exception e){
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
        // TODO: Implement...
    
        
        try{ 
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"filename.ext\"");
            return output -> output.write("Magic".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e){
            throw handleException(e);
        }
    
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
        // TODO: Implement...
    
        
        try{ 
            String response = "YsX8II";
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
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

}
