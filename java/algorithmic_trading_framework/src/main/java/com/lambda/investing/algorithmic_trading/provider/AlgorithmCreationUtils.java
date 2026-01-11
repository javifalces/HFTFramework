package com.lambda.investing.algorithmic_trading.provider;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class AlgorithmCreationUtils {
    private final List<AlgorithmProvider> providers;
    private static AlgorithmCreationUtils instance;

    private static volatile Object lockObject = new Object();


    public static AlgorithmCreationUtils getInstance() {
        synchronized (lockObject) {
            if (instance == null) {
                instance = new AlgorithmCreationUtils(new ArrayList<>());
            }
            return instance;
        }
    }


    public void addProvider(AlgorithmProvider provider) {
        synchronized (providers) {
            providers.add(provider);
            System.out.println("AlgorithmCreationUtils: Provider added: " + provider.getClass().getSimpleName() + " (total: " + providers.size() + ")");
        }
    }

    private AlgorithmCreationUtils(List<AlgorithmProvider> providers) {
        this.providers = providers;
    }

    public Algorithm getAlgorithm(String algorithmName, Map<String, Object> parameters) {
        return getAlgorithm(null, algorithmName, parameters);
    }

    public Algorithm getAlgorithm(
            AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
            String algorithmName,
            Map<String, Object> parameters) {

        System.out.println("AlgorithmCreationUtils.getAlgorithm: Looking for algorithm '" + algorithmName + "' in " + providers.size() + " providers");

        for (AlgorithmProvider provider : providers) {
            System.out.println("  - Checking provider: " + provider.getClass().getSimpleName() + " supports='" + algorithmName + "'? " + provider.supports(algorithmName));
            if (provider.supports(algorithmName)) {
                System.out.println("  - Provider " + provider.getClass().getSimpleName() + " supports algorithm '" + algorithmName + "', creating...");
                return provider.createAlgorithm(algorithmConnectorConfiguration, algorithmName, parameters);
            }
        }


        System.err.println("AlgorithmCreationUtils: algorithm " + algorithmName + " not found in " + providers.size() + " AlgorithmProviders");
        return null;
    }


}