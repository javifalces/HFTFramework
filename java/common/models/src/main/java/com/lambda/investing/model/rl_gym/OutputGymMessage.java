package com.lambda.investing.model.rl_gym;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OutputGymMessage {
    private boolean done;
    private double[] state;
    private double reward;
    private Map<String, String> info;
}