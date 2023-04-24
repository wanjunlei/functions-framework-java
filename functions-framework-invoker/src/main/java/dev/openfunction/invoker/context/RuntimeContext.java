/*
Copyright 2022 The OpenFunction Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package dev.openfunction.invoker.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfunction.functions.*;
import dev.openfunction.invoker.Callback;
import dev.openfunction.invoker.runtime.JsonEventFormat;
import dev.openfunction.invoker.tracing.OpenTelemetryProvider;
import dev.openfunction.invoker.tracing.SkywalkingProvider;
import dev.openfunction.invoker.tracing.TracingProvider;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RuntimeContext {

    private static final Logger logger = Logger.getLogger("dev.openfunction.invoker");

    static final String PodNameEnvName = "POD_NAME";
    static final String PodNamespaceEnvName = "POD_NAMESPACE";
    public static final String SyncRuntime = "Knative";
    public static final String AsyncRuntime = "Async";

    private static final String TracingSkywalking = "skywalking";
    private static final String TracingOpentelemetry = "opentelemetry";

    private final FunctionContext functionContext;
    private final int port;

    private Map<String, Plugin> prePlugins;
    private Map<String, Plugin> postPlugins;
    private TracingProvider tracingProvider;

    public RuntimeContext(String context, ClassLoader classLoader) throws Exception {
        functionContext = new ObjectMapper().readValue(context, FunctionContext.class);

        prePlugins = new HashMap<>();
        postPlugins = new HashMap<>();

        port = Integer.parseInt(functionContext.getPort());

        loadPlugins(classLoader);

        if (functionContext.isTracingEnabled() && functionContext.getPluginsTracing().getProvider() != null) {
            String provider = functionContext.getPluginsTracing().getProvider().getName();
            if (!Objects.equals(provider, TracingSkywalking) && !Objects.equals(provider, TracingOpentelemetry)) {
                throw new IllegalArgumentException("unsupported tracing provider " + provider);
            }

            switch (provider) {
                case TracingSkywalking:
                    tracingProvider = new SkywalkingProvider();
                case TracingOpentelemetry:
                    tracingProvider = new OpenTelemetryProvider(functionContext.getPluginsTracing(),
                            getName(),
                            System.getenv(RuntimeContext.PodNameEnvName),
                            System.getenv(RuntimeContext.PodNamespaceEnvName));
            }
        }

        EventFormatProvider.getInstance().registerFormat(new JsonEventFormat());
    }

    private void loadPlugins(ClassLoader classLoader) {
        prePlugins = loadPlugins(classLoader, functionContext.getPrePlugins());
        postPlugins = loadPlugins(classLoader, functionContext.getPostPlugins());
    }

    private Map<String, Plugin> loadPlugins(ClassLoader classLoader, String[] pluginNames) {
        Map<String, Plugin> plugins = new HashMap<>();
        if (pluginNames == null) {
            return plugins;
        }

        for (String name : pluginNames) {
            if (Objects.equals(name, TracingOpentelemetry) || Objects.equals(name, TracingSkywalking)) {
                continue;
            }

            try {
                Class<?> pluginClass = classLoader.loadClass(name);
                Class<? extends Plugin> pluginImplClass = pluginClass.asSubclass(Plugin.class);
                plugins.put(name, pluginImplClass.getConstructor().newInstance());
            } catch (Exception e) {
                logger.log(Level.WARNING, "load plugin " + name + " error, " + e.getMessage());
                e.printStackTrace();
            }
        }

        return plugins;
    }

    public Map<String, Plugin> getPrePlugins() {
        return prePlugins;
    }

    public Map<String, Plugin> getPostPlugins() {
        return postPlugins;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return functionContext.getName();
    }

    public String getRuntime() {
        return functionContext.getRuntime();
    }

    public Map<String, Component> getInputs() {
        return functionContext.getInputs();
    }

    public Map<String, Component> getOutputs() {
        return functionContext.getOutputs();
    }

    public Map<String, Component> getStates() {
        return functionContext.getStates();
    }

    public TracingProvider getTracingProvider() {
        return tracingProvider;
    }

    public void executeWithTracing(Object obj, Callback callback) throws Exception {
        if (tracingProvider != null) {
            if (obj == null) {
                tracingProvider.executeWithTracing(callback);
            } else if (obj instanceof HttpRequest) {
                tracingProvider.executeWithTracing((HttpRequest) obj, callback);
            } else if (obj instanceof CloudEvent) {
                tracingProvider.executeWithTracing((CloudEvent) obj, callback);
            } else if (obj.getClass().isAssignableFrom(TopicEvent.class)) {
                tracingProvider.executeWithTracing((TopicEvent) obj, callback);
            } else if (obj.getClass().isAssignableFrom(BindingEvent.class)) {
                tracingProvider.executeWithTracing((BindingEvent) obj, callback);
            } else if (obj.getClass().isAssignableFrom(UserContext.class)) {
                tracingProvider.executeWithTracing((UserContext) obj, callback);
            } else if (obj instanceof Plugin) {
                tracingProvider.executeWithTracing((Plugin) obj, callback);
            }
        } else {
            Error error = callback.execute();
            if (error != null) {
                logger.log(Level.WARNING, "execute failed, ", error);
            }
        }
    }
}
