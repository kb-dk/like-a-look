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

import au.com.bytecode.opencsv.CSVReader;
import dk.kb.likealook.config.ServiceConfig;
import dk.kb.likealook.model.ImageDto;
import dk.kb.likealook.model.PersonDto;
import dk.kb.likealook.model.SimilarDto;
import dk.kb.likealook.model.SimilarDto;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import dk.kb.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Special purpose implementation for the DANER project.
 *
 * Keeps track of metadata for 11687 portraits from the DANER project at the Royal Danish Library.
 * The portraits are out of copyright, as are the metadata. Both can be downloaded at https://loar.kb.dk/
 * TODO: Make a direct link to LOAR
 *
 * A sample can be found at the test resources folder.
 */
public class DANERData {
    private static final Logger log = LoggerFactory.getLogger(DANERData.class);

    public static final String DANER_KEY = ".likealook.daner";
    public static final String CSV_KEY = ".csv";

    public static final String FACE_RESOURCES = "faces";
    public static final String CLOSEUP_RESOURCES = "faces_close_cut";

    private static DANERData instance;

    private final Map<String, SimilarDto> metadata = new HashMap<>();

    public static DANERData getInstance() {
        if (instance == null) {
            instance = new DANERData();
        }
        return instance;
    }

    public DANERData() {
        this(ServiceConfig.getConfig());
    }
    public DANERData(YAML config) {
        if (!config.containsKey(DANER_KEY)) {
            log.info("Skipping setup of DANERData as '{}' does not exist in configuration", DANER_KEY);
            return;
        }

        YAML danerConf = config.getSubMap(DANER_KEY);
        Arrays.asList(FACE_RESOURCES, CLOSEUP_RESOURCES).forEach(
                resource -> {
                    if (!ResourceHandler.hasCollection(resource)) {
                        log.warn("Warning: The ResourceHandler has no collection with the name '" + resource +
                                 "'. For DANERData to work properly it should have both " + FACE_RESOURCES +
                                 " and " + CLOSEUP_RESOURCES);
                    }
                });
        String csv = danerConf.getString(CSV_KEY);
        loadCSV(csv);
    }

    /**
     * @return the amount of persons in the collection.
     */
    public static int size() {
        return getInstance().metadata.size();
    }

    /**
     * Assigns image, person and creators to the given response.
     * Note that the assignments are shallow copies. Do not modify the added metadata!
     * @param response the response to fill.
     * @param imageID  the DANER ID used to resolve image, person and creators data.
     * @return the given response, extended with the relevant information.
     */
    public static SimilarDto fillResponse(SimilarDto response, String imageID) {
        SimilarDto data = getInstance().metadata.get(imageID);
        if (data == null) {
            String message = "Error: Unable to locate metadata for imageID '" + imageID + "'";
            log.error(message);
            return response;
            //throw new InternalServiceException(message);
        }
        response.setImage(data.getImage());
        response.setPerson(data.getPerson());
        response.setImageCreators(data.getImageCreators());
        return response;
    }

    public SimilarDto getSimilarImage(String imageID) {
        SimilarDto data = getInstance().metadata.get(imageID);
        if (data == null) {
            throw new InternalServiceException("Error: Unable to locate metadata for imageID '" + imageID + "'");
        }
        return data;
    }

    /**
     * @return the imageIDs currently in the DANER collection.
     */
    public static Set<String> getImageIDs() {
        return getInstance().metadata.keySet();
    }

    private void loadCSV(String csv) {
        List<Path> csvPaths = Resolver.resolveGlob(csv);
        if (csvPaths.isEmpty()) {
            log.warn("Warning: Unable to resolve any csv paths from '" + csv + "'");
        }
        metadata.clear();
        csvPaths.forEach(this::addCSV);
        log.info("Resolved " + metadata.size() + " DANER image metadata from '" + csv + "'");
    }

