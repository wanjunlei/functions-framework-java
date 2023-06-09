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

package dev.openfunction.invoker.tracing;

import dev.openfunction.functions.*;
import dev.openfunction.invoker.Callback;
import dev.openfunction.invoker.context.UserContext;
import io.cloudevents.CloudEvent;

import java.util.Map;

public class SkywalkingProvider implements TracingProvider {
    /**
     * <a href="https://github.com/apache/skywalking/blob/master/oap-server/server-starter/src/main/resources/component-libraries.yml#L515">...</a>
     */
    private static final int componentIDOpenFunction = 5013;

//    private final Map<String, String> tags;
//    private final Map<String, String> baggage;

    public SkywalkingProvider() {

    }

    @Override
    public void executeWithTracing(HttpRequest httpRequest, Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(CloudEvent event, Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(BindingEvent event, Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(TopicEvent event, Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(Plugin plugin, Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(Hook hook, Callback callback) throws Exception {

    }

    @Override
    public void executeWithTracing(UserContext ctx, Callback callback) throws Exception {

    }
}
