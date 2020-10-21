package org.kie.cekit.cacher.properties;

import com.fasterxml.jackson.core.Version;
import org.kie.cekit.cacher.exception.RequiredParameterMissingException;
import org.kie.cekit.cacher.properties.loader.CacherProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Holds all cacher's configurations
 */
@ApplicationScoped
public class CacherProperties {

    public final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    public final Pattern buildDatePattern = Pattern.compile("(\\d{8})");
    public final Version versionBeforeDMPAMPrefix = new Version(7, 8, 0, null, null, null);

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.base.dir", required = true)
    String cacherDataDir;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.username")
    String githubUsername;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.password")
    String githubPassword;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.email")
    String githubEmail;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.reviewers")
    String githubReviewers;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.github.rhdm.upstream.project")
    String rhdmUpstream;

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
    @CacherProperty(name = "org.kie.cekit.cacher.rhdm.url")
    String rhdmUrl;

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


    /**
     * RHPAM properties keys needed to download the nightly builds artifacts
     * These properties came from the product properties file.
     */
    private List<String> rhpamFiles2DownloadPropName = Arrays.asList(
            "rhpam.addons.latest.url",
            "rhpam.business-central-eap7.latest.url",
            "rhpam.monitoring.latest.url",
            "rhpam.kie-server.ee8.latest.url"
    );

    /**
     * RHM properties keys needed to download the nightly builds artifacts
     * These properties came from the product properties file.
     */
    private List<String> rhdmFiles2DownloadPropName = Arrays.asList(
            "rhdm.addons.latest.url",
            "rhdm.decision-central-eap7.latest.url",
            "rhdm.kie-server.ee8.latest.url"
    );

    /**
     * @return github Username
     */
    public String githubUsername() {
        return githubUsername;
    }

    /**
     * @return github password
     */
    public String githubPassword() {
        return githubPassword;
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
            if (null == githubPassword || githubPassword.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.password is required!");
            }
            if (null == githubEmail || githubEmail.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.email is required!");
            }
            if (null == githubReviewers || githubReviewers.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.reviewers is required!");
            }
            if (null == rhdmUpstream || rhdmUpstream.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.rhdm.upstream.project is required!");
            }
            if (null == rhpamUpstream || rhpamUpstream.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.github.rhpam.upstream.project is required!");
            }
        }
        return isGHBotEnabled;
    }

    /**
     * @return default branch for rhdm/pam upstream
     */
    public String defaultBranch() {
        return defaultBranch;
    }

    /**
     * @return the hanguots user id of the github PR reviewers
     */
    public String[] githubReviewers() {
        return githubReviewers.trim().split(",");
    }

    /**
     * @return rhdm upstream git repository
     */
    public String rhdmUpstream() {
        return rhdmUpstream;
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
     * @return rhdm nightly build url information
     */
    public String rhdmUrl() {
        return rhdmUrl;
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
            if (null == rhdmUrl || rhdmUrl.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.rhdm.url is required!");
            }
            if (null == version || version.equals("")) {
                throw new RequiredParameterMissingException("The parameter org.kie.cekit.cacher.product.version is required!");
            }
        }
        return isWatcherEnabled;
    }

    /**
     * @return the pre load file location, it will be read at startup time.
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

    /**
     * @return all cacher directories
     */
    public List<String> getCacherDirs() {
        return Arrays.asList(
                cacherDataDir,
                getCacherArtifactsDir(),
                getArtifactsTmpDir(),
                getGitDir()
        );
    }

    /**
     * @return properties key name for rhpam artifacts
     */
    public List<String> getRhpamFiles2DownloadPropName() {
        return rhpamFiles2DownloadPropName;
    }

    /**
     * @return properties key name for rhdm artifacts
     */
    public List<String> getRhdmFiles2DownloadPropName() {
        return rhdmFiles2DownloadPropName;
    }
}