    private void addCSV(Path csvPath) {
        final int EXPECTED_COUNT = 9;
        log.info("Loading metadata from '" + csvPath + "'");
        try (FileReader fi = new FileReader(csvPath.toFile(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(fi, ';')) {
            csvReader.readNext(); // header
            String[] elements;
            while ((elements = csvReader.readNext()) != null) {
                if (elements.length == 0) {
                    continue; // Blank lines
                }
                if (elements[0].startsWith("#")) {
                    continue; // Comment
                }
                if (elements.length != EXPECTED_COUNT) {
                    log.warn("Warning: Expected element count is " + EXPECTED_COUNT + " but the number of elements " +
                             "for current line is " + elements.length + " for line " + Arrays.toString(elements));
                    continue;
                }
                addCSVLine(elements);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load data from '" + csvPath + "'", e);
        }
    }

    //    FileName;PersonsName;PersonsFamilyName;DateOfBirth;DateOfDeath;DateOfPhotography;PersonsJob;Photographer;LinkToRoyalDanishLibrarysDigitalCollections
    //           0           1                 2           3           4                 5           6           7                                           8
    //    0473151.jpg;Alfred;Lehmann;1858;1921;1895-1907; psykolog;Laurberg, Julie Rasmine Marie (7.9.1856-26.6.1925) fotograf;http://www5.kb.dk/images/billed/2010/okt/billeder/object10530/da/
    //    DP002045.jpg;Jens Johannes;Andersen;07.06.1846;04.04.1902;1875-1919; lærer;Jørgensen, Chresten Estrup (11.9.1843-21.11.1879) fotograf|Jørgensen, Jacobine Wendelia f. Bentzen (19.5.1849-);http://www5.kb.dk/images/billed/2010/okt/billeder/object445368/da/

    private void addCSVLine(String[] elements) {
        final String baseImage = elements[0];
        final String base = baseImage.replaceAll("[.][a-z]*$", ""); // Remove extension
        SimilarDto similar = new SimilarDto();

        ImageDto image = new ImageDto();
        image.setId(base);
        image.setMicroURL(ResourceHandler.getResourceURL(CLOSEUP_RESOURCES + "/" + baseImage));
        image.setTinyURL(ResourceHandler.getResourceURL(FACE_RESOURCES + "/" + baseImage));
        image.setMediumURL(ResourceHandler.getResourceURL(FACE_RESOURCES + "/" + baseImage)); // TODO: Would be better to link to full portrait
        image.setCreationDate(datesToStr(elements[5]));
        image.setDataURL(elements[8]);
        similar.setImage(image);

        PersonDto person = new PersonDto();
        person.setFirstName(elements[1]);
        person.setLastName(elements[2]);
        person.setBirthday(datesToStr(elements[3]));
        person.setDeathday(datesToStr(elements[4]));
        person.setOccupation(elements[6]);
        similar.setPerson(person);

        similar.setImageCreators(
                Arrays.stream(elements[7].split(" *[|] *"))
                        .filter(photographer -> !photographer.isBlank())
                        .map(DANERData::parsePhotographer)
                        .collect(Collectors.toList()));

        metadata.put(base, similar);
    }

    // Jørgensen, Chresten Estrup (11.9.1843-21.11.1879) fotograf
    // Jørgensen, Jacobine Wendelia f. Bentzen (19.5.1849-)
    private static PersonDto parsePhotographer(String photographerStr) {
        String[] comma = photographerStr.split(" *, *", 2);

        PersonDto person = new PersonDto();

        person.setLastName(comma[0]);
        if (comma.length == 1) {
            if (!comma[0].contains("&")) { // e.g. "Budtz Müller & Co."
                log.debug("Only able to extract last name from photographer '" + photographerStr + "'");
            }
            return person;
        }

        // Chresten Estrup (11.9.1843-21.11.1879) fotograf
        // Jacobine Wendelia f. Bentzen (19.5.1849-)
        String[] lparen = comma[1].split(" *[(] *");
        person.setFirstName(lparen[0]);
        if (lparen.length == 1) {
            return person;
        }

        // 11.9.1843-21.11.1879) fotograf
        // 19.5.1849-)
        String[] rparen = lparen[1].split(" *[)] *");
        if (rparen.length > 1) {
            person.setOccupation(rparen[1]);
        }

        // 11.9.1843-21.11.1879
        // 19.5.1849-
        String[] dates = rparen[0].split(" *[-] *");
        person.setBirthday(parseDate(dates[0]));
        if (dates.length > 1 && !dates[1].isBlank()) {
            person.setDeathday(parseDate(dates[1]));
        }

        return person;
    }

    // 07.06.1846,  7.6.1846, 7/6/1846, 11.9.1843-21.11.1879, 1843-1879
    static String datesToStr(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }
        return Arrays.stream(dateStr.split(" *[-] *"))
                .map(DANERData::parseDate)
                .collect(Collectors.joining(" to "));
    }

    // 07.06.1846,  7.6.1846, 7/6/1846, 1843, 11-1879
    static String parseDate(String dateStr) {
        List<String> dElements= Arrays.stream(dateStr.split(" *[^0-9]+ *"))
                .map(element -> (element.length() < 2 ? "0" : "") + element)
                .collect(Collectors.toList());
        Collections.reverse(dElements);
        return String.join("-", dElements);
    }

}

