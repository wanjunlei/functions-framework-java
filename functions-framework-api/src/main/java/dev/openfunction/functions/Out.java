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

public class Out {

    /**
     * code is return code of the user function
     */
    private int code;

    /**
     * error is the error returned by the user function.
     */
    private Error error;

    /**
     * data is the return data of the user function
     */
    private ByteBuffer data;
    /**
     * metadata is the metadata of the event
     */
    private Map<String, String> metadata;

    public Out() {
    }

    public Out(int code, Error error, ByteBuffer data, Map<String, String> metadata) {
        this.code = code;
        this.error = error;
        this.data = data;
        this.metadata = metadata;
    }

    public int getCode() {
        return code;
    }

    public Out setCode(int code) {
        this.code = code;
        return this;
    }

    public ByteBuffer getData() {
        return data;
    }

    public Out setData(ByteBuffer data) {
        this.data = data;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Out setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Error getError() {
        return error;
    }

    public Out setError(Error error) {
        this.error = error;
        return this;
    }
}
