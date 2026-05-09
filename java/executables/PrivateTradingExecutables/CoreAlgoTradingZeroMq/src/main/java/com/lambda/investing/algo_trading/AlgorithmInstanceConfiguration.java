package com.lambda.investing.algo_trading;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AlgorithmInstanceConfiguration {

    private String[] instrumentPks;
    private AlgorithmConfiguration algorithm;

    // Backward compatibility with "algorithms":[{"algorithmName":"...","parameters":{...}}]
    private String algorithmName;
    private java.util.Map<String, Object> parameters;

    public AlgorithmInstanceConfiguration(String[] instrumentPks, AlgorithmConfiguration algorithm) {
        this.instrumentPks = instrumentPks;
        this.algorithm = algorithm;
    }

    public AlgorithmConfiguration getEffectiveAlgorithm() {
        if (algorithm != null) {
            return algorithm;
        }
        if (algorithmName == null || algorithmName.isEmpty()) {
            return null;
        }
        AlgorithmConfiguration output = new AlgorithmConfiguration();
        output.setAlgorithmName(algorithmName);
        output.setParameters(parameters);
        return output;
    }
}
