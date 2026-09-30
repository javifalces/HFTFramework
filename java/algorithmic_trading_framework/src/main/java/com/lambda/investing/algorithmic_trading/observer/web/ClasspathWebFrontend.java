package com.lambda.investing.algorithmic_trading.observer.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * {@link WebFrontend} loaded from classpath resources, so it can be packaged inside any external jar.
 * <p>
 * Expected layout under {@code resourceBasePath}:
 * <pre>
 *   &lt;resourceBasePath&gt;/dashboard.html
 *   &lt;resourceBasePath&gt;/css/...
 *   &lt;resourceBasePath&gt;/js/...
 * </pre>
 * Assets missing in {@code resourceBasePath} are served from the default bundled frontend, so a custom
 * frontend can reuse the default {@code css/} and {@code js/} files.
 */
public class ClasspathWebFrontend implements WebFrontend {

    private static final Logger logger = LogManager.getLogger(ClasspathWebFrontend.class);

    public static final String DASHBOARD_FILE = "dashboard.html";

    /**
     * Frontend bundled with the framework, used when no {@link WebFrontendProvider} matches.
     */
    public static final ClasspathWebFrontend DEFAULT = new ClasspathWebFrontend("default", "",
            ClasspathWebFrontend.class.getClassLoader());

    private final String name;
    private final String resourcePrefix;
    private final ClassLoader classLoader;
    private final String dashboardHtml;

    /**
     * Uses the class loader of this class, which sees every jar on the application classpath.
     */
    public ClasspathWebFrontend(String name, String resourceBasePath) {
        this(name, resourceBasePath, ClasspathWebFrontend.class.getClassLoader());
    }

    /**
     * @param name             frontend name, used for logging
     * @param resourceBasePath classpath folder holding {@code dashboard.html}, e.g. {@code web/my_algo};
     *                         empty for the classpath root
     * @param classLoader      class loader used to resolve the resources (e.g. the external library one)
     * @throws IllegalStateException if {@code dashboard.html} is not found
     */
    public ClasspathWebFrontend(String name, String resourceBasePath, ClassLoader classLoader) {
        this.name = name;
        this.resourcePrefix = normalizePrefix(resourceBasePath);
        this.classLoader = classLoader != null ? classLoader : ClasspathWebFrontend.class.getClassLoader();
        byte[] html = loadResource(resourcePrefix + DASHBOARD_FILE);
        if (html == null) {
            throw new IllegalStateException(resourcePrefix + DASHBOARD_FILE + " not found in classpath for frontend " + name);
        }
        this.dashboardHtml = new String(html, StandardCharsets.UTF_8);
    }

    private static String normalizePrefix(String resourceBasePath) {
        if (resourceBasePath == null) {
            return "";
        }
        String prefix = resourceBasePath.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (prefix.isEmpty()) {
            return "";
        }
        if (prefix.contains("..")) {
            throw new IllegalArgumentException("resourceBasePath must not contain '..': " + resourceBasePath);
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDashboardHtml() {
        return dashboardHtml;
    }

    @Override
    public byte[] getStaticAsset(String resourcePath) {
        return loadResource(resourcePrefix + resourcePath);
    }

    private byte[] loadResource(String resource) {
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            return is == null ? null : is.readAllBytes();
        } catch (IOException e) {
            logger.debug("Could not load web resource {}: {}", resource, e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return "ClasspathWebFrontend{name='" + name + "', resourcePrefix='" + resourcePrefix + "'}";
    }
}
