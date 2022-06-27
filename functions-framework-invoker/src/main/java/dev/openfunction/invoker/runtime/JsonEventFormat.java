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
import com.google.gson.JsonObject;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.format.EventDeserializationException;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.core.v1.CloudEventV1;
import io.cloudevents.rw.CloudEventDataMapper;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class JsonEventFormat implements EventFormat {

    private static final String CONTENT_TYPE = "application/cloudevents+json";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public byte[] serialize(@NotNull CloudEvent event) throws EventSerializationException {
        return GSON.toJson(event).getBytes();
    }

    @Override
    public CloudEvent deserialize(@NotNull byte[] bytes, @NotNull CloudEventDataMapper<? extends CloudEventData> mapper) throws EventDeserializationException {

        try {
            JsonObject jsonObject = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);

            String id = null;
            URI source = null;
            String type = null;
            String datacontenttype = null;
            URI dataschema = null;
            String subject = null;
            OffsetDateTime time = null;
            BytesCloudEventData data = null;
            Map<String, Object> extensions = new HashMap<>();
            for (String key : jsonObject.keySet()) {
                switch (key) {
                    case CloudEventV1.ID:
                        id = jsonObject.get(key).getAsString();
                        break;
                    case CloudEventV1.SOURCE:
                        source = new URI(jsonObject.get(key).getAsString());
                        break;
                    case CloudEventV1.TYPE:
                        type = jsonObject.get(key).getAsString();
                        break;
                    case CloudEventV1.DATACONTENTTYPE:
                        datacontenttype = jsonObject.get(key).getAsString();
                        break;
                    case CloudEventV1.DATASCHEMA:
                        dataschema = new URI(jsonObject.get(key).getAsString());
                        break;
                    case CloudEventV1.SUBJECT:
                        subject = jsonObject.get(key).getAsString();
                        break;
                    case CloudEventV1.TIME:
                        time = OffsetDateTime.parse(jsonObject.get(key).getAsString());
                        break;
                    case "data":
                        data = BytesCloudEventData.wrap(jsonObject.get(key).getAsString().getBytes());
                        break;
                    default:
                        extensions.put(key, jsonObject.get(key).getAsString());
                        break;
                }
            }

            return new CloudEventV1(id, source, type, datacontenttype, dataschema, subject, time, data, extensions);
        } catch (Exception e) {
            throw new EventDeserializationException(e);
        }
    }

    @Override
    public String serializedContentType() {
        return CONTENT_TYPE;
    }
}
