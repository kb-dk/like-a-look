package dk.kb.likealook.api.impl;

import dk.kb.likealook.config.ServiceConfig;
import dk.kb.util.yaml.YAML;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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
        ServiceConfig.setConfig(config);
        
        assertEquals(24, DANERData.size(), "The amount of loaded portraits should be as expected");
    }

    @Test
    void testDateParsing() {
        String[][] TESTS = new String[][]{
                { "1856-12", "12-1856"},
                { "1856-09-07", "7-9-1856"},
                { "1856-10-07", "7/10/1856"},
                { "1856-11-07", "7.11.1856"},
                { "1925-06-26", "26.6.1925"}
        };

        for (String[] test: TESTS) {
            assertEquals(test[0], DANERData.parseDate(test[1]));
        }
    }
}