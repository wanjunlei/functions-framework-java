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

package dev.openfunction.functions;

import io.cloudevents.CloudEvent;

import java.util.Map;

/**
 * An interface for event function context.
 */
public interface Context {
    /**
     * send provides the ability to allow the user to send data to a specified output target.
     *
     * @param outputName output target name
     * @param data       Data String
     * @return Error
     */
    Error send(String outputName, String data);

    /**
     * getHttpRequest returns the Http request.
     *
     * @return HttpRequest
     */
    HttpRequest getHttpRequest();

    /**
     * getHttpResponse returns the Http response.
     *
     * @return HttpResponse
     */
    HttpResponse getHttpResponse();

    /**
     * getBindingEvent returns the binding event.
     *
     * @return BindingEvent GetBindingEvent();
     */
    BindingEvent getBindingEvent();

    /**
     * getTopicEvent returns the topic event.
     *
     * @return TopicEvent
     */
    TopicEvent getTopicEvent();

    /**
     * getCloudEvent returns the cloud Event.
     *
     * @return CloudEvent
     */
    CloudEvent getCloudEvent();

    /**
     * getName returns the function's name.
     *
     * @return Function Name
     */
    String getName();

    /**
     * getMode returns the operating environment mode of the function.
     *
     * @return Mode
     */
    String getMode();

    /**
     * GetOut returns the returned value of function.
     *
     * @return Out
     */
    Out getOut();

    /**
     * getRuntime returns the Runtime.
     *
     * @return String
     */
    String getRuntime();

    /**
     * getHttpPattern returns the path of the server listening for http function.
     *
     * @return String
     */
    String getHttpPattern();


    /**
     * getInputs returns the Inputs of function.
     *
     * @return Inputs
     */
    Map<String, Component> getInputs();

    /**
     * getOutputs returns the Outputs of function.
     *
     * @return Outputs
     */
    Map<String, Component> getOutputs();

    /**
     * getPodName returns the name of the pod the function is running on.
     *
     * @return name of pod
     */
    String getPodName();

    /**
     * getPodNamespace returns the namespace of the pod the function is running on.
     *
     * @return namespace of pod
     */
    String getPodNamespace();

    /**
     * @return pre plugins
     */
    Map<String, Plugin> getPrePlugins();

    /**
     * @return post plugin
     */
    Map<String, Plugin> getPostPlugins();
}
