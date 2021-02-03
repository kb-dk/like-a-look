package dk.kb.likealook.ml;/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/


import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;


/** Sample use of the TensorFlow Java API to label images using a pre-trained model. */
// Taken from the dsc project at the Royal Danish Libaray
public class LabelImageTest {

    private byte[] graphDef;
    private List<String> labels;


    public static void main(String[] args) throws Exception {
        String modelDir = "/home/te/projects/like-a-look/models/v3.0";
        LabelImageTest li = new LabelImageTest(modelDir);
        ArrayList<ImageDetectionGuess> analyzeImageFromUrl = li.analyzeImageFromUrl("http://ekot.dk/dagbog/pic/2021-01/gi_20210124-1316_8_EigilStofferKamp.jpg");
        for (ImageDetectionGuess guess: analyzeImageFromUrl) {
            System.out.println(guess.getItem() + " " + guess.getProbability());
        }

    }


    public LabelImageTest(String modelDir) throws Exception {
        long start = System.currentTimeMillis();

        graphDef = readAllBytesOrExit(Paths.get(modelDir, "tensorflow_inception_graph.pb"));
        System.out.println("Loading model:" + (System.currentTimeMillis() - start));
        labels = readAllLinesOrExit(Paths.get(modelDir, "imagenet_comp_graph_label_strings.txt"));
        System.out.println("Reading labels:" + (System.currentTimeMillis() - start));

    }

    public ArrayList<ImageDetectionGuess> analyzeImageFromUrl(String imageUrl) throws Exception {

        byte[] imageBytes;
        try {
            imageBytes = IOUtils.toByteArray((new URL(imageUrl)).openStream()); //idiom
        } catch (Exception e) {
            throw new Exception("Error loading image:" + imageUrl);
        }

        ArrayList<ImageDetectionGuess> guesses = new ArrayList<ImageDetectionGuess>();
        try (Tensor image = Tensor.create(imageBytes)) {
            float[] labelProbabilities = executeInceptionGraph(graphDef, image);

            //int bestLabelIdx = maxIndex(labelProbabilities);
            //System.out.println(labelProbabilities.length);

            for (int index = 0; index < labelProbabilities.length && index < labels.size(); index++) {
                ImageDetectionGuess guess = new ImageDetectionGuess();
                guess.setProbability(labelProbabilities[index]);
                guess.setItem(labels.get(index));
                guesses.add(guess);
            }

            Collections.sort(guesses);
            ArrayList<ImageDetectionGuess> top5 = new ArrayList<ImageDetectionGuess>();
            for (int i = 0; i < 5; i++) {
                top5.add(guesses.get(i));
            }
            return top5;
        }
    }

    private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
        try (Graph g = new Graph()) {
            g.importGraphDef(graphDef);
            try (Session s = new Session(g);
                 Tensor result = s.runner().feed("DecodeJpeg/contents", image).fetch("softmax").run().get(0)) {
                final long[] rshape = result.shape();
                if (result.numDimensions() != 2 || rshape[0] != 1) {
                    throw new RuntimeException(
                            String.format(Locale.ENGLISH,
                                    "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                                    Arrays.toString(rshape)));
                }
                int nlabels = (int) rshape[1];
                float[][] dest = new float[1][nlabels];
                result.copyTo(dest);
                return dest[0];
            }
        }
    }


    /*
     * Not used. Use when input is a file
     *
     */
    private static byte[] readAllBytesOrExit(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private static List<String> readAllLinesOrExit(Path path) {
        try {
            return Files.readAllLines(path, Charset.forName("UTF-8"));
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(0);
        }
        return null;
    }


    // In the fullness of time, equivalents of the methods of this class should be auto-generated from
    // the OpDefs linked into libtensorflow_jni.so. That would match what is done in other languages
    // like Python, C++ and Go.
    static class GraphBuilder {

        GraphBuilder(Graph g) {
            this.g = g;
        }

        Output div(Output x, Output y) {
            return binaryOp("Div", x, y);
        }

        Output sub(Output x, Output y) {
            return binaryOp("Sub", x, y);
        }

        Output resizeBilinear(Output images, Output size) {
            return binaryOp("ResizeBilinear", images, size);
        }

        Output expandDims(Output input, Output dim) {
            return binaryOp("ExpandDims", input, dim);
        }

        Output cast(Output value, DataType dtype) {
            return g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build().output(0);
        }

        Output decodeJpeg(Output contents, long channels) {
            return g.opBuilder("DecodeJpeg", "DecodeJpeg")
                    .addInput(contents)
                    .setAttr("channels", channels)
                    .build()
                    .output(0);
        }

        Output constant(String name, Object value) {
            try (Tensor t = Tensor.create(value)) {
                return g.opBuilder("Const", name)
                        .setAttr("dtype", t.dataType())
                        .setAttr("value", t)
                        .build()
                        .output(0);
            }
        }

        private Output binaryOp(String type, Output in1, Output in2) {
            return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
        }

        private Graph g;
    }

    public class ImageDetectionGuess implements Comparable<ImageDetectionGuess> {

        private String item;
        private double probability = 0;

        public ImageDetectionGuess() {
        }

        public String getItem() {
            return item;
        }


        public void setItem(String item) {
            this.item = item;
        }


        public double getProbability() {
            return probability;
        }


        public void setProbability(double probability) {
            this.probability = probability;
        }


        @Override
        public int compareTo(ImageDetectionGuess other) {
            return ((Double) other.probability).compareTo((Double) this.probability);
        }

    }

}