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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.kie.cekit.cacher.properties.CacherProperties;

@ApplicationScoped
public class HttpRequestHandler {

    @Inject
    CacherProperties props;

    public static Response executeHttpCall(String url, boolean trustAll) throws IOException {
        if (url.startsWith("https")) {
            if (trustAll) {
                return getUntrustedHttpsClient().newCall(getRequest(url)).execute();
            }
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

    private static OkHttpClient getUntrustedHttpsClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        return client
                .sslSocketFactory(getSslContext().getSocketFactory(), (X509TrustManager) getTrustManager()[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .build();
    }

    private static Request getRequest(String url) {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }

    private static TrustManager[] getTrustManager() {
        return new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
    }

    private static SSLContext getSslContext() {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, getTrustManager(), new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return sc;
    }

    public static void trustAllCertificates() {
        SSLContext sc = getSslContext();
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
}
