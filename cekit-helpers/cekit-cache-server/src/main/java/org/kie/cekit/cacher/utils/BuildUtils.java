package org.kie.cekit.cacher.utils;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.Version;
import okhttp3.Response;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;

@ApplicationScoped
public class BuildUtils {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
    public final DateTimeFormatter legacyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    public final Pattern buildDatePattern = Pattern.compile("(\\d{8})|(\\d{6})");

    public String BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP = "%s_business_central_monitoring_distribution.zip";
    public String MONITORING_EE7_NIGHTLY_ZIP = "%s-%s.redhat-%s-monitoring-ee7.zip";
    public String MONITORING_EE7_ZIP = "%s-%s-monitoring-ee7.zip";

    public String BUSINESS_CENTRAL_DISTRIBUTION_ZIP = "%s_business_central_distribution.zip";
    public String BUSINESS_CENTRAL_EAP7_DEPLOYABLE_NIGHTLY_ZIP = "%s-%s.redhat-%s-business-central-eap7-deployable.zip";
    public String BUSINESS_CENTRAL_EAP7_DEPLOYABLE_ZIP = "%s-%s-business-central-eap7-deployable.zip";

    public String ADD_ONS_DISTRIBUTION_ZIP = "%s_add_ons_distribution.zip";
    public String ADD_ONS_NIGHTLY_ZIP = "%s-%s.redhat-%s-add-ons.zip";
    public String ADD_ONS_ZIP = "%s-%s-add-ons.zip";

    public String KIE_SERVER_DISTRIBUTION_ZIP = "%s_kie_server_distribution.zip";
    public String KIE_SERVER_EE8_NIGHTLY_ZIP = "%s-%s.redhat-%s-kie-server-ee8.zip";
    public String KIE_SERVER_EE8_ZIP = "%s-%s-kie-server-ee8.zip";
    // for both jars, version will be added during nightly or CR builds update
    public String KIE_SEVER_SERVICES_JBPM_CLUSTER_JAR = "kie-server-services-jbpm-cluster-%s.jar";
    public String JBPM_EVENTS_EMITTERS_KAFKA_JAR = "jbpm-event-emitters-kafka-%s.jar";

    @Inject
    CacherProperties cacherProperties;

    @Inject
    CacherUtils cacherUtils;

