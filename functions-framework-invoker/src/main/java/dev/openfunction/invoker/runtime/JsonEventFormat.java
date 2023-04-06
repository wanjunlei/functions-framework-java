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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.format.EventDeserializationException;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.core.v03.CloudEventV03;
import io.cloudevents.core.v1.CloudEventV1;
import io.cloudevents.rw.CloudEventDataMapper;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JsonEventFormat implements EventFormat {

    public static final String CONTENT_TYPE = "application/cloudevents+json";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public byte[] serialize(@NotNull CloudEvent event) throws EventSerializationException {
        JsonObject root = GSON.toJsonTree(event).getAsJsonObject();
        for (String key : event.getExtensionNames()) {
            root.add(key, GSON.toJsonTree(event.getExtension(key)));
        }
        String traceparent = root.get("traceparent").getAsString();
        if (!Objects.equals(traceparent, "")) {
            root.add("traceid", GSON.toJsonTree(traceparent));
        }
        return root.toString().getBytes();
    }

    @Override
    public CloudEvent deserialize(@NotNull byte[] bytes, @NotNull CloudEventDataMapper<? extends CloudEventData> mapper) throws EventDeserializationException {

        try {
            JsonObject jsonObject = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);

            String id = null;
            URI source = null;
            String type = null;
            String datacontenttype = null;
            URI schemaurl = null;
            String subject = null;
            OffsetDateTime time = null;
            BytesCloudEventData data = null;
            Map<String, Object> extensions = new HashMap<>();
            for (String key : jsonObject.keySet()) {
                JsonElement element = jsonObject.get(key);
                if (element.isJsonNull()) {
                    continue;
                }

                switch (key) {
                    case CloudEventV03.ID:
                        id = element.getAsString();
                        break;
                    case CloudEventV03.SOURCE:
                        source = new URI(element.getAsString());
                        break;
                    case CloudEventV03.TYPE:
                        type = element.getAsString();
                        break;
                    case CloudEventV03.DATACONTENTTYPE:
                        datacontenttype = element.getAsString();
                        break;
                    case CloudEventV03.SCHEMAURL:
                        schemaurl = new URI(element.getAsString());
                        break;
                    case CloudEventV1.SUBJECT:
                        subject = element.getAsString();
                        break;
                    case CloudEventV1.TIME:
                        time = OffsetDateTime.parse(element.getAsString());
                        break;
                    case "data":
                        data = BytesCloudEventData.wrap(element.getAsString().getBytes());
                        break;
                    case "extensions":
                        JsonObject extensionsObject= element.getAsJsonObject();
                        for (String extensionKey : extensionsObject.keySet()) {
                            extensions.put(extensionKey, extensionsObject.get(extensionKey).getAsString());
                        }
                        break;
                    default:
                        extensions.put(key, element.getAsString());
                        break;
                }
            }

            return new CloudEventV03(id, source, type, time, schemaurl, datacontenttype, subject, data, extensions);
        } catch (Exception e) {
            throw new EventDeserializationException(e);
        }
    }

    @Override
    public String serializedContentType() {
        return CONTENT_TYPE;
    }
}
