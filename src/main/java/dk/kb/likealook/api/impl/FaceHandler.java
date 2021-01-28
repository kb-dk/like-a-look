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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class FaceHandler {
    private static final Logger log = LoggerFactory.getLogger(FaceHandler.class);

    public enum METHOD { haarcascade;

        public static METHOD getDefault() {
            return haarcascade;
        }
    }

    public static List<BoxDto> detectFaces(InputStream imageStream, METHOD method, String sourceID) throws IOException {
        long startTime = System.currentTimeMillis();
        List<BoxDto> faceBoxes = detectFacesHaar(imageStream, sourceID);
        log.debug("Extracted {} faces from '{}' in {}ms using {}",
                  faceBoxes.size(), sourceID, System.currentTimeMillis()-startTime, method);
        return faceBoxes;
    }

    private static List<BoxDto> detectFacesHaar(InputStream imageStream, String sourceID) throws IOException {
        final MBFImage image = ImageUtilities.readMBF(imageStream);

        // Need to create new one as it is not thread safe. Takes about 100ms for creation
        FaceDetector<DetectedFace, FImage> fd = new HaarCascadeDetector(200);
        // 12MP image with 20+ faces: ~1.1 seconds
        List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(image));

        AtomicInteger faceID = new AtomicInteger(0);
        return faces.stream().
                map(face -> faceToBox(face, sourceID, faceID)).
                collect(Collectors.toList());
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
