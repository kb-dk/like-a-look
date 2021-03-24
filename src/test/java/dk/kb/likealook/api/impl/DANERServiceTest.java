package dk.kb.likealook.api.impl;

import dk.kb.likealook.TestHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        final String sourceURL = "Needonehere"; // TODO: Find a proper image we can link to

        TestHelper.initTestSetup();

        DANERService.findSimilarRemote("someID", sourceURL, 10)
                .forEach(System.out::println);
    }

}