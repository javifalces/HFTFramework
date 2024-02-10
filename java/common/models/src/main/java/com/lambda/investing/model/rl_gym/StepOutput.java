package com.lambda.investing.model.rl_gym;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class StepOutput {
    private double[] state;
    private double reward;
}