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

package dev.openfunction.invoker.trigger;

import com.google.protobuf.Value;
import dev.openfunction.functions.BindingEvent;
import dev.openfunction.functions.Component;
import dev.openfunction.functions.OpenFunction;
import dev.openfunction.functions.TopicEvent;
import dev.openfunction.invoker.context.RuntimeContext;
import dev.openfunction.invoker.context.UserContext;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.DaprAppCallbackProtos;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.collections.MapUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes the user's asynchronous function.
 */
public final class DaprTrigger implements Trigger {
    private static final Logger logger = Logger.getLogger("dev.openfunction.invoker");

    private final RuntimeContext runtimeContext;

    private final ArrayList<OpenFunction> functions;

    private final Service service;

    public DaprTrigger(RuntimeContext runtimeContext, Class<?>[] functionClasses) {
        this.runtimeContext = runtimeContext;

        functions = new ArrayList<>();
        for (Class<?> c : functionClasses) {
            if (!OpenFunction.class.isAssignableFrom(c)) {
                throw new Error("Unsupported function " + c.getName());
            }

            try {
                Class<? extends OpenFunction> openFunctionClass = c.asSubclass(OpenFunction.class);
                functions.add(openFunctionClass.getConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new Error("Could not construct an instance of " + c.getName(), e);
            }
        }

        service = new Service();
    }

    @Override
    public void start() throws Exception {
        if (MapUtils.isEmpty(runtimeContext.getDaprTrigger())) {
            throw new Error("no dapr trigger defined for the function");
        }

        this.service.start(runtimeContext.getPort());
    }

    @Override
    public void close() {

    }

    private class Service extends AppCallbackGrpc.AppCallbackImplBase {

        private Server daprServer;
        private DaprClient daprClient;

        public void start(int port) throws Exception {
            daprServer = ServerBuilder
                    .forPort(port)
                    .addService(Service.this)
                    .build()
                    .start();

            daprClient = new DaprClientBuilder().build();
            daprClient.waitForSidecar(WaitDaprSidecarTimeout);

            // Now we handle ctrl+c (or any other JVM shutdown)
            java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        daprClient.shutdown();
                        daprServer.shutdown();
                    })
            );

            daprServer.awaitTermination();
        }

        @Override
        public void listInputBindings(com.google.protobuf.Empty request,
                                      io.grpc.stub.StreamObserver<io.dapr.v1.DaprAppCallbackProtos.ListInputBindingsResponse> responseObserver) {

            List<String> bindings = new ArrayList<>();
            for (String key : runtimeContext.getDaprTrigger().keySet()) {
                Component component = runtimeContext.getDaprTrigger().get(key);
                if (component.isBinding()) {
                    bindings.add(component.getComponentName());
                }
            }

            responseObserver.onNext(DaprAppCallbackProtos.ListInputBindingsResponse.newBuilder().addAllBindings(bindings).build());
            responseObserver.onCompleted();
        }

        @Override
        public void onBindingEvent(DaprAppCallbackProtos.BindingEventRequest request,
                                   StreamObserver<DaprAppCallbackProtos.BindingEventResponse> responseObserver) {
            BindingEvent event = new BindingEvent(request.getName(), request.getMetadataMap(), request.getData().asReadOnlyByteBuffer());

            try {
                runtimeContext.executeWithTracing(event, () -> {
                            for (OpenFunction function : functions) {
                                new UserContext(runtimeContext, daprClient).
                                        withBindingEvent(event).
                                        executeFunction(function, request.getData().toStringUtf8());
                            }
                            responseObserver.onNext(DaprAppCallbackProtos.BindingEventResponse.getDefaultInstance());
                            responseObserver.onCompleted();
                            return null;
                        }
                );
            } catch (Exception e) {
                logger.log(Level.INFO, "catch exception when execute function " + runtimeContext.getName());
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }

        @Override
        public void listTopicSubscriptions(com.google.protobuf.Empty request,
                                           io.grpc.stub.StreamObserver<io.dapr.v1.DaprAppCallbackProtos.ListTopicSubscriptionsResponse> responseObserver) {
            List<DaprAppCallbackProtos.TopicSubscription> subscriptions = new ArrayList<>();
            for (String key : runtimeContext.getDaprTrigger().keySet()) {
                Component component = runtimeContext.getDaprTrigger().get(key);
                if (component.isPubsub()) {
                    subscriptions.add(DaprAppCallbackProtos.TopicSubscription.newBuilder().setTopic(component.getTopic()).setPubsubName(component.getComponentName()).build());
                }
            }
            responseObserver.onNext(DaprAppCallbackProtos.ListTopicSubscriptionsResponse.newBuilder().addAllSubscriptions(subscriptions).build());
            responseObserver.onCompleted();
        }

        @Override
        public void onTopicEvent(DaprAppCallbackProtos.TopicEventRequest request,
                                 io.grpc.stub.StreamObserver<io.dapr.v1.DaprAppCallbackProtos.TopicEventResponse> responseObserver) {
            TopicEvent event = new TopicEvent(request.getPubsubName(),
                    request.getId(),
                    request.getTopic(),
                    request.getSpecVersion(),
                    request.getSource(),
                    request.getType(),
                    request.getDataContentType(),
                    request.getData().asReadOnlyByteBuffer(),
                    getExtensions(request));

            try {
                runtimeContext.executeWithTracing(event, () -> {
                            for (OpenFunction function : functions) {
                                new UserContext(runtimeContext, daprClient).
                                        withTopicEvent(event).
                                        executeFunction(function, request.getData().toStringUtf8());
                            }
                            responseObserver.onNext(DaprAppCallbackProtos.TopicEventResponse.getDefaultInstance());
                            responseObserver.onCompleted();
                            return null;
                        }
                );
            } catch (Exception e) {
                logger.log(Level.INFO, "catch exception when execute function " + runtimeContext.getName());
                e.printStackTrace();
                responseObserver.onError(e);
            }
        }
    }

    private Map<String, String> getExtensions(DaprAppCallbackProtos.TopicEventRequest request) {
        Map<String, String> extensions = new HashMap<>();
        Map<String, Value> fields = request.getExtensions().getFieldsMap();
        for (String key : fields.keySet()) {
            Value value = fields.get(key);
            if (value.hasStringValue()) {
                extensions.put(key, value.getStringValue());
            }
        }

        return extensions;
    }
}
