package org.kie.cekit.cacher.builds.nightly;

import io.quarkus.scheduler.Scheduled;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.BuildUtils;
import org.kie.cekit.cacher.utils.CacherUtils;
import org.kie.cekit.cacher.utils.UrlUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class holds the operations related with rhpam nightly builds
 */
@ApplicationScoped
public class NightlyBuildsWatcher {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherUtils cacherUtils;

    @Inject
    BuildUtils builderUtils;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    NightlyBuildUpdatesInterceptor buildCallback;

    public void verifyNightlyBuild(Optional<String> version, Optional<String> branch, Optional<String> buildDate, boolean force) {
        tryBuildDate(version, branch, buildDate, force);
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
     * @param version   target version
     * @param branch    target branch
     * @param buildDate new build date
     */
    private void tryBuildDate(Optional<String> version, Optional<String> branch, Optional<String> buildDate, boolean force) {
        if (cacherProperties.isWatcherEnabled()) {
            String normalizedVersion = version.orElse(cacherProperties.version());
            String normalizedBranch = branch.orElse(cacherProperties.defaultBranch());
            int rhpamCounter = 0;

            if (buildDate.isPresent()) {

                log.fine(String.format("new manual build tried, params: branch-> %s, version-> %s, buildDate-> %s",
                                       normalizedBranch, normalizedVersion, buildDate.get()));

                //Properties rhpamProp, String buildDate, String version, String branch
                rhpamNightlyBuildDownloader(cacherProperties.productPropertyFile(
                        String.format(cacherProperties.rhpamUrl(), normalizedVersion, buildDate.get())),
                                            buildDate.get(),
                                            normalizedVersion,
                                            normalizedBranch,
                                            force);
            } else {

                while (rhpamCounter < 4) {
                    Properties rhpamProps = cacherProperties.productPropertyFile(
                            String.format(cacherProperties.rhpamUrl(),
                                          cacherProperties.version(),
                                          LocalDate.now().minusDays(rhpamCounter).format(
                                                  builderUtils.formatter(cacherProperties.getFormattedVersion()))));

                    if (rhpamProps != null && rhpamProps.size() > 0) {
                        rhpamNightlyBuildDownloader(rhpamProps,
                                                    LocalDate.now().minusDays(rhpamCounter).format(
                                                            builderUtils.formatter(cacherProperties.getFormattedVersion())),
                                                    normalizedVersion,
                                                    normalizedBranch,
                                                    force);
                        log.info("RHPAM - Nightly build found, latest is " +
                                         LocalDate.now().minusDays(rhpamCounter).format
                                                 (builderUtils.formatter(cacherProperties.getFormattedVersion())));
                        break;
                    }
                    rhpamCounter++;
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
        // set the kieVersion
        cacherProperties.setKieVersion(rhpamProp.get("KIE_VERSION").toString());
        cacherProperties.getFiles2DownloadPropName("rhpam").stream().forEach(file -> {
            // make sure there is no rhpam already downloaded files
            if (!cacherUtils.fileExistsByNameExcludeTmp(UrlUtils.getFileName(rhpamProp.get(file).toString()))) {
                // Notify the git consumer that a new file is being downloaded.
                buildCallback.onNewBuildReceived(new PlainArtifact(UrlUtils.getFileName(rhpamProp.get(file).toString()),
                                                                   "",
                                                                   "",
                                                                   buildDate,
                                                                   version,
                                                                   branch,
                                                                   0), force);
                new Thread(() -> log.info(cacherUtils.fetchFile(rhpamProp.get(file).toString(), Optional.of("nightly"), 0))).start();
            }
        });
    }

}
