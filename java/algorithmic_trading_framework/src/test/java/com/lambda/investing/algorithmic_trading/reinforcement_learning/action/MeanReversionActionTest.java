package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;


public class MeanReversionActionTest {

    MeanReversionAction meanReversionAction;

    public MeanReversionActionTest() {
        int[] periods = new int[]{1, 2};
        double[] upperBounds = new double[]{60, 70};
        double[] upperBoundsExit = new double[]{55};
        double[] lowerBounds = new double[]{30, 40};
        double[] lowerBoundsExit = new double[]{45};
        int[] changeSide = new int[]{0};
        int[] levelQuote = new int[]{0};
        this.meanReversionAction = new MeanReversionAction(periods, upperBounds, upperBoundsExit, lowerBounds, lowerBoundsExit, changeSide, levelQuote);
    }

    @Test
    public void testGetActionIndex() {
        for (int repeat = 0; repeat < 5; repeat++) {
            System.out.println("Repeat " + repeat);
            int randomNum = ThreadLocalRandom.current().nextInt(0, this.meanReversionAction.getNumberActions());
            double[] actionValue = this.meanReversionAction.getAction(randomNum);
            int positionGet = this.meanReversionAction.getAction(actionValue);
            Assert.assertEquals(positionGet, randomNum);
        }

    }

}