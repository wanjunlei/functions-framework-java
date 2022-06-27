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
import java.util.Map;

public class BindingEvent {

    /**
     * The name of the input binding component.
     */
    private final String name;

    private final ByteBuffer data;

    private final Map<String, String> metadata;

    public BindingEvent(String name, Map<String, String> metadata, ByteBuffer data) {
        this.name = name;
        this.metadata = metadata;
        this.data = data;
    }

    public ByteBuffer getData() {
        return data;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }
}
