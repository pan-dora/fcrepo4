package org.fcrepo.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

public class MetricsHealthCheckContextListener extends HealthCheckServlet.ContextListener {

    /**
     * Provide a health-check registry
     * TODO actually populate the health-check registry with checks
     * @return a new health check registry
     */
    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}
