package org.kie.cekit.cacher.builds.nightly;

import io.quarkus.scheduler.Scheduled;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.kie.cekit.cacher.builds.github.BuildDateUpdatesInterceptor;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.CacherUtils;
import org.kie.cekit.cacher.utils.UrlUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class holds the operations related with rhpam/rhdm nightly builds
 */
@ApplicationScoped
public class NightlyBuildsWatcher {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherUtils cacherUtils;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    BuildDateUpdatesInterceptor buildCallback;

    public void verifyNightlyBuild(Optional<String> version, Optional<String> branch, Optional<String> buildDate) {
        tryBuildDate(version, branch, buildDate, false);
    }

    /**
     * Nightly builds watcher
     * do not run on start up, runs every 12hours
     * or a specific buildDate can be specified using the /rest/buildDate endpoint
     */
    @Scheduled(every = "12h", delay = 12, delayUnit = TimeUnit.HOURS)
    public void nightlyProductBuildsWatcherScheduler() {
        tryBuildDate(Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    /**
     * Nightly builds watcher
     * can be forced by calling /watcher/retry rest endpoint
     * or a specific buildDate can be specified using the /rest/buildDate endpoint
     */
    public void nightlyProductBuildsWatcher(boolean force) {
        tryBuildDate(Optional.empty(), Optional.empty(), Optional.empty(), force);
    }

    /**
     * This method will take care of verifying new builds. User can force a new try with:
     * - REST /watcher/retry will see if there is a new build, will look for the latest 4 days, if none found, give up
     * - REST /watcher/{buildDate} if there is a build in the given buildDate, the download will start.
     * <p>
     * If a build is added and the downloads starts, the PullRequestAcceptor wil be notified for every file being
     * downloaded and then will compare the latest buildDate from upstream, if upstream is recent, then the new files
     * will be only downloaded.
     * Otherwise, if the buildDate is older than the new files, the Pull Request process will start as soon all
     * needed files are persisted on the filesystem.
     *
     * @param version
     * @param branch
     * @param buildDate
     */
    private void tryBuildDate(Optional<String> version, Optional<String> branch, Optional<String> buildDate, boolean force) {
        if (cacherProperties.isWatcherEnabled()) {
            String normalizedVersion = version.orElse(cacherProperties.version());
            String normalizedBranch = branch.orElse(cacherProperties.defaultBranch());
            int rhpamCounter = 0;
            int rhdmCounter = 0;


            if (buildDate.isPresent()) {

                log.fine(String.format("new manual build tried, params: branch-> %s, version-> %s, buildDate-> %s",
                        normalizedBranch, normalizedVersion, buildDate.get()));

                //Properties rhpamProp, String buildDate, String version, String branch
                rhpamNightlyBuildDownloader(productPropertyFile(
                        String.format(cacherProperties.rhpamUrl(), normalizedVersion, buildDate.get())),
                        buildDate.get(),
                        normalizedVersion,
                        normalizedBranch,
                        force);
                rhdmNightlyBuildDownloader(productPropertyFile(
                        String.format(cacherProperties.rhdmUrl(), normalizedVersion, buildDate.get())),
                        buildDate.get(),
                        normalizedVersion,
                        normalizedBranch,
                        force);

            } else {

                while (rhpamCounter < 4) {
                    Properties rhpamProps = productPropertyFile(String.format(cacherProperties.rhpamUrl(),
                            cacherProperties.version(), LocalDate.now().minusDays(rhpamCounter).format(cacherProperties.formatter)));
                    if (rhpamProps != null && rhpamProps.size() > 0) {
                        rhpamNightlyBuildDownloader(rhpamProps,
                                LocalDate.now().minusDays(rhpamCounter).format(cacherProperties.formatter),
                                normalizedVersion,
                                normalizedBranch,
                                force);
                        log.info("RHPAM - Nightly build found, latest is " + LocalDate.now().minusDays(rhpamCounter).format(cacherProperties.formatter));
                        break;
                    }
                    rhpamCounter++;
                }

                while (rhdmCounter < 4) {
                    Properties rhdmProps = productPropertyFile(String.format(cacherProperties.rhdmUrl(),
                            cacherProperties.version(), LocalDate.now().minusDays(rhdmCounter).format(cacherProperties.formatter)));

                    if (rhdmProps != null && rhdmProps.size() > 0) {
                        rhdmNightlyBuildDownloader(rhdmProps,
                                LocalDate.now().minusDays(rhdmCounter).format(cacherProperties.formatter),
                                normalizedVersion,
                                normalizedBranch,
                                force);
                        log.info("RHDM - Nightly build found, latest is " + LocalDate.now().minusDays(rhdmCounter).format(cacherProperties.formatter));
                        break;
                    }
                    rhdmCounter++;
                }
            }

        } else {
            log.info("Watcher disabled.");
        }
    }

    /**
     * Downloads the rhpam files
     *
     * @param rhpamProp
     * @param buildDate
     * @param version
     * @param branch
     */
    private void rhpamNightlyBuildDownloader(Properties rhpamProp, String buildDate, String version, String branch, boolean force) {
        cacherProperties.getRhpamFiles2DownloadPropName().stream().forEach(file -> {
            // make sure there is no rhpam already downloaded files
            if (!cacherUtils.fileExistsByNameExcludeTmp(UrlUtils.getFileName(rhpamProp.get(file).toString()))) {
                // Notify the git consumer that a new file is being downloaded.
                buildCallback.onNewBuildReceived(new PlainArtifact(UrlUtils.getFileName(rhpamProp.get(file).toString()),
                        "",
                        "",
                        buildDate,
                        version,
                        branch), force);
                new Thread(() -> {
                    log.info(cacherUtils.fetchFile(rhpamProp.get(file).toString()));
                }).start();
            }
        });
    }

    /**
     * Downloads rhdm files
     *
     * @param rhdmProp
     * @param buildDate
     * @param version
     * @param branch
     */
    private void rhdmNightlyBuildDownloader(Properties rhdmProp, String buildDate, String version, String branch, boolean force) {
        cacherProperties.getRhdmFiles2DownloadPropName().stream().forEach(file -> {
            // make sure there is no rhdm already downloaded files
            if (!cacherUtils.fileExistsByNameExcludeTmp(UrlUtils.getFileName(rhdmProp.get(file).toString()))) {
                // Notify the git consumer that a new file is being downloaded.
                buildCallback.onNewBuildReceived(new PlainArtifact(UrlUtils.getFileName(rhdmProp.get(file).toString()),
                        "",
                        "",
                        buildDate,
                        version,
                        branch), force);
                new Thread(() -> {
                    log.info(cacherUtils.fetchFile(rhdmProp.get(file).toString()));
                }).start();
            }
        });
    }

    /**
     * fetch the RHDM/RHPAM build properties file.
     *
     * @param url
     * @return
     */
    private Properties productPropertyFile(String url) {
        log.info("Trying to get the properties file from " + url);
        Properties p = new Properties();
        OkHttpClient ok = new OkHttpClient.Builder()
                // no https required.
                .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = ok.newCall(request).execute()) {
            if (response.code() == 404) {
                log.info("Nightly build not found... url -> " + url);
                return p;
            }
            try (final InputStream stream = Objects.requireNonNull(response.body()).byteStream()) {
                p.load(stream);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }

        return p;
    }
}
