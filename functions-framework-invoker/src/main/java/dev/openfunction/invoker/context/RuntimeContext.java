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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.openfunction.functions.Component;
import dev.openfunction.functions.Plugin;
import dev.openfunction.invoker.plugins.OpenTelemetryPlugin;
import dev.openfunction.invoker.plugins.SkywalkingPlugin;
import kotlin.Pair;
import org.eclipse.jetty.util.ArrayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RuntimeContext {

    private static final Logger logger = Logger.getLogger("dev.openfunction.invoker");
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final String ModeEnvName = "CONTEXT_MODE";
    private static final String KubernetesMode = "kubernetes";
    private static final String SelfHostMode = "self-host";
    private static final String PodNameEnvName = "POD_NAME";
    private static final String PodNamespaceEnvName = "POD_NAMESPACE";
    private static final String TracingSkywalking = "skywalking";
    private static final String TracingOpentelemetry = "opentelemetry";

    private final FunctionContext functionContext;
    private String mode;
    private final int port;
    private String pod;
    private String namespace;

    private Map<String, Plugin> prePlugins;
    private Map<String, Plugin> postPlugins;
    private Plugin tracingPlugin;

    public RuntimeContext(String context, ClassLoader classLoader) throws Exception {

        functionContext = GSON.getAdapter(FunctionContext.class).fromJson(context);

        prePlugins = new HashMap<>();
        postPlugins = new HashMap<>();

        port = Integer.parseInt(functionContext.getPort());

        mode = System.getenv(ModeEnvName);
        if (!Objects.equals(mode, SelfHostMode)) {
            mode = KubernetesMode;
        }

        if (mode.equals(KubernetesMode)) {
            pod = System.getenv(PodNameEnvName);
            if (pod == null || pod.isEmpty()) {
                throw new Error("environment variable `POD_NAME` not found");
            }

            namespace = System.getenv(PodNamespaceEnvName);
            if (pod == null || pod.isEmpty()) {
                throw new Error("environment variable `POD_NAMESPACE` not found");
            }
        }

        loadPlugins(classLoader);
    }

    private void loadPlugins(ClassLoader classLoader) {

        String[] prePluginNames = ArrayUtil.add(functionContext.getPrePlugins(), null);
        String[] postPluginNames = ArrayUtil.add(functionContext.getPostPlugins(), null);

        if (functionContext.isTracingEnabled()) {
            Pair<String, String> provider = functionContext.getTracingProvider();
            String providerName = provider.component1();
            if (!Objects.equals(providerName, TracingSkywalking) && !Objects.equals(providerName, TracingOpentelemetry)) {
                throw new IllegalArgumentException("unsupported tracing provider " + provider);
            }

            prePluginNames = ArrayUtil.addToArray(prePluginNames, providerName, String.class);
            postPluginNames = ArrayUtil.addToArray(postPluginNames, providerName, String.class);

            Map<String, String> tags = functionContext.getTracingTags();
            tags.put("instance", pod);
            tags.put("namespace", namespace);

            switch (providerName) {
                case TracingSkywalking:
                    tracingPlugin = new SkywalkingPlugin(providerName, tags, functionContext.getTracingBaggage());
                case TracingOpentelemetry:
                    tracingPlugin = new OpenTelemetryPlugin(providerName, tags, functionContext.getTracingBaggage());
            }
        }

        prePlugins = loadPlugins(classLoader, prePluginNames);
        postPlugins = loadPlugins(classLoader, postPluginNames);
    }

    private Map<String, Plugin> loadPlugins(ClassLoader classLoader, String[] pluginNames) {
        Map<String, Plugin> plugins = new HashMap<>();

        if (pluginNames == null) {
            return plugins;
        }

        for (String name : pluginNames) {
            if (Objects.equals(name, TracingSkywalking)) {
                plugins.put(TracingSkywalking, tracingPlugin);
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

    public String getMode() {
        return mode;
    }

    public String getPod() {
        return pod;
    }

    public String getNamespace() {
        return namespace;
    }
}
