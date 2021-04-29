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
import dk.kb.likealook.model.ElementDto;
import dk.kb.likealook.model.SimilarDto;
import dk.kb.likealook.model.SimilarResponseDto;
import dk.kb.util.yaml.YAML;
import dk.kb.webservice.exception.InternalServiceException;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import org.apache.cxf.helpers.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Special purpose implementation for the DANER project.
 *
 */
public class DANERService {
    private static final Logger log = LoggerFactory.getLogger(DANERService.class);

    public static final String DANER_KEY = ".likealook.daner";

    // .implementation has been superceeded by collection. Ponder the best way of controlling the DANER implementation
    public static final String MOCK_MODE_KEY = ".implementation"; // mock / remote
    public static final String MOCK_MODE_DEFAULT = "mock";

    public static final String REMOTE_SERVICE_KEY = ".remote.url";
    public static final String REMOTE_SERVICE_DEFAULT = null;

    public static final String REMOTE_IMAGEURL_KEY = "imageurl";

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

    public static SimilarResponseDto findSimilar(
            String collection, InputStream imageStream, String sourceID, Integer maxMatches) {
        log.info("findSimilar(..., sourceID=" + sourceID + ", maxMatches=" + maxMatches + ") called");

        maxMatches = maxMatches == null ? 10 : maxMatches;

        switch (collection) {
            case "daner_mock": return findSimilarMock(sourceID, maxMatches);
            case "daner_v1": {
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

                //byte[] sourceImage = ResourceHandler.getEphemeral(sourceID).getContent();
                final String sourceURL = ResourceHandler.getResourceURL(ResourceHandler.EPHEMERAL + "/" + sourceID);
                return findSimilarRemoteMulti(sourceID, sourceURL, maxMatches);
            }
            default: throw new InternalServiceException("DANER implementation '" + collection + "' is not known");
        }
    }

    private static SimilarResponseDto findSimilarMock(String sourceID, Integer maxMatches) {
        final int ELEMENTS = 2;
        final Random r = new Random();

        List<String> imageIDs = new ArrayList<>(DANERData.getImageIDs());
        if (imageIDs.isEmpty()) {
            throw new InternalServiceException("No images available for mock service");
        }
        Collections.shuffle(imageIDs, r);

        SimilarResponseDto response = new SimilarResponseDto()
                .sourceID(sourceID)
                .sourceURL(ResourceHandler.getResourceURL(ResourceHandler.EPHEMERAL + "/" + sourceID))
                .technote("Created by mock service: Result is randomly selected");
        List<ElementDto> elements = new ArrayList<>(ELEMENTS);
        response.setElements(elements);

        for (int e = 0 ; e < ELEMENTS ; e++) {
            ElementDto element = new ElementDto().index(e);
            elements.add(element);

            List<SimilarDto> similars = new ArrayList<>(maxMatches);
            element.setSimilars(similars);
            double distance = r.nextDouble();
            for (int s = 0 ; s < maxMatches ; s++) {
                SimilarDto item = new SimilarDto()
                        .distance(distance += r.nextDouble());
                DANERData.fillResponse(item, imageIDs.get((e*maxMatches+s) % imageIDs.size()));
                similars.add(item);
            }
        }

        return response;
    }

    public static List<SimilarResponseDto> findSimilarRemoteSingle(String sourceID, String sourceURL, Integer maxMatches) {
        throw new UnsupportedOperationException("Deprecated");
        //HttpURLConnection http = prepareGETConnection();
        //httpGetString(sourceURL, http);
/*        List<PersonMatch> personMatches = getPersonMatchesSingle(httpGetRequest(sourceURL));

        return personMatches.stream()
                .sorted()
                .limit(maxMatches)
                .map(pm -> new SimilarResponseDto()
                        .sourceID(pm.getId())
                        .distance(pm.getDistance())
                        .technote("Facial similarity by daner_v1 (remote call to Wolfram backed service)"))
                .peek(DANERData::fillResponse)
                .map(sr -> sr
                        .sourceID(sourceID)
                        .sourceURL(sourceURL))
                .collect(Collectors.toList());*/
    }

