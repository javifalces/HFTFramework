package com.lambda.investing.algorithmic_trading.trend_predictor;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OnnxModelTest {
    String model = "script_nn.onnx";
    int inputs = 15;
    int rows = 10;
    URL res = getClass().getClassLoader().getResource(model);
    File file = Paths.get(res.toURI()).toFile();
    OnnxModel onnxModel = new OnnxModel(new String[]{file.getPath()}, 1, "dense_input");


    String model1 = "script_normalizer.onnx";
    URL res1 = getClass().getClassLoader().getResource(model1);
    File file1 = Paths.get(res1.toURI()).toFile();
    OnnxNormalizer onnxNormalizer = new OnnxNormalizer(file1.getPath());

    @Before
    public void setUp() throws Exception {

    }

    double[][] input = new double[][]{
            {0.14783452040667455, 0.43203349441552263, 0.47955821062699655, 0.7152227250901313, 0.24882092034088388},
            {0.9143624798239866, 0.02835484048879977, 0.6346503389223118, 0.8374419941087844, 0.6347395661592008},
            {0.8254188281053058, 0.8616778825432797, 0.13277359729415195, 0.43391256237282305, 0.2741521355649923},
            {0.413581019070303, 0.9416764717878483, 0.9988426505653405, 0.41103934475207593, 0.3769612998903893},
            {0.3941851977267813, 0.2790301218208199, 0.7086053564407157, 0.4247390572912215, 0.6727461472154954},
            {0.6325476386877091, 0.5979286422411888, 0.5573292266916681, 0.13347795477559976, 0.41954617443486153},
            {0.0403576596958084, 0.3057140140328024, 0.1334376175311207, 0.5516897048973607, 0.07606154186891667},
            {0.18808568837910122, 0.9590173561805687, 0.30193804930200396, 0.5372665506571417, 0.5614578730683532},
            {0.4043855137364104, 0.6217456293939169, 0.5736198957980541, 0.2899707741207167, 0.2913053075497355},
            {0.6909230748977117, 0.6927651391827442, 0.9939838235979486, 0.609766419845345, 0.5741963646642629}

    };

    double[] output = new double[]{
            0.797665,
            1.0436367,
            0.7877561,
            1.0180774,
            0.73097,
            0.44976047,
            0.31759652,
            1.083067,
            0.5323086,
            1.2125093
    };

    public OnnxModelTest() throws URISyntaxException {
        onnxModel.setFloat(true);
    }


    @Test
    public void testOutputRow0() {
        double[] input = new double[]{0.14783452040667455, 0.43203349441552263, 0.47955821062699655, 0.7152227250901313, 0.24882092034088388};
        double[] transformedInput = new double[]{-1.1363307520790897, -0.47727152216143315, -0.2473108743739703, 1.144045927876607, -0.88963017798847};
        double[] outputArr = onnxModel.predict(transformedInput);
        double expectOutput = 0.8583447;
        double output = outputArr[0];
        Assert.assertEquals(expectOutput, output, 0.1);

    }

    @Test
    public void testOutputRow5() {
        double[] transformedInput = new double[]{0.5993642702998995, 0.08843682666111179, 0.020135970525225505, -1.8705968515144102, 0.03547861507401045};
        double[] outputArr = onnxModel.predict(transformedInput);
        double output = outputArr[0];
        double expectOutput = 0.4585249;
        Assert.assertEquals(expectOutput, output, 0.1);
    }

    @Test
    public void testOutputRow4() {
        double[] transformedInput = new double[]{-0.25418081996958336, -0.9990184593641297, 0.5403596432659866, -0.3612611008183578, 1.407493098025703};
        double[] outputArr = onnxModel.predict(transformedInput);
        double output = outputArr[0];
        double expectOutput = 0.73097;
        Assert.assertEquals(expectOutput, output, 0.1);
    }

    @Test
    public void testOutputRow9() {
        double[] transformedInput = new double[]{0.808399161738834, 0.4118326510483353, 1.5217480126798932, 0.5975638529701762, 0.8734814728776426};
        double[] outputArr = onnxModel.predict(transformedInput);
        double output = outputArr[0];
        double expectOutput = 1.2125093;
        Assert.assertEquals(expectOutput, output, 0.1);
    }

    private double predictComplete(double[] input) throws URISyntaxException {
//        String model = "script_normalizer.onnx";
//        URL res = getClass().getClassLoader().getResource(model);
//        File file = Paths.get(res.toURI()).toFile();
//        OnnxNormalizer onnxNormalizer = new OnnxNormalizer(file.getPath());

        double[] transformedInput = onnxNormalizer.transform(input);

        double[] outputArr = onnxModel.predict(transformedInput);
        double output = outputArr[0];
        return output;
    }

    @Test
    public void testOutputRow9Normalization() throws URISyntaxException {
        double[] input = new double[]{0.6909230748977117, 0.6927651391827442, 0.9939838235979486, 0.609766419845345, 0.5741963646642629};
        double output = predictComplete(input);
        double expectOutput = 1.2125093;
        Assert.assertEquals(expectOutput, output, 0.1);
    }

    @Test
    public void testOutputRowSequential() throws URISyntaxException {
        for (int row = 0; row < this.input.length; row++) {
            double[] input = this.input[row];
            double output = predictComplete(input);
            double expectOutput = this.output[row];
            Assert.assertEquals(expectOutput, output, 0.1);
        }

    }

    @Test
    public void testOutputRowParallel() throws URISyntaxException, InterruptedException {
        int timesClone = 5;
        int nJobs = 4;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nJobs);


        double[][] inputBigger = new double[this.input.length * timesClone][this.input[0].length];
        double[] outputBigger = new double[this.output.length * timesClone];
        int rowBigger = 0;
        for (int i = 0; i < timesClone; i++) {
            for (int row = 0; row < this.input.length; row++) {
                inputBigger[rowBigger] = this.input[row];
                outputBigger[rowBigger] = this.output[row];
                rowBigger++;
            }

        }


        double[] outputsPrediction = new double[outputBigger.length];
        for (int row = 0; row < inputBigger.length; row++) {
            double[] input = inputBigger[row];
            int finalRow = row;
            executor.submit(() -> {
                try {
                    outputsPrediction[finalRow] = predictComplete(input);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        int smallRow = 0;
        for (int row = 0; row < outputBigger.length; row++) {

            double expectOutput = this.output[smallRow];

            double output = outputsPrediction[row];
            Assert.assertEquals(expectOutput, output, 0.1);

            smallRow++;
            if (smallRow >= this.output.length) {
                smallRow = 0;
            }
        }

    }

}



