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

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Shape;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.Tensors;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.ConfigProto;
import org.tensorflow.framework.GPUOptions;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://stackoverflow.com/questions/61923351/how-to-invoke-model-from-tensorflow-java/61968561#61968561
 */
public class TensorflowHubTest {
    private static final Logger log = LoggerFactory.getLogger(TensorflowHubTest.class);

    public static void main(String[] args) {
        new TensorflowHubTest().run();
    }

    public void run() {
        try (SavedModelBundle module = loadModule(Paths.get("models/universal-sentence-encoder"), "serve")) {

            //MetaGraphDef graphDef = MetaGraphDef.parseFrom(module.metaGraphDef());
            //System.out.println(graphDef.getMetaInfoDef());

            try (Tensor<String> input = Tensors.create(new byte[][] {
                    "hello".getBytes(StandardCharsets.UTF_8),
                    "world".getBytes(StandardCharsets.UTF_8)
            }))
            {
                MetaGraphDef metadata = MetaGraphDef.parseFrom(module.metaGraphDef());
                Map<String, Shape> nameToInput = getInputToShape(metadata);
                String firstInput = nameToInput.keySet().iterator().next();

                Map<String, Shape> nameToOutput = getOutputToShape(metadata);
                String firstOutput = nameToOutput.keySet().iterator().next();

                System.out.println("input: " + firstInput);
                System.out.println("output: " + firstOutput);
                System.out.println();

                List<Tensor<?>> result = module.session().runner().feed(firstInput, input).
                        fetch(firstOutput).run();
                for (Tensor<?> tensor : result)
                {
                    {
                        float[][] array = new float[tensor.numDimensions()][tensor.numElements() /
                                                                            tensor.numDimensions()];
                        tensor.copyTo(array);
                        System.out.println(Arrays.deepToString(array));
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a graph from a file.
     *
     * @param source the directory containing  to load from
     * @param tags   the model variant(s) to load
     * @return the graph
     * @throws NullPointerException if any of the arguments are null
     * @throws IOException          if an error occurs while reading the file
     */
    protected SavedModelBundle loadModule(Path source, String... tags) throws IOException {
        // https://stackoverflow.com/a/43526228/14731
        try
        {
            return SavedModelBundle.loader(source.toAbsolutePath().normalize().toString()).
                    withTags(tags).
                    withConfigProto(ConfigProto.newBuilder().
                            setGpuOptions(GPUOptions.newBuilder().setAllowGrowth(true)).
                            setAllowSoftPlacement(true).
                            build().toByteArray()).
                    load();
        }
        catch (TensorFlowException e) {
            throw new IOException(e);
        }
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
}