    public static SimilarResponseDto findSimilarRemoteMulti(String sourceID, String sourceURL, Integer maxMatches) {
        return new SimilarResponseDto()
                .sourceID(sourceID)
                .sourceURL(sourceURL)
                .technote("Facial similarity by daner_v1 (remote call to Wolfram backed service")
                .elements(multiMatchesToElements(getPersonMatchesMulti(httpGetRequest(sourceURL))));
    }

    private static List<ElementDto> multiMatchesToElements(List<List<PersonMatch>> multiMatches) {
        AtomicInteger elementID = new AtomicInteger(0);
        return multiMatches.stream()
                .map(matches -> new ElementDto()
                        .index(elementID.getAndIncrement())
                        .similars(matchesToSimilars(matches)))
                .collect(Collectors.toList());
    }

    private static List<SimilarDto> matchesToSimilars(List<PersonMatch> matches) {
        return matches.stream()
                .map(match -> DANERData.fillResponse(new SimilarDto().distance(match.getDistance()), match.id))
                .collect(Collectors.toList());
    }

    private static InputStream httpGetRequest(String sourceURL) {
        final String getURL = getInstance().remoteURL + "/?imageurl=" + sourceURL;
        log.debug("Calling HTTP GET for '" + getURL + "'");
        //final String getURL = getInstance().remoteURL + "/?imageurl=http://localhost/pmd.png";
        try {
            URLConnection conn = new URL(getURL).openConnection();
            return conn.getInputStream();
        } catch (Exception e) {
            log.warn("Exception calling '{}'", getURL, e);
            throw new InternalServiceException("Exception calling '" + getURL + "'");
        }
    }

    private static HttpURLConnection prepareGETConnection() {
        HttpURLConnection http = prepareGenericConnection();
        try {
            http.setRequestMethod("GET");
            http.setDoOutput(false);
        } catch (ProtocolException e) {
            throw logThrow("Internal error: Unable to set request method GET (this should not happen)", e);
        }
        return http;
    }
    private static HttpURLConnection preparePOSTConnection() {
        HttpURLConnection http = prepareGenericConnection();
        try {
            http.setRequestMethod("POST");
            http.setDoOutput(true);
        } catch (ProtocolException e) {
            throw logThrow("Internal error: Unable to set request method POST (this should not happen)", e);
        }
        //http.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + DELIMITER);
        //http.setChunkedStreamingMode(0);
        return http;
    }
    private static HttpURLConnection prepareGenericConnection() {
        log.debug("Preparing HTTP connection to " + getInstance().remoteURL);
        URL url;
        try {
            url = new URL(getInstance().remoteURL);
        } catch (MalformedURLException e) {
            throw logThrow("Malformed URL for remote DANER service '" + getInstance().remoteURL + "'", e);
        }
        URLConnection con;
        try {
            con = url.openConnection();
        } catch (IOException e) {
            throw logThrow("Internal error: Unable to establish connection to URL '" + getInstance().remoteURL + "'", e);
        }
        HttpURLConnection http = (HttpURLConnection)con;
        http.setDoInput(true);
        http.setConnectTimeout(500);
        http.setReadTimeout(10*1000); // 10 seconds
        return http;
    }
    private static final String DELIMITER = "MultipartDelimiterString";

    private static void httpGetString(String sourceURL, HttpURLConnection http) {
        log.debug("GETting from external service '{}': '{}'",
                  getInstance().remoteURL, sourceURL.length() < 400 ? sourceURL : sourceURL.substring(0, 397) + "...");
        http.setRequestProperty(REMOTE_IMAGEURL_KEY, sourceURL);
    }

    private static void httpPostString(String content, HttpURLConnection http) {
        log.debug("Posting to external service '{}': '{}'",
                  getInstance().remoteURL, content.length() < 400 ? content : content.substring(0, 397) + "...");
        try (OutputStream out = http.getOutputStream()) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw logThrow("Error calling external DANER service '" + getInstance().remoteURL + "'", e);
        }
    }

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
            throw logThrow("Error calling external DANER service '" + getInstance().remoteURL + "'", e);
        }
