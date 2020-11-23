package org.kie.cekit.cacher.notification.gchat;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.kie.cekit.cacher.notification.Notification;

import javax.enterprise.context.ApplicationScoped;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class GChatNotifier implements Notification {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    public void send(String message, String webhook) {

        String jsonPayload = "{\"text\" : \"" + message + "\" }";
        log.fine("Trying to notify " + webhook + " with payload " + jsonPayload);

        RequestBody body = RequestBody.create(JSON, jsonPayload);

        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        HttpUrl.Builder httpBuilder = HttpUrl.parse(webhook).newBuilder();
        Request request = new Request.Builder()
                .addHeader("content-type", "application/json; charset=UTF-8")
                .post(body)
                .url(httpBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                log.fine("Google chat notification sent.");
            } else {
                log.fine("Failed to send notification " + response.body().string());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
