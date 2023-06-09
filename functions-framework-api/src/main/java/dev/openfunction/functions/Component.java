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

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

public class Component {
    private static final String ComponentTypeBinding = "bindings";
    private static final String ComponentTypePubsub = "pubsub";

    @Deprecated
    private String uri;
    private String topic;
    private String componentName;
    private String componentType;
    private Map<String, String> metadata;
    private String operation;

    @Deprecated
    public String getUri() {
        if (!StringUtils.isBlank(uri)) {
            return uri;
        } else if (!StringUtils.isBlank(topic)) {
            return topic;
        } else {
            return componentName;
        }
    }

    @Deprecated
    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTopic() {
        if (StringUtils.isBlank(topic)) {
            return topic;
        } else if (StringUtils.isBlank(uri) && !Objects.equals(uri, componentName)) {
            return uri;
        } else {
            return null;
        }
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isPubsub() {
        return componentType.startsWith(ComponentTypePubsub);
    }
    public boolean isBinding() {
        return componentType.startsWith(ComponentTypeBinding);
    }

}

