package org.kie.cekit.cacher.builds.github;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.kie.cekit.cacher.notification.Notification;
import org.kie.cekit.cacher.properties.CacherProperties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class PullRequestSender {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    // rhdm|rhpam-7-image git repo, org is static
    private final String GIT_HUB_API_ENDPOINT = "https://api.github.com/repos/jboss-container-images/%s/pulls";
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    // string format: PR title, PR description, target branch on target repository, PR requester username, PR requester branch
    private final String JSON_PAYLOAD = "{\"title\":\"%s\", \"body\":\"%s\", \"base\":\"%s\", \"head\":\"%s:%s\"}";

    @Inject
    CacherProperties cacherProperties;

    @Inject
    Notification notification;

    /**
     * Perform a Pull Request using the
     *
     * @param repo
     * @param targetBranch
     * @param prTittle
     * @param prDescription
     */
    public boolean performPullRequest(String repo, String baseBranch, String targetBranch, String prTittle, String prDescription) {

        return sendPR(String.format(GIT_HUB_API_ENDPOINT, repo),
                String.format(JSON_PAYLOAD, prTittle, prDescription, baseBranch, cacherProperties.githubUsername(), targetBranch),
                repo);
    }

    /**
     * perform the Pull request via Git Hub Rest API and notifies
     *
     * @param url
     * @param requestBodyAsText
     * @return true for PR created or false
     */
    private boolean sendPR(String url, String requestBodyAsText, String repo) {
        log.info("Trying to perform pull request on " + url + " and with payload: " + requestBodyAsText);
        RequestBody body = RequestBody.create(JSON, requestBodyAsText);

        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("Authorization", "token " + cacherProperties.oauthToken())
                .post(body)
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            JsonReader reader = Json.createReader(new StringReader(response.body().string()));
            JsonObject object = reader.readObject();

            StringBuilder message = new StringBuilder();

            if (response.code() == 201) {

                message.append("PR against ").append(notification.bold(repo));
                message.append(" repository successfully created. Payload is: \n");
                message.append(notification.monospaceBlock(requestBodyAsText.replace("\"", "")));
                message.append("\n");
                message.append(notification.link(object.getString("html_url"), "Click here to access the Pull Request"));
                message.append("\n");
                message.append("Reviewers please take a look: " + notification.mention(cacherProperties.githubReviewers()));

                log.fine(message.toString());
                notification.send(message.toString(), cacherProperties.gChatWebhook());
                return true;
            } else {
                message.append("Failed to created PR against ");
                message.append(repo).append(" repository: ");
                message.append(notification.monospaceBlock(object.getString("message") +
                        ". Reason: " + object.getJsonArray("errors").getJsonObject(0).getString("message")));

                notification.send(message.toString(), cacherProperties.gChatWebhook());

                return false;
            }

        } catch (final Exception e) {
            notification.send("Failed to submit PR against " + repo + " - reason: " + notification.bold(e.getMessage()),
                    cacherProperties.gChatWebhook());
            e.printStackTrace();
            return false;
        }

    }

}
