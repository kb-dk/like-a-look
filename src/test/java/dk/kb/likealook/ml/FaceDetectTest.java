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
package dk.kb.likealook.ml;

import dk.kb.util.Resolver;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class FaceDetectTest {
    private static Log log = LogFactory.getLog(FaceDetectTest.class);

    // https://stackoverflow.com/questions/51994659/java-face-detection-with-openimaj
    @Test
    public void testDetectFace() throws IOException, InterruptedException {
        File facesFile = new File(Resolver.resolveURL("pexels-andrea-piacquadio-3812743.jpg").getFile());
        final MBFImage image = ImageUtilities.readMBF(facesFile);

        FaceDetector<DetectedFace, FImage> fd = new HaarCascadeDetector(200);
        List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(image));
//        System.out.println("# Found " + faces.size() + " faces, one per line.");
//        System.out.println("# <x>, <y>, <width>, <height>");

/*        for (DetectedFace face : faces) {
            Rectangle bounds = face.getBounds();
            image.drawShape(face.getBounds(), RGBColour.RED);

             System.out.println(bounds.x + ";" + bounds.y + ";" + bounds.width + ";" + bounds.height);
        }*/

        assertEquals(23, faces.size(), "There should be the expected number of detected faces using OpenIMAJ");

        //DisplayUtilities.display(image);
        //Thread.sleep(5000);
    }
}
