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

public interface Plugin {

    /**
     * name return the name of this plugin.
     *
     * @return Plugin name
     */
    String name();

    /**
     * version return the version of this plugin.
     *
     * @return Plugin name
     */
    String version();

    /**
     * init will create a new plugin, and execute hook in this calling.
     * If you do not want to use a new plugin to execute hook, just return `nil`.
     *
     * @return Plugin
     */
    Plugin init();


    /**
     * execPreHook executes a hook before the function called.
     *
     * @param ctx     Runtime context
     * @return error
     */
    Error execPreHook(Context ctx);

    /**
     * execPreHook executes a hook after the function called.
     *
     * @param ctx     Runtime context
     * @return error
     */
    Error execPostHook(Context ctx);

    /**
     * get return the value of the fieldName`
     *
     * @param fieldName Name of member
     * @return Object
     */
    Object getField(String fieldName);
}
