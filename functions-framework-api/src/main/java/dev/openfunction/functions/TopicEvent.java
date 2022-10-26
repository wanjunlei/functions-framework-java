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

import java.nio.ByteBuffer;

public class TopicEvent {
    /**
     * The name of the pubsub the publisher sent to.
     */
    private final String name;

    /**
     * ID identifies the event.
     */
    private final String id;

    /**
     * The version of the CloudEvents specification.
     */
    private final String specversion;

    /**
     * The type of event related to the originating occurrence.
     */
    private final String type;

    /**
     * Source identifies the context in which an event happened.
     */
    private final String source;

    /**
     *
     */
    private final String datacontenttype;

    /**
     * The content of the event.
     * Note, this is why the gRPC and HTTP implementations need separate structs for cloud events.
     */
    private final ByteBuffer data;

    /**
     * The pubsub topic which publisher sent to.
     */
    private final String topic;

    public TopicEvent(String name, String id, String topic, String specversion, String source, String type, String datacontenttype, ByteBuffer data) {
        this.name = name;
        this.id = id;
        this.topic = topic;
        this.specversion = specversion;
        this.source = source;
        this.type = type;
        this.datacontenttype = datacontenttype;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getSpecversion() {
        return specversion;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getDatacontenttype() {
        return datacontenttype;
    }

    public ByteBuffer getData() {
        return data;
    }

    public String getTopic() {
        return topic;
    }
}
