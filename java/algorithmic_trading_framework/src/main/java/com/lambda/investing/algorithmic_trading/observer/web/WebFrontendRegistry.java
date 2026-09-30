package com.lambda.investing.algorithmic_trading.observer.web;

import com.lambda.investing.algorithmic_trading.Algorithm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of {@link WebFrontendProvider}s used to choose the web UI frontend depending on the algorithm type.
 * Providers are checked in registration order (explicit {@link #addProvider} first, then {@link ServiceLoader}
 * ones); if none matches, {@link ClasspathWebFrontend#DEFAULT} is returned.
 */
public class WebFrontendRegistry {

    private static final Logger logger = LogManager.getLogger(WebFrontendRegistry.class);

    private static final WebFrontendRegistry INSTANCE = new WebFrontendRegistry();

    private final List<WebFrontendProvider> providers = new CopyOnWriteArrayList<>();
    private volatile boolean serviceLoaderProvidersLoaded = false;

    public static WebFrontendRegistry getInstance() {
        return INSTANCE;
    }

    WebFrontendRegistry() {
    }

    public void addProvider(WebFrontendProvider provider) {
        if (provider == null || providers.contains(provider)) {
            return;
        }
        providers.add(provider);
        logger.info("WebFrontendProvider added: {} (total: {})", provider.getClass().getSimpleName(), providers.size());
    }

    public void removeProvider(WebFrontendProvider provider) {
        providers.remove(provider);
    }

    /**
     * @param algorithm root algorithm of the web observer; {@code null} returns the default frontend
     * @return the frontend of the first provider supporting the algorithm, or {@link ClasspathWebFrontend#DEFAULT}
     */
    public WebFrontend getFrontend(Algorithm algorithm) {
        loadServiceLoaderProviders();
        if (algorithm != null) {
            for (WebFrontendProvider provider : providers) {
                try {
                    if (provider.supports(algorithm)) {
                        WebFrontend frontend = provider.getFrontend(algorithm);
                        if (frontend != null) {
                            logger.info("Using web frontend '{}' from {} for algorithm {}", frontend.getName(),
                                    provider.getClass().getSimpleName(), algorithm.getAlgorithmInfo());
                            return frontend;
                        }
                    }
                } catch (Exception e) {
                    logger.error("WebFrontendProvider {} failed for algorithm {} -> skipping",
                            provider.getClass().getSimpleName(), algorithm.getAlgorithmInfo(), e);
                }
            }
        }
        return ClasspathWebFrontend.DEFAULT;
    }

    private void loadServiceLoaderProviders() {
        if (serviceLoaderProvidersLoaded) {
            return;
        }
        synchronized (this) {
            if (serviceLoaderProvidersLoaded) {
                return;
            }
            try {
                for (WebFrontendProvider provider : ServiceLoader.load(WebFrontendProvider.class)) {
                    addProvider(provider);
                }
            } catch (ServiceConfigurationError e) {
                logger.error("Error loading WebFrontendProvider services", e);
            }
            serviceLoaderProvidersLoaded = true;
        }
    }
}
