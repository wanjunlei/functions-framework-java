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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.format.EventDeserializationException;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.format.EventSerializationException;
import io.cloudevents.core.v03.CloudEventV03;
import io.cloudevents.core.v1.CloudEventV1;
import io.cloudevents.rw.CloudEventDataMapper;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class JsonEventFormat implements EventFormat {

    public static final String CONTENT_TYPE = "application/cloudevents+json";

    public final static String ID = "id";
    public final static String SOURCE = "source";
    public final static String SPECVERSION = "specversion";
    public final static String TYPE = "type";
    public final static String TIME = "time";
    public final static String SCHEMAURL = "schemaurl";
    public final static String DATACONTENTTYPE = "datacontenttype";
    public final static String DATASCHEMA = "dataschema";
    public final static String SUBJECT = "subject";
    public final static String DATA = "data";
    public final static String EXTENSIONS = "extensions";
    public final static String TRACEPARENT = "traceparent";
    public final static String TRACEID = "traceid";

    @Override
    public byte[] serialize(@NotNull CloudEvent event) throws EventSerializationException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
        root.set(SPECVERSION, objectMapper.valueToTree(event.getSpecVersion().toString()));
        root.set(ID, objectMapper.valueToTree(event.getId()));
        root.set(TYPE, objectMapper.valueToTree(event.getType()));
        root.set(SOURCE, objectMapper.valueToTree(event.getSource()));
        root.set(SCHEMAURL, objectMapper.valueToTree(event.getDataSchema()));
        root.set(DATACONTENTTYPE, objectMapper.valueToTree(event.getDataContentType()));
        root.set(SUBJECT, objectMapper.valueToTree(event.getSubject()));
        root.set(DATASCHEMA, objectMapper.valueToTree(event.getDataSchema()));

        if (event.getTime() != null) {
            root.set(TIME, objectMapper.valueToTree(event.getTime().format(DateTimeFormatter.ISO_DATE_TIME)));
        }

        if (event.getData() != null) {
            root.set(DATA, objectMapper.valueToTree(new String(event.getData().toBytes())));
        }

        ObjectNode extensions = objectMapper.createObjectNode();
        for (String key : event.getExtensionNames()) {
            root.set(key, objectMapper.valueToTree(event.getExtension(key)));
            extensions.set(key, objectMapper.valueToTree(event.getExtension(key)));
        }
        root.set(EXTENSIONS, extensions);

        if (root.get(TRACEPARENT) != null) {
            String traceparent = root.get(TRACEPARENT).asText();
            if (!Objects.equals(traceparent, "")) {
                root.set(TRACEID, objectMapper.valueToTree(traceparent));
            }
        }

        try {
            return objectMapper.writeValueAsBytes(root);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException(e);
        }
    }

    @Override
    public CloudEvent deserialize(@NotNull byte[] bytes, @NotNull CloudEventDataMapper<? extends CloudEventData> mapper) throws EventDeserializationException {
        try {
            String specversion = null;
            String id = null;
            URI source = null;
            String type = null;
            String datacontenttype = null;
            URI schemaurl = null;
            URI dataschema = null;
            String subject = null;
            OffsetDateTime time = null;
            BytesCloudEventData data = null;
            Map<String, Object> extensions = new HashMap<>();

            JsonNode root = new ObjectMapper().readTree(bytes);
            Iterator<String> fields = root.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode node = root.get(field);
                if (node.isNull()) {
                    continue;
                }

                switch (field) {
                    case SPECVERSION:
                        specversion = node.asText();
                        break;
                    case ID:
                        id = node.asText();
                        break;
                    case SOURCE:
                        source = new URI(node.asText());
                        break;
                    case TYPE:
                        type = node.asText();
                        break;
                    case DATACONTENTTYPE:
                        datacontenttype = node.asText();
                        break;
                    case SCHEMAURL:
                        schemaurl = new URI(node.asText());
                        break;
                    case SUBJECT:
                        subject = node.asText();
                        break;
                    case TIME:
                        time = OffsetDateTime.parse(node.asText());
                        break;
                    case DATASCHEMA:
                        dataschema = new URI(node.asText());
                        break;
                    case DATA:
                        data = BytesCloudEventData.wrap(node.asText().getBytes());
                        break;
                    case EXTENSIONS:
                        Iterator<String> it = node.fieldNames();
                        while ( it.hasNext() ) {
                            String name = it.next();
                            extensions.put(name, node.get(name));
                        }
                        break;
                    default:
                        extensions.put(field, node);
                        break;
                }
            }

            if (Objects.equals(specversion, SpecVersion.V1.toString())) {
                return new CloudEventV1(id, source, type, datacontenttype, dataschema, subject,time, data, extensions);
            } else {
                return new CloudEventV03(id, source, type, time, schemaurl, datacontenttype, subject, data, extensions);
            }
        } catch (Exception e) {
            throw new EventDeserializationException(e);
        }
    }

    @Override
    public String serializedContentType() {
        return CONTENT_TYPE;
    }
}
