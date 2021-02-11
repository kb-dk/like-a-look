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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.*;
import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.modality.*;
import ai.djl.modality.cv.*;
import ai.djl.modality.cv.util.*;
import ai.djl.modality.cv.transform.*;
import ai.djl.modality.cv.translator.*;
import ai.djl.repository.zoo.*;
import ai.djl.translate.*;
import ai.djl.training.util.*;

/**
 *
 */
public class DJLTest {
    private static final Logger log = LoggerFactory.getLogger(DJLTest.class);

    public static void main(String[] args) {
        // https://github.com/awslabs/djl/blob/master/docs/load_model.md#load-model-from-a-url
        Criteria<Image, Classifications> criteria = Criteria.builder()
                .setTypes(Image.class, Classifications.class) // defines input and output data type
                .optTranslator(ImageClassificationTranslator.builder().optSynsetArtifactName("synset.txt").build())
                .optModelUrls("file:///var/models/my_resnet50") // search models in specified path
                .optModelName("resnet50") // specify model file prefix
                .build();
    }
}
