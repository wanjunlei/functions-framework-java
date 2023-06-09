package dev.openfunction.invoker.tracing;

import dev.openfunction.functions.*;
import dev.openfunction.invoker.Callback;
import dev.openfunction.invoker.context.UserContext;
import io.cloudevents.CloudEvent;

import java.util.Map;

public interface TracingProvider {
    void executeWithTracing(HttpRequest httpRequest, Callback callback) throws Exception;

    void executeWithTracing(CloudEvent event, Callback callback) throws Exception;

    void executeWithTracing(TopicEvent event, Callback callback) throws Exception;

    void executeWithTracing(BindingEvent event, Callback callback) throws Exception;

    void executeWithTracing(Callback callback)throws Exception;

    void executeWithTracing(Plugin plugin, Callback callback)throws Exception;

    void executeWithTracing(Hook hook, Callback callback)throws Exception;

    void executeWithTracing(UserContext ctx, Callback callback)throws Exception;
}
