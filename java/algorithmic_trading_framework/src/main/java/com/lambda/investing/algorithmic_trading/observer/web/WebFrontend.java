package com.lambda.investing.algorithmic_trading.observer.web;

/**
 * Frontend (HTML + static assets) served by {@link AlgorithmWebServer}.
 * <p>
 * The default implementation is {@link ClasspathWebFrontend#DEFAULT}, the dashboard bundled with the
 * framework. Custom frontends are selected per algorithm type through a {@link WebFrontendProvider}
 * registered in {@link WebFrontendRegistry}.
 */
public interface WebFrontend {

    /**
     * @return a human readable name, used for logging
     */
    String getName();

    /**
     * @return the HTML served on {@code /} and {@code /index.html}; never {@code null}
     */
    String getDashboardHtml();

    /**
     * Loads a static asset requested by the browser.
     *
     * @param resourcePath path relative to the web root without leading slash, e.g. {@code css/base.css}.
     *                     Already validated against path traversal.
     * @return the asset bytes, or {@code null} if this frontend does not provide it (the server then falls
     * back to the default bundled assets)
     */
    byte[] getStaticAsset(String resourcePath);
}
