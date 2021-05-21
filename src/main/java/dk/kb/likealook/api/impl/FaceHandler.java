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

import dk.kb.likealook.model.BoxDto;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detection of faces in images.
 */
public class FaceHandler {
    private static final Logger log = LoggerFactory.getLogger(FaceHandler.class);

    public enum METHOD { haarcascade;

        public static METHOD getDefault() {
            return haarcascade;
        }
        public static METHOD valueOfWithDefault(String method) {
            return method == null || method.isEmpty() ? getDefault() : valueOf(method.toLowerCase(Locale.ROOT));
        }
    }

    public static List<BoxDto> detectFaces(InputStream imageStream, METHOD method, String sourceID) throws IOException {
        long startTime = System.currentTimeMillis();
        final MBFImage image = ImageUtilities.readMBF(imageStream);
        List<BoxDto> faceBoxes = detectFacesHaar(image, sourceID);
        log.debug("Extracted {} faces from '{}' in {}ms using {}",
                  faceBoxes.size(), sourceID, System.currentTimeMillis()-startTime, method);
        return faceBoxes;
    }

    public static byte[] faceOverlay(InputStream inputStream, METHOD realMethod, String sourceID) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, bos);
        } catch (IOException e) {
            throw new InvalidArgumentServiceException("Unable to copy content of input stream");
        }
        final MBFImage image = ImageUtilities.readMBF(new ByteArrayInputStream(bos.toByteArray()));
        List<DetectedFace> faces = detectFacesHaarList(image);
        int imageID = 0;
        for (DetectedFace face : faces) {
            Rectangle bounds = face.getBounds();
            image.drawShape(bounds, RGBColour.RED);
            image.drawText(Integer.toString(imageID++), (int)bounds.x + 3, (int)(bounds.y + 33),
                           HersheyFont.FUTURA_MEDIUM, 30, RGBColour.RED);
        }

        ByteArrayOutputStream iout = new ByteArrayOutputStream();
        ImageUtilities.write(image, "jpeg", iout);
        return iout.toByteArray();
    }


    private static List<BoxDto> detectFacesHaar(MBFImage image, String sourceID) throws IOException {
        AtomicInteger faceID = new AtomicInteger(0);
        return detectFacesHaarList(image).stream().
                map(face -> faceToBox(face, sourceID, faceID)).
                collect(Collectors.toList());
    }

    private static List<DetectedFace> detectFacesHaarList(MBFImage image) throws IOException {

        // Need to create new one as it is not thread safe. Takes about 100ms for creation
        FaceDetector<DetectedFace, FImage> fd = new HaarCascadeDetector(200);
        // 12MP image with 20+ faces: ~1.1 seconds
        return fd.detectFaces(Transforms.calculateIntensity(image));
    }

    private static BoxDto faceToBox(DetectedFace detectedFace, String sourceID, AtomicInteger faceID) {
        BoxDto box = new BoxDto();
        box.sourceID(sourceID).faceID(faceID.getAndIncrement());
        box.confidence(detectedFace.getConfidence());
        Rectangle dBox = detectedFace.getBounds().calculateRegularBoundingBox();
        box.x((int) dBox.x);
        box.y((int) dBox.y);
        box.width((int) dBox.width);
        box.height((int) dBox.height);
        return box;
    }
}
