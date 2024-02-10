package com.lambda.investing.model.rl_gym;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InputGymMessage {
    private String type;
    private double[] value;

    public InputGymMessage(InputGymMessageValue inputGymMessageValue) {
        this.type = inputGymMessageValue.getType();
        this.value = new double[]{inputGymMessageValue.getValue()};
    }

}