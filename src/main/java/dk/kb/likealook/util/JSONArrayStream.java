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
package dk.kb.likealook.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public class JSONArrayStream implements StreamingOutput {
    private final List<?> content;

    public JSONArrayStream(List<?> content) {
        this.content = content;
    }

    @Override
    public void write(final OutputStream outputStream) throws IOException, WebApplicationException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonFactory jsonFactory = objectMapper.getFactory();
        try (final JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream)) {
            jsonGenerator.writeStartArray();
            for (Object o: content) {
                jsonGenerator.writeObject(o);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
        }
    }
}