    // RHPAM artifact files, shared between nightly and CR builds
    public String bcMonitoringFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/businesscentral-monitoring/modules/businesscentral-monitoring/module.yaml";
    }

    public String businessCentralFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/businesscentral/modules/businesscentral/module.yaml";
    }

    public String pamControllerFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/controller/modules/controller/module.yaml";
    }

    public String dashbuilderFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/dashbuilder/modules/dashbuilder/module.yaml";
    }

    public String pamKieserverFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/kieserver/modules/kieserver/module.yaml";
    }

    public String smartrouterFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/smartrouter/modules/smartrouter/module.yaml";
    }

    public String processMigrationFile() {
        return cacherProperties.getGitDir() + "/rhpam-7-image/process-migration/modules/process-migration/module.yaml";
    }

    // RHDM artifact files. Share between nightly and CR builds
    public String dmControllerFile() {
        return cacherProperties.getGitDir() + "/rhdm-7-image/controller/modules/controller/module.yaml";
    }

    public String decisionCentralFile() {
        return cacherProperties.getGitDir() + "/rhdm-7-image/decisioncentral/modules/decisioncentral/module.yaml";
    }

    public String dmKieserverFile() {
        return cacherProperties.getGitDir() + "/rhdm-7-image/kieserver/modules/kieserver/module.yaml";
    }

    /**
     * Extract the jar version from busineses central zip file.
     *
     * @param jbpmWbKieServerBackendSourceFile source file from where the jar will be extracted from
     * @param buildDate                        for nightly builds
     * @return jbpm-wb-kie-server-backend version
     */
    public String getJbpmWbKieBackendVersion(String jbpmWbKieServerBackendSourceFile, Optional<String> buildDate) {

        String jbpmWbKieServerBackendVersion = cacherUtils.detectJarVersion("jbpm-wb-kie-server-backend", jbpmWbKieServerBackendSourceFile);

        log.fine("Detected jbpm-wb-kie-server-backend version is [" + jbpmWbKieServerBackendVersion + "]");
        if (buildDate.isPresent()) {
            //nightly build
            return String.format("jbpm-wb-kie-server-backend-%s.redhat-%s.jar", jbpmWbKieServerBackendVersion, buildDate.get());
        } else {
            // CR builds
            return String.format("jbpm-wb-kie-server-backend-%s.jar", jbpmWbKieServerBackendVersion);
        }
    }

    /**
     * Verify if the elements HashMap contains all required rhpam files
     * Valid for Nightly and CR builds
     *
     * @return true if the files are ready or false if its not ready
     */
    public boolean isRhpamReadyForPR(Map<String, PlainArtifact> elements) {
        boolean isReady = true;
        HashMap<String, PlainArtifact> artifacts = new HashMap<>();
        elements.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("rhpam-") || entry.getKey().startsWith("bamoe-"))
                .forEach(entry -> artifacts.put(entry.getKey(), entry.getValue()));

        for (Map.Entry<String, PlainArtifact> element : artifacts.entrySet()) {
            if (element.getValue().getChecksum().isEmpty()) {
                isReady = false;
            }
        }
        return isReady && artifacts.size() == 4;
    }

    /**
     * Verify if the elements HashMap contains all required rhdm files
     * Valid for Nightly and CR builds
     *
     * @return true if the files are ready or false if its not ready
     */
    public boolean isRhdmReadyForPR(Map<String, PlainArtifact> elements) {
        boolean isReady = true;
        HashMap<String, PlainArtifact> rhdm = new HashMap<>();
        elements.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("rhdm-"))
                .forEach(entry -> rhdm.put(entry.getKey(), entry.getValue()));

        for (Map.Entry<String, PlainArtifact> element : rhdm.entrySet()) {
            if (element.getValue().getChecksum().isEmpty()) {
                isReady = false;
            }
        }
        return isReady && rhdm.size() == 3;
    }

    /**
     * parses string version to {@link Version}
     *
     * @param v String array with version
     * @return {@link Version}
     */
    public Version getVersion(String[] v) {
        log.fine("Trying to parse the version " + Arrays.deepToString(v));
        return new Version(Integer.parseInt(v[0]), Integer.parseInt(v[1]),
                           Integer.parseInt(v[2]), null, null, null);
    }

    /**
     * Re-add comments on the module.yaml file.
     *
     * @param fileName    file name
     * @param linePattern patter to search
     * @param comment     comment that should be added
     */
    public void reAddComment(String fileName, String linePattern, String comment) {
        Path path = Paths.get(fileName);
        try (Stream<String> lines = Files.lines(path)) {
            List<String> replaced = lines.map(line -> line.replace(linePattern, linePattern + "\n" + comment))
                    .collect(Collectors.toList());
            Files.write(path, replaced);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * returns the right build date formatter according the target version.
     *
     * @param version
     * @return DateTimeFormatter
     */
    public DateTimeFormatter formatter(Version version) {
        // build date for nightly build has changed from yyyyDDmm to yyDDmm on pam >= 7.11.0
        if (version.compareTo(cacherProperties.pam711) >= 0) {
            log.fine("version is >= 7.11, returning date formatter 'yyMMdd'");
            return formatter;
        }
        log.fine("version is < 7.11, returning date formatter 'yyyyMMdd'");
        return legacyFormatter;
    }

    /**
     * returns the right build date formatter according the target current build date.
     *
     * @param bdate - current build date
     * @return DateTimeFormatter
     */
    public DateTimeFormatter formatter(String bdate) {
        if (bdate.length() == 6) {
            log.fine("version is >= 7.11, returning date formatter 'yyMMdd'");
            return formatter;
        }
        log.fine("version is < 7.11, returning date formatter 'yyyyMMdd'");
        return legacyFormatter;
    }

    /**
     * @param standaloneJarName jar name without version
     * @param version desired version to fetch info
     * @param currentChecksum current checksum to compare with the new, if found, otherwise the old will be returned and
     *                        no update will be performed
     * @param crBuild in case, the update is a CR build, its number must be provided.
     * @return
     */
    public String checkStandaloneJarChecksum(String standaloneJarName, String version, String currentChecksum, Optional<String> type, int crBuild) {
        log.fine("Trying to fetch the standalone jar checksum " + standaloneJarName);
        String mavenRepo = cacherProperties.nightlyMavenRepo();
        if (type.get().equals("cr")) {
            mavenRepo = cacherProperties.crMavenRepo();
        }
        if (null == mavenRepo || mavenRepo.isEmpty()) {
            log.warning("Property 'org.kie.cekit.cacher.nightly.maven.repo' or 'org.kie.cekit.cr.nightly.maven.repo' not set, falling back to the current checksum");
            return currentChecksum;
        }

        String requestJarUrl = buildUrl(mavenRepo, standaloneJarName, version);
        String md5ChecksumUrl = requestJarUrl + ".md5";
        log.fine("Trying to get the artifact's checksum using the url --> " + md5ChecksumUrl);
        try (Response response = HttpRequestHandler.executeHttpCall(md5ChecksumUrl, cacherProperties.trustAllCerts())) {
            if (response.code() == 404) {
                log.warning("The artifact " + standaloneJarName + " was not found , url used: " + md5ChecksumUrl);
                return currentChecksum;
            }

            String newChecksum = response.body().string();

            if (null != newChecksum || !newChecksum.isEmpty()) {
                log.fine("Checksum for artifact " + standaloneJarName + " found, new value is " + newChecksum);
                log.fine("found new artifact " + standaloneJarName + ", requesting cacher do fetch it.");
                // do not make it async
                cacherUtils.fetchFile(requestJarUrl, type, crBuild);
            }
            return newChecksum;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return currentChecksum;
    }

    /**
     * @param mavenRepoURL      the maven repo from where the info will be pulled off
     * @param standaloneJarName the standalone jar name without version and suffix
     * @param version           the current jar version
     * @return the formatted URL
     */
    private String buildUrl(String mavenRepoURL, String standaloneJarName, String version) {
        StringBuilder urlBuilder = new StringBuilder(mavenRepoURL);
        if (!urlBuilder.toString().endsWith("/")) {
            urlBuilder.append("/");
        }
        if (standaloneJarName.contains("kie-server-services-jbpm-cluster")) {
            urlBuilder.append("org/kie/server/").append(standaloneJarName).append("/");
            urlBuilder.append(version).append("/");
            urlBuilder.append(String.format(KIE_SEVER_SERVICES_JBPM_CLUSTER_JAR, version));
        } else if (standaloneJarName.contains("jbpm-event-emitters-kafka")) {
            urlBuilder.append("org/jbpm/").append(standaloneJarName).append("/");
            urlBuilder.append(version).append("/");
            urlBuilder.append(String.format(JBPM_EVENTS_EMITTERS_KAFKA_JAR, version));
        }
        return urlBuilder.toString();
    }
}
