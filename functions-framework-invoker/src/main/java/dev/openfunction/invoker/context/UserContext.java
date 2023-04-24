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
import dev.openfunction.invoker.Callback;
import dev.openfunction.invoker.runtime.JsonEventFormat;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.v03.CloudEventBuilder;
import io.dapr.client.DaprClient;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserContext implements Context {

    private static final Logger logger = Logger.getLogger("dev.openfunction.invoker");

    public static final String OpenFuncBinding = "bindings";
    public static final String OpenFuncTopic = "pubsub";

    private static final Set<String> MiddlewaresCloudEventFormatReqired = Set.of(
            "kafka",
            "kubemq",
            "mqtt3",
            "rabbitmq",
            "redis",
            "gcp.pubsub",
            "azure.eventhubs"
    );

    private final RuntimeContext runtimeContext;
    private final DaprClient daprClient;

    private Out out;

    private BindingEvent bindingEvent;
    private TopicEvent topicEvent;
    private CloudEvent cloudEvent;

    private HttpRequest httpRequest;
    private HttpResponse httpResponse;

    private Object function;

    public UserContext(RuntimeContext runtimeContext, DaprClient daprClient) {
        this.runtimeContext = runtimeContext;
        this.daprClient = daprClient;
    }

    public UserContext withHttp(HttpRequest httpRequest, HttpResponse httpResponse) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        return this;
    }

    public UserContext withBindingEvent(BindingEvent event) {
        this.bindingEvent = event;
        return this;
    }

    public UserContext withTopicEvent(TopicEvent event) {
        this.topicEvent = event;
        return this;
    }

    @Override
    public Error send(String outputName, String data) {
        if (data == null) {
            return null;
        }
        Map<String, Component> outputs = runtimeContext.getOutputs();
        if (outputs.isEmpty()) {
            return new Error("no output");
        }

        Component output = outputs.get(outputName);
        if (output == null) {
            return new Error("output " + outputName + " not found");
        }

        if (output.getComponentType().startsWith(OpenFuncTopic)) {
            daprClient.publishEvent(output.getComponentName(), output.getUri(), data);
        } else if (output.getComponentType().startsWith(OpenFuncBinding)) {
            // If a middleware supports both binding and pubsub, then the data send to
            // binding must be in CloudEvent format, otherwise pubsub cannot parse the data.
            byte[] payload = data.getBytes();
            if (MiddlewaresCloudEventFormatReqired.contains(output.getComponentType().substring(OpenFuncBinding.length() + 1))) {
                CloudEvent event = new CloudEventBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withType("dapr.invoke")
                        .withSource(URI.create("openfunction/invokeBinding"))
                        .withData(data.getBytes())
                        .withDataContentType(JsonEventFormat.CONTENT_TYPE)
                        .withSubject(output.getUri())
                        .build();
                payload = new JsonEventFormat().serialize(event);
            }

            daprClient.invokeBinding(output.getComponentName(), output.getOperation(), payload).block();
        } else {
            return new Error("unsupported output type " + output.getComponentType());
        }

        return null;
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
    public Out getOut() {
        return out;
    }

    @Override
    public String getHttpPattern() {
        return null;
    }

    @Override
    public Map<String, Component> getOutputs() {
        return runtimeContext.getOutputs();
    }

    @Override
    public DaprClient getDaprClient() {
        return daprClient;
    }

    public Map<String, Component> getInputs() {
        return runtimeContext.getInputs();
    }

    public Class<?> getFunctionClass() {
        return function.getClass();
    }

    private void executePrePlugins() throws Exception {
        for (String name : runtimeContext.getPrePlugins().keySet()) {
            Plugin plugin = runtimeContext.getPrePlugins().get(name).init();
            if (plugin.needToTracing()) {
                runtimeContext.executeWithTracing(plugin, () -> {
                    Error error = plugin.execPreHook(UserContext.this);
                    if (error != null) {
                        logger.log(Level.SEVERE, "execute plugin " + plugin.name() + ":" + plugin.version() + " error", error);
                    }

                    return error;
                });
            } else {
                Error error = plugin.execPreHook(UserContext.this);
                if (error != null) {
                    logger.log(Level.SEVERE, "execute plugin " + plugin.name() + ":" + plugin.version() + " error", error);
                }
            }
        }
    }

    private void executePostPlugins() throws Exception {
        for (String name : runtimeContext.getPostPlugins().keySet()) {
            Plugin plugin = runtimeContext.getPostPlugins().get(name).init();
            if (plugin.needToTracing()) {
                runtimeContext.executeWithTracing(plugin, () -> {
                    Error error = plugin.execPostHook(UserContext.this);
                    if (error != null) {
                        logger.log(Level.SEVERE, "execute plugin " + plugin.name() + ":" + plugin.version() + " error", error);
                    }

                    return error;
                });
            } else {
                Error error = plugin.execPostHook(UserContext.this);
                if (error != null) {
                    logger.log(Level.SEVERE, "execute plugin " + plugin.name() + ":" + plugin.version() + " error", error);
                }
            }
        }
    }

    public void executeFunction(HttpFunction function) throws Exception {
        this.function = function;
        executeFunction(() -> {
            function.service(this.httpRequest, this.httpResponse);
            return null;
        });
    }

    public void executeFunction(CloudEventFunction function, CloudEvent event) throws Exception {
        this.function = function;
        this.cloudEvent = event;
        executeFunction(() -> {
            Error err = function.accept(UserContext.this, event);
            if (err == null) {
                httpResponse.setStatusCode(HttpServletResponse.SC_OK);
                httpResponse.getOutputStream().write(out == null || out.getData() == null ? "Success".getBytes() : out.getData().array());
            } else {
                httpResponse.setStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.getOutputStream().write(err.getMessage().getBytes());
            }
            return null;
        });
    }

    public void executeFunction(OpenFunction function, String payload) throws Exception {
        this.function = function;
        executeFunction(() -> {
            out = function.accept(UserContext.this, payload);
            if (httpResponse != null) {
                if (out == null || out.getError() == null) {
                    httpResponse.setStatusCode(HttpServletResponse.SC_OK);
                    httpResponse.getOutputStream().write(out == null || out.getData() == null ? "Success".getBytes() : out.getData().array());
                } else {
                    httpResponse.setStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    httpResponse.getOutputStream().write(out.getError().getMessage().getBytes());
                }
            }

            return out == null ? null : out.getError();
        });
    }

    private void executeFunction(Callback callBack) throws Exception {
        runtimeContext.executeWithTracing(this,
                () -> {
                    executePrePlugins();
                    runtimeContext.executeWithTracing(null, callBack);
                    executePostPlugins();
                    return null;
                });
    }
}
