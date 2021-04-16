package dk.kb.likealook.api.impl;

import dk.kb.likealook.TestHelper;
import dk.kb.likealook.model.SimilarDto;
import dk.kb.likealook.model.SimilarResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;

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
class LikeALookTest {

    @Test
    void testDANERMock() {
        TestHelper.initTestSetup();

        SimilarResponseDto similarResponse = new LikeALook().findSimilarWhole(
                null, "daner_mock", "dummy", 10);
        List<SimilarDto> similars = similarResponse.getElements().get(0).getSimilars();
        assertEquals(10, similars.size(), "Calling similar for 'daner' should yield the right number of results");
        System.out.println(similarResponse);
    }

}