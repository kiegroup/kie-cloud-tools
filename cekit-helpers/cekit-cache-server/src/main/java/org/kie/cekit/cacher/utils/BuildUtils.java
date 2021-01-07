package org.kie.cekit.cacher.utils;

import com.fasterxml.jackson.core.Version;
import org.kie.cekit.cacher.builds.yaml.YamlFilesHelper;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.image.descriptors.module.Module;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class BuildUtils {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    // artifacts and zip file names used on Nightly and CR builds
    public String RHDM_ADD_ONS_DISTRIBUTION_ZIP = "rhdm_add_ons_distribution.zip";
    public String RHDM_ADD_ONS_NIGHTLY_ZIP = "rhdm-%s.redhat-%s-add-ons.zip";
    public String RHDM_ADD_ONS_ZIP = "rhdm-%s-add-ons.zip";

    public String RHDM_DECISION_CENTRAL_DISTRIBUTION_ZIP = "rhdm_decision_central_distribution.zip";
    public String RHDM_DECISION_CENTRAL_EAP7_DEPLOYABLE_NIGHTLY_ZIP = "rhdm-%s.redhat-%s-decision-central-eap7-deployable.zip";
    public String RHDM_DECISION_CENTRAL_EAP7_DEPLOYABLE_ZIP = "rhdm-%s-decision-central-eap7-deployable.zip";

    public String RHDM_KIE_SERVER_DISTRIBUTION_ZIP = "rhdm_kie_server_distribution.zip";
    public String RHDM_KIE_SERVER_EE8_NIGHTLY_ZIP = "rhdm-%s.redhat-%s-kie-server-ee8.zip";
    public String RHDM_KIE_SERVER_EE8_ZIP = "rhdm-%s-kie-server-ee8.zip";

    public String RHPAM_BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP = "rhpam_business_central_monitoring_distribution.zip";
    public String RHPAM_MONITORING_EE7_NIGHTLY_ZIP = "rhpam-%s.redhat-%s-monitoring-ee7.zip";
    public String RHPAM_MONITORING_EE7_ZIP = "rhpam-%s-monitoring-ee7.zip";

    public String RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP = "rhpam_business_central_distribution.zip";
    public String RHPAM_BUSINESS_CENTRAL_EAP7_DEPLOYABLE_NIGHTLY_ZIP = "rhpam-%s.redhat-%s-business-central-eap7-deployable.zip";
    public String RHPAM_BUSINESS_CENTRAL_EAP7_DEPLOYABLE_ZIP = "rhpam-%s-business-central-eap7-deployable.zip";

    public String RHPAM_ADD_ONS_DISTRIBUTION_ZIP =  "rhpam_add_ons_distribution.zip";
    public String RHPAM_ADD_ONS_NIGHTLY_ZIP = "rhpam-%s.redhat-%s-add-ons.zip";
    public String RHPAM_ADD_ONS_ZIP = "rhpam-%s-add-ons.zip";

    public String RHPAM_KIE_SERVER_DISTRIBUTION_ZIP = "rhpam_kie_server_distribution.zip";
    public String RHPAM_KIE_SERVER_EE8_NIGHTLY_ZIP = "rhpam-%s.redhat-%s-kie-server-ee8.zip";
    public String RHPAM_KIE_SERVER_EE8_ZIP = "rhpam-%s-kie-server-ee8.zip";

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
     * @param buildDate for nightly builds
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
           return  String.format("jbpm-wb-kie-server-backend-%s.jar", jbpmWbKieServerBackendVersion);
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
        HashMap<String, PlainArtifact> rhpam = new HashMap<>();
        elements.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("rhpam-"))
                .forEach(entry -> rhpam.put(entry.getKey(), entry.getValue()));

        for (Map.Entry<String, PlainArtifact> element : rhpam.entrySet()) {
            if (element.getValue().getChecksum().isEmpty()) {
                isReady = false;
            }
        }
        return isReady && rhpam.size() == 4;
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
     * @param v String array with version
     * @return {@link Version}
     */
    public Version getVersion(String[] v) {
        log.fine("Trying to parse the version " + Arrays.deepToString(v));
        return new Version(Integer.parseInt(v[0]), Integer.parseInt(v[1]),
                Integer.parseInt(v[2]), null,null, null);
    }

    /**
     * Re-add comments on the module.yaml file.
     *
     * @param fileName file name
     * @param linePattern patter to search
     * @param comment comment that should be added
     */
    public void reAddComment(String fileName, String linePattern, String comment) {
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            List<String> replaced = lines.map(line -> line.replace(linePattern, linePattern + "\n" + comment))
                    .collect(Collectors.toList());
            Files.write(Paths.get(fileName), replaced);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
