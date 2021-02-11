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

import dk.kb.likealook.api.impl.FaceHandler;
import dk.kb.likealook.config.ServiceConfig;
import dk.kb.likealook.model.SubjectDto;
import dk.kb.util.Resolver;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
public class SubjectNonHubTest implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(FaceHandler.class);

    private byte[] graphDef;
    private List<String> labels;
    private Graph graph;

    public static void main(String[] args) throws IOException {
        List<SubjectDto> subjects = new SubjectNonHubTest().detect(
                Resolver.resolveStream("pexels-andrea-piacquadio-3812743.jpg"), "Test", 10);
        System.out.println(subjects);
    }

    public SubjectNonHubTest() throws IOException {
        ServiceConfig.initialize("conf/like-a-look-*.yaml");
        YAML setup = ServiceConfig.getConfig().getSubMap(".likealook.detect.subject.inception3");

        
        String graphPath;  // /<localpath>/models/v3.0/tensorflow_inception_graph.pb
        graphPath = setup.getString(".graph");
        log.info("Loading graph definition from '{}'", graphPath);
        graphDef = Files.readAllBytes(Path.of(graphPath));
        graph = new Graph();
        graph.importGraphDef(graphDef);

        Graph g = new Graph();
        g.importGraphDef(graphDef);


        String labelsPath;// /<localpath>/models/v3.0/imagenet_comp_graph_label_strings.txt
        labelsPath = setup.getString(".labels");
        log.info("Loading labels definition from '{}'", labelsPath);
        labels = Files.readAllLines(Path.of(labelsPath), StandardCharsets.UTF_8);
    }

    private List<SubjectDto> detect(InputStream imageStream, String sourceID, int maxSize) throws IOException {
        byte[] imageBytes;
        try {
            imageBytes = IOUtils.toByteArray(imageStream);
        } catch (IOException e) {
            throw new IOException("Unable to load image stream into memory for '" + sourceID + "'");
        }
        try (Tensor image = Tensor.create(imageBytes)) {
            float[] labelProbabilities = executeGraph(image);
            return IntStream.range(0, Math.min(labelProbabilities.length-1, labels.size()-1))
                    .boxed()
                    .map(index -> indexToSubject(index, labelProbabilities, sourceID))
                    .sorted(Comparator.comparingDouble(SubjectDto::getConfidence).reversed())
                    .limit(maxSize)
                    .collect(Collectors.toList());
        }
    }

    private float[] executeGraph(Tensor image) {
        try (Session s = new Session(graph);
            // TODO: Expand this to generic image handling
            Tensor result = s.runner().feed("DecodeJpeg/contents", image).fetch("softmax").run().get(0)) {
            final long[] rshape = result.shape();
            if (result.numDimensions() != 2 || rshape[0] != 1) {
                throw new RuntimeException(String.format(
                        Locale.ENGLISH, "Expected model to produce a [1 N] shaped tensor where N is the number " +
                                        "of labels, instead it produced one with shape %s",
                        Arrays.toString(rshape)));
            }
            int nlabels = (int) rshape[1];
            float[][] dest = new float[1][nlabels];
            result.copyTo(dest);
            return dest[0];
        }
    }

    private SubjectDto indexToSubject(int index, float[] labelProbabilities, String sourceID) {
        SubjectDto guess = new SubjectDto();
        guess.setConfidence(labelProbabilities[index]);
        guess.setSubject(labels.get(index));
        guess.setSourceID(sourceID);
        return guess;
    }

    @Override
    public void close() throws IOException {
        if (graph != null) {
            graph.close();
        }
    }
}
