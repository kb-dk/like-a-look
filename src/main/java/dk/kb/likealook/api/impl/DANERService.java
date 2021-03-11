/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.likealook.api.impl;

import dk.kb.likealook.config.ServiceConfig;
import dk.kb.likealook.model.SimilarResponseDto;
import dk.kb.util.yaml.YAML;
import dk.kb.webservice.exception.InternalServiceException;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import org.apache.cxf.helpers.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonArray;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Special purpose implementation for the DANER project.
 *
 */
public class DANERService {
    private static final Logger log = LoggerFactory.getLogger(DANERService.class);

    public static final String DANER_KEY = ".likealook.daner";

    public static final String MOCK_MODE_KEY = ".implementation"; // mock / remote
    public static final String MOCK_MODE_DEFAULT = "mock";

    public static final String REMOTE_SERVICE_KEY = ".remote.url";
    public static final String REMOTE_SERVICE_DEFAULT = null;

    public enum IMPLEMENTATION {mock, remote}

    private static DANERService instance;

    private final IMPLEMENTATION implementation;
    private final String remoteURL;

    public static DANERService getInstance() {
        if (instance == null) {
            instance = new DANERService();
        }
        return instance;
    }

    public DANERService() {
        this(ServiceConfig.getConfig());
    }
    public DANERService(YAML config) {
        if (!config.containsKey(DANER_KEY)) {
            log.info("Skipping setup of DANERService as '{}' does not exist in configuration", DANER_KEY);
            implementation = null;
            remoteURL = null;
            return;
        }

        YAML danerConf = config.getSubMap(DANER_KEY);
        implementation = IMPLEMENTATION.valueOf(danerConf.getString(MOCK_MODE_KEY, MOCK_MODE_DEFAULT));
        remoteURL = danerConf.getString(REMOTE_SERVICE_KEY, REMOTE_SERVICE_DEFAULT);
        log.info("Created " + this);
    }

    public static List<SimilarResponseDto> findSimilar(InputStream imageStream, String sourceID, Integer maxMatches) {
        log.info("findSimilar(..., sourceID=" + sourceID + ", maxMatches=" + maxMatches + ") called");

        try {
            // TODO: Append image extension (jpg/png)
            sourceID = sourceID == null || sourceID.isBlank() ?
                    ResourceHandler.createEphemeral(imageStream) :
                    ResourceHandler.createEphemeral(sourceID, imageStream);
        } catch (IOException e) {
            String message = "findSimilarDANER encountered IOException while reading source image";
            log.warn(message, e);
            throw new InvalidArgumentServiceException(message, e);
        }
        maxMatches = maxMatches == null ? 10 : maxMatches;

        switch (getInstance().implementation) {
            case mock: return findSimilarMock(sourceID, maxMatches);
            case remote: {
                byte[] sourceImage = ResourceHandler.getEphemeral(sourceID).getContent();
                return findSimilarRemote(sourceID, sourceImage, maxMatches);
            }
            default: throw new InternalServiceException(
                    "DANER implementation '" + getInstance().implementation + "' not supported yet");
        }
    }

    private static List<SimilarResponseDto> findSimilarMock(String sourceID, Integer maxMatches) {
        List<SimilarResponseDto> response = new ArrayList<>();
        Random r = new Random();
        List<String> imageIDs = new ArrayList<>(DANERData.getImageIDs());
        Collections.shuffle(imageIDs, r);

        double distance = r.nextDouble();
        for (int i = 0; i < maxMatches; i++) {
            SimilarResponseDto item = new SimilarResponseDto();
            distance += r.nextDouble();
            item.setDistance(distance);
            item.setSourceID(sourceID);
            item.setSourceURL(ResourceHandler.getResourceURL(ResourceHandler.EPHEMERAL + "/" + sourceID));
            DANERData.fillResponse(item, imageIDs.get(i));

            response.add(item);
        }
        return response;
    }

    // https://stackoverflow.com/questions/3324717/sending-http-post-request-in-java
    private static List<SimilarResponseDto> findSimilarRemote(String sourceID, byte[] sourceImage, Integer maxMatches) {
        HttpURLConnection http = prepareConnection();
        postImage(sourceImage, http);
        List<PersonMatch> personMatches = getPersonMatches(http);

        System.out.println("*** " + personMatches);
        return null;
    }

