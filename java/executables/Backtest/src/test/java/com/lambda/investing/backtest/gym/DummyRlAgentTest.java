package com.lambda.investing.backtest.gym;

import com.lambda.investing.Configuration;
import com.lambda.investing.backtest.InputConfiguration;
import com.lambda.investing.backtest.UtilsTest;
import com.lambda.investing.gym.DummyRlAgent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;

import static com.lambda.investing.backtest.UtilsTest.AddTestInstruments;
import static com.lambda.investing.model.Util.fromJsonString;

@RunWith(MockitoJUnitRunner.class)
public class DummyRlAgentTest {
    String jsonPath = "test_AlphaAvellanedaStoikov.json";
    String backtestConfigurationJson = null;
    InputConfiguration inputConfiguration = null;
    String lambdaDataPath = "lambda_data";
    String tempDataPath = "temp";

    public DummyRlAgentTest() throws Exception {
        InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(jsonPath);
        backtestConfigurationJson = new String(inputStream.readAllBytes());
        inputConfiguration = fromJsonString(backtestConfigurationJson, InputConfiguration.class);

        String lambdaDataPathCompletePath = Configuration.class.getClassLoader().getResource(lambdaDataPath).getPath();
        String tempPathCompletePath = Configuration.class.getClassLoader().getResource(tempDataPath).getPath();

        UtilsTest.ConfigureEnvironment(lambdaDataPathCompletePath, tempPathCompletePath);
        AddTestInstruments();
    }

    @Test
    public void testIsRlAlgorithm() {
        DummyRlAgent dummyRlAgent = new DummyRlAgent(backtestConfigurationJson);
        Assert.assertTrue(dummyRlAgent.isDummyAgent());
        Assert.assertEquals(2122, dummyRlAgent.getZeroMqConfiguration().getPort());
        Assert.assertEquals("localhost", dummyRlAgent.getZeroMqConfiguration().getHost());
    }

    @Test
    public void testIsNotRlAlgorithm() throws IOException {
        InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("test_AvellanedaStoikov.json");
        String backtestConfigurationJson1 = new String(inputStream.readAllBytes());

        DummyRlAgent dummyRlAgent = new DummyRlAgent(backtestConfigurationJson1);
        Assert.assertFalse(dummyRlAgent.isDummyAgent());
        Assert.assertNull(dummyRlAgent.getZeroMqConfiguration());
    }
}
