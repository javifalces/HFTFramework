package com.lambda.investing.algorithmic_trading.trend_predictor;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class OnnxNormalizerTest {
    String model = "script_normalizer.onnx";
    int inputs = 15;
    int rows = 10;
    URL res = getClass().getClassLoader().getResource(model);
    File file = Paths.get(res.toURI()).toFile();
    OnnxNormalizer onnxNormalizer = new OnnxNormalizer(file.getPath());


    public OnnxNormalizerTest() throws URISyntaxException {
    }

    @Test
    public void testOutput1() {
        double[] inputState = new double[]{0.14783452040667455, 0.43203349441552263, 0.47955821062699655, 0.7152227250901313, 0.24882092034088388};
        double[] expectedOutput = new double[]{-1.1363307520790897, -0.47727152216143315, -0.2473108743739703, 1.144045927876607, -0.88963017798847};
        double[] output = onnxNormalizer.transform(inputState);

        Assert.assertArrayEquals(expectedOutput, output, 0.001);
    }

    @Test
    public void testOutput2() {
        double[] inputState = new double[]{0.413581019070303, 0.9416764717878483, 0.9988426505653405, 0.41103934475207593, 0.3769612998903893};
        double[] expectedOutput = new double[]{-0.18472689084901067, 1.260629139775219, 1.5384570388453673, -0.43225398565948564, -0.19527600937579756};
        double[] output = onnxNormalizer.transform(inputState);

        Assert.assertArrayEquals(expectedOutput, output, 0.001);
    }
}
