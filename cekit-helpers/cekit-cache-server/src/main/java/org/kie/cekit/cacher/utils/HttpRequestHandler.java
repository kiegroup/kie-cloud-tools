/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.cekit.cacher.utils;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpRequestHandler {

    public static Response executeHttpCall(String url) throws IOException {
        if (url.startsWith("https")) {
            return getHttpsClient().newCall(getRequest(url)).execute();
        } else {
            return getHttpCleatTextClient().newCall(getRequest(url)).execute();
        }
    }

    private static OkHttpClient getHttpCleatTextClient() {
        return new OkHttpClient.Builder()
                // no https required.
                .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT))
                .build();
    }

    private static OkHttpClient getHttpsClient() {
        return new OkHttpClient.Builder()
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .build();
    }

    private static Request getRequest(String url) {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
