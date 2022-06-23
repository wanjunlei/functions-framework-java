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

package dev.openfunction.invoker.runtime;

import dev.openfunction.functions.CloudEventFunction;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.OpenFunction;
import dev.openfunction.functions.Out;
import dev.openfunction.invoker.context.RuntimeContext;
import dev.openfunction.invoker.context.UserContext;
import dev.openfunction.invoker.http.HttpRequestImpl;
import dev.openfunction.invoker.http.HttpResponseImpl;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.http.HttpMessageFactory;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes the user's synchronize method.
 */
public class SynchronizeRuntime extends HttpServlet implements Runtime {
    private static final Logger logger = Logger.getLogger("dev.openfunction..invoker");

    private HttpFunction httpFunction;

    private CloudEventFunction cloudEventFunction;

    private OpenFunction openFunction;

    private RuntimeContext runtimeContext;

    private DaprClient daprClient;

    private SynchronizeRuntime(HttpFunction httpFunction) {
        this.httpFunction = httpFunction;
    }

    private SynchronizeRuntime(CloudEventFunction cloudEventFunction) {
        this.cloudEventFunction = cloudEventFunction;
        EventFormatProvider.getInstance().registerFormat(new JsonEventFormat());
    }

    private SynchronizeRuntime(OpenFunction openFunction) {
        this.openFunction = openFunction;
    }

    /**
     * Makes a {@link SynchronizeRuntime} for the given class.
     *
     * @param functionClass function class
     * @return {@link SynchronizeRuntime}
     */
    public static SynchronizeRuntime forClass(Class<?> functionClass) {

        try {
            if (HttpFunction.class.isAssignableFrom(functionClass)) {
                Class<? extends HttpFunction> httpFunctionClass = functionClass.asSubclass(HttpFunction.class);
                HttpFunction httpFunction = httpFunctionClass.getConstructor().newInstance();
                return new SynchronizeRuntime(httpFunction);
            } else if (CloudEventFunction.class.isAssignableFrom(functionClass)) {
                Class<? extends CloudEventFunction> cloudEventFunctionClass = functionClass.asSubclass(CloudEventFunction.class);
                CloudEventFunction cloudEventFunction = cloudEventFunctionClass.getConstructor().newInstance();
                return new SynchronizeRuntime(cloudEventFunction);
            } else if (OpenFunction.class.isAssignableFrom(functionClass)) {
                Class<? extends OpenFunction> asyncFunctionClass = functionClass.asSubclass(OpenFunction.class);
                OpenFunction openFunction = asyncFunctionClass.getConstructor().newInstance();
                return new SynchronizeRuntime(openFunction);
            }
        } catch (ReflectiveOperationException e) {
            throw new Error("Could not construct an instance of " + functionClass.getName(), e);
        }

        throw new Error("Class " + functionClass.getName() + " does not implement function");
    }

    /**
     * Executes the user's method, can handle all HTTP type methods.
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) {
        HttpRequestImpl reqImpl = new HttpRequestImpl(req);
        HttpResponseImpl respImpl = new HttpResponseImpl(res);
        try {
            UserContext userContext = new UserContext(runtimeContext, daprClient, reqImpl, respImpl);

            if (httpFunction != null) {
                userContext.executePrePlugins();
                httpFunction.service(reqImpl, respImpl);

                if (userContext.getOut() == null) {
                    userContext.setOut(new Out().setCode(respImpl.getStatusCode()));
                }

                userContext.executePostPlugins();
            } else if (cloudEventFunction != null) {
                MessageReader messageReader = HttpMessageFactory.createReaderFromMultimap(reqImpl.getHeaders(), reqImpl.getInputStream().readAllBytes());
                CloudEvent event = messageReader.toEvent();
                userContext.setCloudEvent(event);

                userContext.executePrePlugins();
                Error err = cloudEventFunction.accept(userContext, event);

                if (userContext.getOut() == null) {
                    userContext.setOut(new Out().setError(err));
                }

                userContext.executePostPlugins();
                if (userContext.getOut() == null) {
                    userContext.setOut(new Out().setError(err));
                }
                if (userContext.getOut().getData() == null) {
                    userContext.getOut().setData(ByteBuffer.wrap(("Success".getBytes())));
                }

                if (err == null) {
                    respImpl.setStatusCode(HttpServletResponse.SC_OK);
                } else {
                    respImpl.setStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

                respImpl.getOutputStream().write(userContext.getOut().getData().array());
            } else if (openFunction != null) {
                userContext.executePrePlugins();
                Out out = openFunction.accept(userContext, Arrays.toString(reqImpl.getInputStream().readAllBytes()));
                userContext.setOut(out);
                userContext.executePostPlugins();

                if (userContext.getOut() == null) {
                    userContext.setOut(out);
                }

                if (userContext.getOut().getData() == null) {
                    userContext.getOut().setData(ByteBuffer.wrap(("Success".getBytes())));
                }

                if (out.getError() == null) {
                    respImpl.setStatusCode(HttpServletResponse.SC_OK);
                } else {
                    respImpl.setStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

                respImpl.getOutputStream().write(userContext.getOut().getData().array());
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Failed to execute function", t);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try {
                // We can't use HttpServletResponse.flushBuffer() because we wrap the PrintWriter
                // returned by HttpServletResponse in our own BufferedWriter to match our API.
                // So we have to flush whichever of getWriter() or getOutputStream() works.
                try {
                    respImpl.getOutputStream().flush();
                } catch (IllegalStateException e) {
                    respImpl.getWriter().flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void start(RuntimeContext ctx) throws Exception {

        runtimeContext = ctx;
        // create dapr client when dapr sidecar enabled.
        if (System.getenv("DAPR_GRPC_PORT") != null || System.getenv("DAPR_HTTP_PORT") != null) {
            daprClient = new DaprClientBuilder().build();
            daprClient.waitForSidecar(Runtime.WaitDaprSidecarTimeout);
        }

        Server server = new Server(ctx.getPort());

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        ServletHolder servletHolder = new ServletHolder(this);
        servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(""));
        servletContextHandler.addServlet(servletHolder, "/*");

        server.start();
        server.join();
    }

    @Override
    public void close() {

    }
}
