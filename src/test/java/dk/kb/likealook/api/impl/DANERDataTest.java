package dk.kb.likealook.api.impl;

import dk.kb.util.yaml.YAML;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
class DANERDataTest {

    @Test
    void testparsing() {
        YAML config = YAML.parse(new ByteArrayInputStream(
                ("likealook:\n" +
                 "  daner:\n" +
                 "     csv: 'daner_metadata.csv'\n"
                ).getBytes(StandardCharsets.UTF_8)));
        DANERData dd = new DANERData(config);
        assertEquals(26, dd.size(), "The amount of loadedportraits should be as expected");
    }
}