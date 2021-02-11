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
import org.nd4j.shade.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Shape;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.ConfigProto;
import org.tensorflow.framework.GPUOptions;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;
import org.tensorflow.op.math.Sub;

import javax.validation.constraints.Null;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * https://tfhub.dev/google/imagenet/inception_v3/classification/4
 */
public class SubjectHubTest implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(FaceHandler.class);

    private SavedModelBundle module;
    private Graph graph;

    public static void main(String[] args) throws IOException {
        List<SubjectDto> subjects = new SubjectHubTest().detect(
                Resolver.resolveStream("pexels-andrea-piacquadio-3812743.jpg"), "Test", 10);
        System.out.println(subjects);
    }

    public SubjectHubTest() throws IOException {
        ServiceConfig.initialize("conf/like-a-look-*.yaml");
        YAML setup = ServiceConfig.getConfig().getSubMap(".likealook.detect.subject.hubinception3");

        module = loadModule(Paths.get(setup.getString(".model")), "serve");
        graph = module.graph();

        MetaGraphDef metadata = MetaGraphDef.parseFrom(module.metaGraphDef());
        Map<String, SignatureDef> signatureDefMap = metadata.getSignatureDefMap();
        Set<String> keys = signatureDefMap.keySet();
        System.out.println("Signature keys: " + keys);

        try {
            dumpBundle(module);
        } catch (Exception e) {
            System.out.println("Exception dumping model 1");
        }
/*        try {
            dumpBundle2(module);
        } catch (Exception e) {
            System.out.println("Exception dumping model 2");
        }*/

/*        Map<String, Shape> nameToInput = getInputToShape(metadata);
        if (nameToInput != null) {
            String firstInput = nameToInput.keySet().iterator().next();
            System.out.println("input: " + firstInput);
        }*/

/*        Map<String, Shape> nameToOutput = getOutputToShape(metadata);
        if (nameToOutput != null) {
            String firstOutput = nameToOutput.keySet().iterator().next();
            System.out.println("output: " + firstOutput);
        }*/

        //System.out.println(metaGraphDef.getAssetFileDefList());
        //graph.operations().forEachRemaining(op -> System.out.println(op.name()));
    }

//    private void dumpBundle2(SavedModelBundle module) {
//        SignatureDef modelInfo = module.metaGraphDef().getSignatureDefMap().get("serving_default");
//    }

    private void dumpBundle(SavedModelBundle bundle) throws InvalidProtocolBufferException {
        //final String defKey = "serving_default";
        final String defKey = "serving_default_input";
        final MetaGraphDef metaGraphDef = MetaGraphDef.parseFrom(bundle.metaGraphDef());
        final SignatureDef signatureDef = metaGraphDef.getSignatureDefMap().get(defKey);

        if (signatureDef == null) {
            throw new NullPointerException("No defmap for '" + defKey + "'");
        }

        final TensorInfo inputTensorInfo = signatureDef.getInputsMap()
            .values()
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Sorry, no input"));

        System.out.println("Input: " + inputTensorInfo.getName());
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
            return IntStream.range(0, labelProbabilities.length-1)
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
            //Tensor result = s.runner().feed("DecodeJpeg/contents", image).fetch("softmax").run().get(0)) {
            Tensor result = s.runner().feed("input", image).fetch("softmax").run().get(0)) {
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
        guess.setSubject("Index" + index); // TODO: Implement this
        guess.setSourceID(sourceID);
        return guess;
    }

    @Override
    public void close() throws IOException {
        if (graph != null) {
            graph.close();
        }
    }


    protected SavedModelBundle loadModule(Path source, String... tags) throws IOException {
        // https://stackoverflow.com/a/43526228/14731
        try {
            SavedModelBundle.Loader loader = SavedModelBundle.loader(source.toAbsolutePath().normalize().toString());
            System.out.println(loader);
            return loader.
                    withTags(tags).
                    withConfigProto(ConfigProto.newBuilder().
                            setGpuOptions(GPUOptions.newBuilder().setAllowGrowth(true)).
                            setAllowSoftPlacement(true).
                            build().toByteArray()).
                    load();
        } catch (TensorFlowException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param metadata the graph metadata
     * @return a map from an input name to its shape
     */
    protected Map<String, Shape> getInputToShape(MetaGraphDef metadata)
    {
        Map<String, Shape> result = new HashMap<>();
        SignatureDef servingDefault = getServingSignature(metadata);
        for (Map.Entry<String, TensorInfo> entry : servingDefault.getInputsMap().entrySet())
        {
            TensorShapeProto shapeProto = entry.getValue().getTensorShape();
            List<TensorShapeProto.Dim> dimensions = shapeProto.getDimList();
            long firstDimension = dimensions.get(0).getSize();
            long[] remainingDimensions = dimensions.stream().skip(1).mapToLong(TensorShapeProto.Dim::getSize).toArray();
            Shape shape = Shape.make(firstDimension, remainingDimensions);
            result.put(entry.getValue().getName(), shape);
        }
        return result;
    }
    /**
     * @param metadata the graph metadata
     * @return a map from an output name to its shape
     */
    protected Map<String, Shape> getOutputToShape(MetaGraphDef metadata)
    {
        Map<String, Shape> result = new HashMap<>();
        SignatureDef servingDefault = getServingSignature(metadata);
        for (Map.Entry<String, TensorInfo> entry : servingDefault.getOutputsMap().entrySet())
        {
            TensorShapeProto shapeProto = entry.getValue().getTensorShape();
            List<TensorShapeProto.Dim> dimensions = shapeProto.getDimList();
            long firstDimension = dimensions.get(0).getSize();
            long[] remainingDimensions = dimensions.stream().skip(1).mapToLong(TensorShapeProto.Dim::getSize).toArray();
            Shape shape = Shape.make(firstDimension, remainingDimensions);
            result.put(entry.getValue().getName(), shape);
        }
        return result;
    }
    /**
     * @param metadata the graph metadata
     * @return the output signature
     */
    private SignatureDef getServingSignature(MetaGraphDef metadata)
    {
        return metadata.getSignatureDefOrDefault("serving_default", getFirstSignature(metadata));
    }
    /**
     * @param metadata the graph metadata
     * @return the first signature, or null
     */
    private SignatureDef getFirstSignature(MetaGraphDef metadata)
    {
        Map<String, SignatureDef> nameToSignature = metadata.getSignatureDefMap();
        if (nameToSignature.isEmpty())
            return null;
        return nameToSignature.get(nameToSignature.keySet().iterator().next());
    }
}