/*
        int response;
        try {
            log.debug("Retrieving response code");
            response = http.getResponseCode();
            log.debug("Finished posting image in " + (System.nanoTime()-startTime)/1000000L +
                      " ms + with response " + response);
        } catch (IOException e) {
            logThrow("Error retrieving response code from call to external DANER service '" +
                                getInstance().remoteURL + "'", e);
        }*/
    }

    private static InternalServiceException logThrow(String message, Exception e) {
        log.warn(message, e);
        return new InternalServiceException(message + e.getMessage(), e);
    }

    private static List<PersonMatch> getPersonMatchesSingle(HttpURLConnection http) {
        final long startTime = System.nanoTime();
        log.debug("Retrieving HTTP response");
        InputStream response;
        try {
            response = http.getInputStream();
        } catch (IOException e) {
            throw logThrow("Error getting result from POST to external DANER service '" + getInstance().remoteURL + "'", e);
        }
        log.debug("Finished retrieving response in " + (System.nanoTime()-startTime)/1000000L + " ms");

        return getPersonMatchesSingle(response);
    }

    private static List<PersonMatch> getPersonMatchesSingle(InputStream response) {
        String json;
        try {
            json = IOUtils.toString(response, "utf-8");
        } catch (IOException e) {
            throw logThrow("Error piping result from POST to external DANER service '" + getInstance().remoteURL + "'", e);
        }

        return json2MatchesSingle(json);
    }

    public static List<PersonMatch> json2MatchesSingle(String jsonStr) {
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
        JSONObject json;
        try {
            json = new JSONObject(jsonStr);
        } catch (JSONException e) {
            String message = "Expected JSON from remote DANER server, but got " +
            (jsonStr.length() > 400 ? jsonStr.substring(0, 397) + "..." : jsonStr);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }
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

    private static List<List<PersonMatch>> getPersonMatchesMulti(HttpURLConnection http) {
        final long startTime = System.nanoTime();
        log.debug("Retrieving HTTP response");
        InputStream response;
        try {
            response = http.getInputStream();
        } catch (IOException e) {
            throw logThrow("Error getting result from POST to external DANER service '" + getInstance().remoteURL + "'", e);
        }
        log.debug("Finished retrieving response in " + (System.nanoTime()-startTime)/1000000L + " ms");

        return getPersonMatchesMulti(response);
    }

    private static List<List<PersonMatch>> getPersonMatchesMulti(InputStream response) {
        String json;
        try {
            json = IOUtils.toString(response, "utf-8");
        } catch (IOException e) {
            throw logThrow("Error piping result from POST to external DANER service '" + getInstance().remoteURL + "'", e);
        }

        return json2MatchesMulti(json);
    }

    public static List<List<PersonMatch>> json2MatchesMulti(String jsonStr) {
        // [
        //	[
        //		{
        //			"id":"DP038937",
        //			"distance":4.30669854378983e1
        //		},
        //		{
        //			"id":"DP008594",
        //			"distance":4.46568637651598e1
        //		},
        //		{
        //			"id":"DP036909",
        //			"distance":4.478868972806472e1
        //		}
        //	]
        //]
        JSONObject json;
        try {
            json = new JSONObject("{ tmp: " + jsonStr + "}");
        } catch (JSONException e) {
            String message = "Expected JSON array from remote DANER server, but got " +
            (jsonStr.length() > 400 ? jsonStr.substring(0, 397) + "..." : jsonStr);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }
        JSONArray jsonArray = json.getJSONArray("tmp");

        List<List<PersonMatch>> personMatchesMajor = new ArrayList<>(jsonArray.length());
        for (int m = 0 ; m < jsonArray.length() ; m++) {
            JSONArray portraits = jsonArray.getJSONArray(m);
            List<PersonMatch> personMatches = new ArrayList<>(portraits.length());
            for (int i = 0 ; i < portraits.length() ; i++) {
                personMatches.add(new PersonMatch(portraits.getJSONObject(i)));
            }
            personMatchesMajor.add(personMatches);
        }
        return personMatchesMajor;
    }

    public static class PersonMatch implements Comparable<PersonMatch> {
        public final String id;
        public final double distance;
        private static final Comparator<PersonMatch> comparator =
                Comparator.comparing(PersonMatch::getDistance).thenComparing(PersonMatch::getId);

        public PersonMatch(JSONObject json) {
            //			"id":"DP002205",
            //			"probability":0.762
            this(json.getString("id"), json.getDouble("distance"));
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

        @Override
        public String toString() {
            return "PersonMatch(id='" + id + "', distance=" + distance + ")";
        }
    }

    @Override
    public String toString() {
        return "DANERService(impl=" + implementation + ", remoteServiceURL=" + remoteURL + ")";
    }
}

