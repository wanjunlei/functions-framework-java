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

import dev.openfunction.functions.Component;

import java.util.Map;

class FunctionContext {

    private String name;
    private String version;
    private Map<String, Component> inputs;
    private Map<String, Component> outputs;
    private String runtime;
    private String port;

    private String[] prePlugins;
    private String[] postPlugins;
    private TracingConfig pluginsTracing;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Component> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Component> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Component> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Component> outputs) {
        this.outputs = outputs;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String[] getPrePlugins() {
        return prePlugins;
    }

    public void setPrePlugins(String[] prePlugins) {
        this.prePlugins = prePlugins;
    }

    public String[] getPostPlugins() {
        return postPlugins;
    }

    public void setPostPlugins(String[] postPlugins) {
        this.postPlugins = postPlugins;
    }

    public TracingConfig getPluginsTracing() {
        return pluginsTracing;
    }

    public void setPluginsTracing(TracingConfig pluginsTracing) {
        this.pluginsTracing = pluginsTracing;
    }

    public boolean isTracingEnabled() {
        return pluginsTracing != null && pluginsTracing.isEnabled();
    }
}
