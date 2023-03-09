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

package dev.openfunction.invoker.plugins;

import dev.openfunction.functions.Context;
import dev.openfunction.functions.Plugin;

import java.util.Map;

public class SkywalkingPlugin implements Plugin {

    private static final String Version = "v1";

    /**
     * <a href="https://github.com/apache/skywalking/blob/master/oap-server/server-starter/src/main/resources/component-libraries.yml#L515">...</a>
     */
    private static final int componentIDOpenFunction = 5013;

    private final String name;
    private final Map<String, String> tags;
    private final Map<String, String> baggage;

    public SkywalkingPlugin(String name, Map<String, String> tags, Map<String, String> baggage) {
        this.name = name;
        this.tags = tags;
        this.baggage = baggage;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return Version;
    }

    @Override
    public Plugin init() {
        return this;
    }

    @Override
    public Error execPreHook(Context ctx) {
        return null;
    }

    @Override
    public Error execPostHook(Context ctx) {
        return null;
    }

    @Override
    public Object getField(String fieldName) {
        return null;
    }
}