    private static HttpURLConnection prepareConnection() {
        log.debug("Preparing HTTP connection to " + getInstance().remoteURL);
        URL url;
        try {
            url = new URL(getInstance().remoteURL);
        } catch (MalformedURLException e) {
            throw new InternalServiceException(
                    "Malformed URL for remote DANER service '" + getInstance().remoteURL + "'", e);
        }
        URLConnection con;
        try {
            con = url.openConnection();
        } catch (IOException e) {
            throw new InternalServiceException(
                    "Internal error: Unable to establish connection to URL '" + getInstance().remoteURL + "'", e);
        }
        HttpURLConnection http = (HttpURLConnection)con;
        try {
            http.setRequestMethod("POST"); // PUT is another valid option
        } catch (ProtocolException e) {
            throw new InternalServiceException(
                    "Internal error: Unable to set request method POST (this should not happen)", e);
        }
        http.setDoOutput(true);
        http.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + DELIMITER);
        http.setChunkedStreamingMode(0);
        http.setDoInput(true);
        http.setDoOutput(true);
        return http;
    }
    private static final String DELIMITER = "MultipartDelimiterString";

    private static void postImage(byte[] sourceImage, HttpURLConnection http) {
        final long startTime = System.nanoTime();
        log.debug("Posting image");
        try (OutputStream out = http.getOutputStream()) {
            // https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
            out.write(("\r\n--" + DELIMITER + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Disposition: form-data; name=\"image\"; filename=\"masked.jpg\"\r\n\r\n"
                              .getBytes(StandardCharsets.UTF_8));
            out.write(sourceImage);
            out.write(("\r\n--" + DELIMITER + "--\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            log.debug("Bytes delivered, closing output");
        } catch (IOException e) {
            throw new InternalServiceException(
                    "Error calling external DANER service '" + getInstance().remoteURL + "': " + e.getMessage(), e);
        }

        int response;
        try {
            log.debug("Retrieving response code");
            response = http.getResponseCode();
        } catch (IOException e) {
            String message = "Error retrieving response code from call to external DANER service '" +
                                getInstance().remoteURL + "': " + e.getMessage();
            log.warn(message, e);
            throw new InternalServiceException(message, e);
        }
        log.debug("Finished posting image in " + (System.nanoTime()-startTime)/1000000L +
                  " ms + with response " + response);
    }

    private static List<PersonMatch> getPersonMatches(HttpURLConnection http) {
        final long startTime = System.nanoTime();
        log.debug("Retrieving HTTP response");
        InputStream response;
        try {
            response = http.getInputStream();
        } catch (IOException e) {
            throw new InternalServiceException(
                    "Error getting result from POST to external DANER service '" + getInstance().remoteURL + "'", e);
        }
        log.debug("Finished retrieving response in " + (System.nanoTime()-startTime)/1000000L + " ms");

        String json;
        try {
            json = IOUtils.toString(response, "utf-8");
        } catch (IOException e) {
            throw new InternalServiceException(
                    "Error piping result from POST to external DANER service '" + getInstance().remoteURL + "'", e);
        }

        return json2Matches(json);
    }

    public static List<PersonMatch> json2Matches(String jsonStr) {
        // {
        //	"portraits":[
        //		{
        //			"id":"DP002205",
        //			"probability":0.762
        //		},
        //		{
        //			"id":"DP002212",
        //			"probability":0.123
        //		}
        //	]
        //}
        JSONObject json = new JSONObject(jsonStr);
        if (!json.has("portraits")) {
            throw new InvalidArgumentServiceException(
                    "Returned JSON for DANER remote service did not contain entry 'portraits':\n" + jsonStr);
        }
        JSONArray portraits = json.getJSONArray("portraits");
        List<PersonMatch> personMatches = new ArrayList<>(portraits.length());
        for (int i = 0 ; i < portraits.length() ; i++) {
            personMatches.add(new PersonMatch(portraits.getJSONObject(i)));
        }
        return personMatches;
    }

    public static class PersonMatch implements Comparable<PersonMatch> {
        public final String id;
        public final double distance;
        private static final Comparator<PersonMatch> comparator =
                Comparator.comparing(PersonMatch::getDistance).thenComparing(PersonMatch::getId);

        public PersonMatch(JSONObject json) {
            //			"id":"DP002205",
            //			"probability":0.762
            this(json.getString("id"), json.getDouble("probability"));
        }

        public PersonMatch(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }

        @Override
        public int compareTo(PersonMatch o) {
            return comparator.compare(this, o);
        }

        public String getId() {
            return id;
        }

        public double getDistance() {
            return distance;
        }
    }

    @Override
    public String toString() {
        return "DANERService(impl=" + implementation + ", remoteServiceURL=" + remoteURL + ")";
    }
}

