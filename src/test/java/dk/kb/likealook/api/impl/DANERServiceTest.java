package dk.kb.likealook.api.impl;

import dk.kb.likealook.TestHelper;
import org.junit.jupiter.api.Test;

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
class DANERServiceTest {

    //@Test
    void testRemote() {
        // https://commons.wikimedia.org/wiki/Face#/media/File:Aj_young_425625.jpg
        final String sourceURL = "https://upload.wikimedia.org/wikipedia/commons/6/6f/Christopher_Meloni_croped_face.png";

        TestHelper.initTestSetup();

        DANERService.findSimilarRemoteSingle("someID", sourceURL, 10)
                .forEach(System.out::println);
    }

    // Requires daner-face-search to be running locally

    @Test
    void testRemoteV2() {
        // https://commons.wikimedia.org/wiki/Face#/media/File:Aj_young_425625.jpg
        final String sourceURL = "http://localhost:8234/daner-face-search/thispersondoesnotexist.com.jpg";

        TestHelper.initTestSetup();

        System.out.println(DANERService.findSimilarRemoteMultiV2("someID", sourceURL, 10));
    }

}