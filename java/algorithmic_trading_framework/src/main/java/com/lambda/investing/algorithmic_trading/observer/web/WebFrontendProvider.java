package com.lambda.investing.algorithmic_trading.observer.web;

import com.lambda.investing.algorithmic_trading.Algorithm;

/**
 * Selects a custom {@link WebFrontend} depending on the algorithm type.
 * <p>
 * Mirrors {@link com.lambda.investing.algorithmic_trading.provider.AlgorithmProvider}: external libraries
 * register an implementation in {@link WebFrontendRegistry}, either from a Spring {@code @Component}
 * ({@code @PostConstruct -> WebFrontendRegistry.getInstance().addProvider(this)}) or through
 * {@link java.util.ServiceLoader} ({@code META-INF/services/com.lambda.investing.algorithmic_trading.observer.web.WebFrontendProvider}).
 */
public interface WebFrontendProvider {

    /**
     * @param algorithm root algorithm registered in the web observer (may be a {@code MultiAlgorithm})
     * @return {@code true} if this provider has a frontend for the algorithm
     */
    boolean supports(Algorithm algorithm);

    /**
     * Called only when {@link #supports(Algorithm)} returned {@code true}.
     */
    WebFrontend getFrontend(Algorithm algorithm);
}
