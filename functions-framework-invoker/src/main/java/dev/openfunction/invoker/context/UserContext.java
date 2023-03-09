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

import dev.openfunction.functions.*;
import io.cloudevents.CloudEvent;
import io.dapr.client.DaprClient;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserContext implements Context {

    private static final Logger logger = Logger.getLogger("dev.openfunction.invoker");

    private static final Map<String, Boolean> bindingQueueComponents = Map.of(
            "bindings.kafka", true,
            "bindings.rabbitmq", true,
            "bindings.aws.sqs", true,
            "bindings.aws.kinesis", true,
            "bindings.gcp.pubsub", true,
            "bindings.azure.eventgrid", true,
            "bindings.azure.eventhubs", true,
            "bindings.azure.servicebusqueues", true,
            "bindings.azure.storagequeues", true
    );

    public static final String OpenFuncBinding = "bindings";
    public static final String OpenFuncTopic = "pubsub";

    private RuntimeContext runtimeContext;
    private DaprClient daprClient;

    private Map<String, Plugin> prePlugins;
    private Map<String, Plugin> postPlugins;
    private Out out;

    private BindingEvent bindingEvent;
    private TopicEvent topicEvent;
    private CloudEvent cloudEvent;

    private HttpRequest httpRequest;
    private HttpResponse httpResponse;

    public UserContext(RuntimeContext runtimeContext, DaprClient daprClient, BindingEvent event) {
        init(runtimeContext, daprClient);
        this.bindingEvent = event;
    }

    public UserContext(RuntimeContext runtimeContext, DaprClient daprClient, TopicEvent event) {
        init(runtimeContext, daprClient);
        this.topicEvent = event;
    }

    public UserContext(RuntimeContext runtimeContext, DaprClient daprClient, HttpRequest httpRequest, HttpResponse httpResponse) {
        init(runtimeContext, daprClient);
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
    }

    private void init(RuntimeContext runtimeContext, DaprClient daprClient) {
        this.runtimeContext = runtimeContext;

        Map<String, Plugin> plugins = new HashMap<>();
        for (String name : runtimeContext.getPrePlugins().keySet()) {
            plugins.put(name, (runtimeContext.getPrePlugins().get(name).init()));
        }
        prePlugins = plugins;

        plugins = new HashMap<>();
        for (String name : runtimeContext.getPostPlugins().keySet()) {
            plugins.put(name, (runtimeContext.getPostPlugins().get(name).init()));
        }
        postPlugins = plugins;

        this.daprClient = daprClient;
    }

    @Override
    public Error send(String outputName, String data) {

        Map<String, Component> outputs = runtimeContext.getOutputs();
        if (outputs.isEmpty()) {
            return new Error("no output");
        }

        Component output = outputs.get(outputName);
        if (output == null) {
            return new Error("output " + outputName + " not found");
        }

        String payload = data;
        // Convert queue binding event into cloud event format to add tracing metadata in the cloud event context.
        if (isTraceable(output.getComponentType())) {
        }

        if (output.getComponentType().startsWith(OpenFuncTopic)) {
            daprClient.publishEvent(output.getComponentName(), output.getUri(), payload);
        } else if (output.getComponentType().startsWith(OpenFuncBinding)) {
            daprClient.invokeBinding(output.getComponentName(), output.getOperation(), payload.getBytes(), output.getMetadata()).block();
        } else {
            return new Error("unknown output type " + output.getComponentType());
        }

        return null;
    }

    /**
     * isTraceable Convert queue binding event into cloud event format to add tracing metadata in the cloud event context.
     *
     * @param t output type
     * @return Boolean
     */
    private Boolean isTraceable(String t) {

        if (t.startsWith("pubsub")) {
            return true;
        }

        // For dapr binding components, let the mapping conditions of the bindingQueueComponents
        // determine if the tracing metadata can be added.
        return bindingQueueComponents.get(t);
    }

    @Override
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    @Override
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    @Override
    public BindingEvent getBindingEvent() {
        return bindingEvent;
    }

    @Override
    public TopicEvent getTopicEvent() {
        return topicEvent;
    }

    @Override
    public CloudEvent getCloudEvent() {
        return cloudEvent;
    }

    @Override
    public String getName() {
        return runtimeContext.getName();
    }

    @Override
    public String getMode() {
        return runtimeContext.getMode();
    }

    @Override
    public Out getOut() {
        return out;
    }

    @Override
    public String getRuntime() {
        return runtimeContext.getRuntime();
    }

    @Override
    public String getHttpPattern() {
        return null;
    }

    @Override
    public Map<String, Component> getInputs() {
        return runtimeContext.getInputs();
    }

    @Override
    public Map<String, Component> getOutputs() {
        return runtimeContext.getOutputs();
    }

    @Override
    public String getPodName() {
        return runtimeContext.getPod();
    }

    @Override
    public String getPodNamespace() {
        return runtimeContext.getNamespace();
    }

    @Override
    public Map<String, Plugin> getPrePlugins() {
        return prePlugins;
    }

    @Override
    public Map<String, Plugin> getPostPlugins() {
        return postPlugins;
    }

    public void setCloudEvent(CloudEvent cloudEvent) {
        this.cloudEvent = cloudEvent;
    }

    public void setOut(Out out) {
        this.out = out;
    }

    public void executePrePlugins() {
        for (String name : getPrePlugins().keySet()) {
            Plugin plugin = getPrePlugins().get(name);
            Error error = plugin.execPreHook(this);
            if (error != null) {
                logger.log(Level.SEVERE, "execute plugin " + plugin.name() + ":" + plugin.version() + " error", error);
            }
        }
    }

    public void executePostPlugins() {
        for (String name : getPostPlugins().keySet()) {
            Plugin plugin = getPostPlugins().get(name);
            Error error = plugin.execPostHook(this);
            if (error != null) {
                logger.log(Level.SEVERE, "execute plugin " + plugin.name() + ":" + plugin.version() + " error", error);
            }
        }
    }
}
