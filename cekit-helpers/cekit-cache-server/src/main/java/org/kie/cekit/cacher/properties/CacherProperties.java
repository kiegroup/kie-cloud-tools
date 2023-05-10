package org.kie.cekit.cacher.properties;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.Version;
import okhttp3.Response;
import org.kie.cekit.cacher.exception.RequiredParameterMissingException;
import org.kie.cekit.cacher.properties.loader.CacherProperty;
import org.kie.cekit.cacher.utils.BuildUtils;
import org.kie.cekit.cacher.utils.HttpRequestHandler;

/**
 * Holds all cacher's configurations
 */
@ApplicationScoped
public class CacherProperties {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public final Version versionBeforeDMPAMPrefix = new Version(7, 8, 0, null, null, null);
    public final Version pam710 = new Version(7, 10, 0, null, null, null);
    public final Version pam711 = new Version(7, 11, 0, null, null, null);

    @Inject
    BuildUtils buildUtils;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.base.dir", required = true)
    String cacherDataDir;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.username")
    String githubUsername;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.oauth-token")
    String oauthToken;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.email")
    String githubEmail;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.reviewers")
    String githubReviewers;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.rhpam.upstream.project")
    String rhpamUpstream;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.enable.github.bot")
    boolean isGHBotEnabled;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.default.branch")
    String defaultBranch;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.rhpam.url")
    String rhpamUrl;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.rhpam.cr.url")
    String rhpamCRUrl;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.bamoe.cr.url")
    String bamoeCRUrl;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.nightly.maven.repo")
    String nightlyMavenRepo;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.cr.maven.repo")
    String crMavenRepo;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.product.version")
    String version;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.gchat.webhook")
    String gChatWebhook;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.enable.nightly.watcher")
    boolean isWatcherEnabled;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.preload.file")
    String preLoadFileLocation;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.trust.all")
    boolean trustAll;

    /**
     * Value will set during runtime while CR or Nightly build watcher is run, it comes from
     * the build properties file from the property "KIE_VERSION"
     */
    private String kieVersion;

    /**
     * RHPAM properties keys needed to download the nightly builds artifacts
     * These properties came from the product properties file.
     */
    private List<String> rhpamFiles2DownloadPropName = Arrays.asList(
            "rhpam.addons.latest.url",
            "rhpam.business-central-eap7.latest.url",
            "rhpam.monitoring.latest.url",
            "rhpam.kie-server.ee8.latest.url");

    /**
     * @return github Username
     */
    public String githubUsername() {
        return githubUsername;
    }

    /**
     * @return github oauth token
     */
    public String oauthToken() {
        return oauthToken;
    }

    /**
     * @return github user email
     */
    public String githubEmail() {
        return githubEmail;
    }

    /**
     * @return if the github integration is enabled or not
     */
    public boolean isGHBotEnabled() {
        if (isGHBotEnabled) {
            if (null == githubUsername || githubUsername.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.username is required!");
            }
            if (null == oauthToken || oauthToken.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.oauth-token is required!");
            }
            if (null == githubEmail || githubEmail.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.email is required!");
            }
            if (null == githubReviewers || githubReviewers.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.reviewers is required!");
            }
            if (null == rhpamUpstream || rhpamUpstream.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.rhpam.upstream.project is required!");
            }
        }
        return isGHBotEnabled;
    }

    /**
     * @return default branch for rhpam upstream
     */
    public String defaultBranch() {
        return defaultBranch;
    }

    /**
     * @return the gchat user id of the GitHub PR reviewers
     */
    public String[] githubReviewers() {
        return githubReviewers.trim().split(",");
    }

    /**
     * @return rhpam upstream git repository
     */
    public String rhpamUpstream() {
        return rhpamUpstream;
    }

    /**
     * @return rhpam nightly build url information
     */
    public String rhpamUrl() {
        return rhpamUrl;
    }

    /**
     * @return the URL that holds CR build properties for rhpam
     */
    public String rhpamCRUrl() {
        return rhpamCRUrl;
    }

    /**
     * @return the URL that holds CR build properties for bamoe
     */
    public String bamoeCRUrl() {
        return bamoeCRUrl;
    }

    /**
     * @return the nightly maven repo to fetch information
     * about the standalone jars needed by the KIE Server image
     */
    public String nightlyMavenRepo() {
        return nightlyMavenRepo;
    }

    /**
     * @return the CR maven repo to fetch information
     * about the standalone jars needed by the KIE Server image
     */
    public String crMavenRepo() {
        return crMavenRepo;
    }

    /**
     * @return rhpam/dm product shortened version
     */
    public String shortenedVersion(String customVersion) {
        String[] ver;
        if (null == customVersion || customVersion.isEmpty()) {
            ver = version.split("[.]");
            return ver[0] + "." + ver[1];
        }
        ver = customVersion.split("[.]");
        return ver[0] + "." + ver[1];
    }

    /**
     * @return rhpam/dm product version
     */
    public String version() {
        return version;
    }

    public Version getFormattedVersion() {
        Version fVersion = buildUtils.getVersion(version.split("[.]"));
        return fVersion;
    }

    /**
     * @return Google Chat webhook address to send notifications
     */
    public String gChatWebhook() {
        return gChatWebhook;
    }

    /**
     * @return if nightly builds watcher is enabled
     */
    public boolean isWatcherEnabled() {
        if (isWatcherEnabled) {
            if (null == rhpamUrl || rhpamUrl.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.rhpam.url is required!");
            }
            if (null == version || version.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.product.version is required!");
            }
        }
        return isWatcherEnabled;
    }

    /**
     * @return the preload file location, it will be read at startup time.
     */
    public String preLoadFileLocation() {
        return preLoadFileLocation;
    }

    /**
     * @return cacher artifacts dir location
     */
    public String getCacherArtifactsDir() {
        return cacherDataDir + "/artifacts";
    }

    /**
     * Path used for download in progess files
     *
     * @return cacher temporary files location
     */
    public String getArtifactsTmpDir() {
        return getCacherArtifactsDir() + "/tmp";
    }

    /**
     * @return cacher git repository base dir
     */
    public String getGitDir() {
        return cacherDataDir + "/git";
    }

    public boolean trustAllCerts() {
        return trustAll;
    }

    /**
     * @return all cacher directories
     */
    public List<String> getCacherDirs() {
        return Arrays.asList(cacherDataDir, getCacherArtifactsDir(), getArtifactsTmpDir(), getGitDir());
    }

    public String getKieVersion() {
        return kieVersion;
    }

    public void setKieVersion(String kieVersion) {
        if (null == kieVersion || kieVersion.isEmpty()) {
            kieVersion = "not-able-to-find-please-check";
        }
        this.kieVersion = kieVersion;
    }

    /**
     * @return properties key name for rhpam artifacts
     */
    public List<String> getFiles2DownloadPropName(String product) {
        if (product.equals("bamoe")) {
            return rhpamFiles2DownloadPropName.stream()
                    .map(item -> item.replace("rhpam", "bamoe"))
                    .collect(Collectors.toList());
        }
        return rhpamFiles2DownloadPropName;
    }


    /**
     * fetch the RHPAM build properties file.
     *
     * @param url
     * @return parsed properties from target url
     */
    public Properties productPropertyFile(String url) {
        log.info("Trying to get product properties file from " + url);
        Properties p = new Properties();

        try (Response response = HttpRequestHandler.executeHttpCall(url, trustAllCerts())) {
            if (response.code() == 404) {
                log.info("Property file not found... url -> " + url);
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

