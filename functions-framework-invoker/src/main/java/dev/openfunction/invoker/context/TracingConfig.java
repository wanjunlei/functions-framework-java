package dev.openfunction.invoker.context;

import java.time.Duration;
import java.util.Map;

public class TracingConfig {
    private boolean enabled;
    private Provider provider;
    private Map<String, String> tags;
    private Map<String, String> baggage;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getBaggage() {
        return baggage;
    }

    public void setBaggage(Map<String, String> baggage) {
        this.baggage = baggage;
    }

    public static class Provider {
        private String name;
        private String oapServer;
        private Exporter exporter;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Exporter getExporter() {
            return exporter;
        }

        public void setExporter(Exporter exporter) {
            this.exporter = exporter;
        }

        public String getOapServer() {
            return oapServer;
        }

        public void setOapServer(String oapServer) {
            this.oapServer = oapServer;
        }
    }

    public static class Exporter {
        private String name;
        private String endpoint;
        private Map<String, String> headers;
        private String compression;
        private Duration timeout;
        private String protocol;


        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public String getCompression() {
            return compression;
        }

        public void setCompression(String compression) {
            this.compression = compression;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
