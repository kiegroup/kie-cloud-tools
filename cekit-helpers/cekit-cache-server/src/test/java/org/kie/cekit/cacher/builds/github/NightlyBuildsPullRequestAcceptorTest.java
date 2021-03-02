package org.kie.cekit.cacher.builds.github;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.cekit.cacher.builds.yaml.YamlFilesHelper;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.BuildUtils;
import org.kie.cekit.cacher.utils.CacherUtils;
import org.kie.cekit.image.descriptors.module.Module;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NightlyBuildsPullRequestAcceptorTest {

    @Inject
    YamlFilesHelper yamlFilesHelper;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    BuildUtils buildUtils;

    @Inject
    GitRepository gitRepository;

    @Inject
    CacherUtils cacherUtils;

    @Test
    public void testNightlyRhpamCommentaryAddition() throws Exception {
        // reset git repo before this test
        gitRepository.cleanGitRepos();
        cacherUtils.startupVerifications();
        gitRepository.prepareLocalGitRepo();

        // test bc monitoring
        String bcMonitoringFile = cacherProperties.getGitDir() + "/rhpam-7-image/businesscentral-monitoring/modules/businesscentral-monitoring/module.yaml";
        Module bcMonitoring = yamlFilesHelper.load(bcMonitoringFile);
        yamlFilesHelper.writeModule(bcMonitoring, bcMonitoringFile);
        buildUtils.reAddComment(bcMonitoringFile, "name: \"" + buildUtils.RHPAM_BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-monitoring-ee7.zip"));
        Assertions.assertTrue(containsComment(bcMonitoringFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-monitoring-ee7.zip")));

        // test businessCentral
        String businessCentralFile = cacherProperties.getGitDir() + "/rhpam-7-image/businesscentral/modules/businesscentral/module.yaml";
        Module businessCentral = yamlFilesHelper.load(businessCentralFile);
        yamlFilesHelper.writeModule(businessCentral, businessCentralFile);
        buildUtils.reAddComment(businessCentralFile, "name: \"" + buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-business-central-eap7-deployable.zip"));
        Assertions.assertTrue(containsComment(businessCentralFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-business-central-eap7-deployable.zip")));

        // test rhpam controller
        String controllerFile = cacherProperties.getGitDir() + "/rhpam-7-image/controller/modules/controller/module.yaml";
        Module controller = yamlFilesHelper.load(controllerFile);
        yamlFilesHelper.writeModule(controller, controllerFile);
        buildUtils.reAddComment(controllerFile, "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip"));
        Assertions.assertTrue(containsComment(controllerFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip")));
        controller.getEnvs().stream().forEach(env -> {
            if (env.getName().equals("CONTROLLER_DISTRIBUTION_ZIP")) {
                // rhpam-${shortenedVersion}-controller-ee7.zip
                String controllerEE7Zip = String.format("rhpam-%s-controller-ee7.zip", "7.9");
                // if the filename does not match the current shortened version, update it
                if (!env.getValue().equals(controllerEE7Zip)) {
                    env.setValue(controllerEE7Zip);
                }
                Assertions.assertEquals(controllerEE7Zip, env.getValue());
            }
        });

        // test rhpam dashbuilder
        String dashbuilderFile = cacherProperties.getGitDir() + "/rhpam-7-image/dashbuilder/modules/dashbuilder/module.yaml";
        Module dashbuilder = yamlFilesHelper.load(dashbuilderFile);
        yamlFilesHelper.writeModule(dashbuilder, dashbuilderFile);
        buildUtils.reAddComment(dashbuilderFile, "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip"));
        Assertions.assertTrue(containsComment(dashbuilderFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip")));


        // test rhpam kieserver
        String kieserverFile = cacherProperties.getGitDir() + "/rhpam-7-image/kieserver/modules/kieserver/module.yaml";
        String buildDate = gitRepository.getCurrentProductBuildDate(cacherProperties.defaultBranch(), true);
        Module kieserver = yamlFilesHelper.load(kieserverFile);

        yamlFilesHelper.writeModule(kieserver, kieserverFile);

        String backendFileName = String.format("jbpm-wb-kie-server-backend-7.48.1.0.redhat-%s.jar", buildDate);
        buildUtils.reAddComment(kieserverFile, String.format("  value: \"%s\"", backendFileName),
                "# remember to also update \"JBPM_WB_KIE_SERVER_BACKEND_JAR\" value");
        Assertions.assertTrue(containsComment(kieserverFile, "# remember to also update \"JBPM_WB_KIE_SERVER_BACKEND_JAR\" value"));

        buildUtils.reAddComment(kieserverFile, "name: \"slf4j-simple.jar\"", "  # slf4j-simple-1.7.22.redhat-2.jar");
        Assertions.assertTrue(containsComment(kieserverFile,
                String.format("  # %s", "slf4j-simple-1.7.22.redhat-2.jar")));

        buildUtils.reAddComment(kieserverFile, "name: \"" + buildUtils.RHPAM_KIE_SERVER_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-kie-server-ee8.zip"));
        Assertions.assertTrue(containsComment(kieserverFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-kie-server-ee8.zip")));

        buildUtils.reAddComment(kieserverFile, "name: \"" + buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-business-central-eap7-deployable.zip"));
        Assertions.assertTrue(containsComment(kieserverFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-business-central-eap7-deployable.zip")));

        // smartrouter tests
        String smartrouterFile = cacherProperties.getGitDir() + "/rhpam-7-image/smartrouter/modules/smartrouter/module.yaml";
        Module smartrouter = yamlFilesHelper.load(smartrouterFile);
        yamlFilesHelper.writeModule(smartrouter, smartrouterFile);
        buildUtils.reAddComment(smartrouterFile, "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip"));
        Assertions.assertTrue(containsComment(smartrouterFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip")));

        // test rhpam process-migration
        String processMigrationFile = cacherProperties.getGitDir() + "/rhpam-7-image/process-migration/modules/process-migration/module.yaml";
        Module processMigration = yamlFilesHelper.load(processMigrationFile);
        yamlFilesHelper.writeModule(processMigration, processMigrationFile);
        buildUtils.reAddComment(processMigrationFile, "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip"));
        Assertions.assertTrue(containsComment(processMigrationFile, String.format("  # %s", "rhpam-7.8.0.redhat-201006-add-ons.zip")));

    }

    @Test
    public void testNightlyRhdmCommentaryAddition() throws IOException, InterruptedException {

        String controllerFile = cacherProperties.getGitDir() + "/rhdm-7-image/controller/modules/controller/module.yaml";
        Module controller = yamlFilesHelper.load(controllerFile);
        yamlFilesHelper.writeModule(controller, controllerFile);
        buildUtils.reAddComment(controllerFile, "name: \"" + buildUtils.RHDM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhdm-7.8.0.redhat-201006-add-ons.zip"));
        Assertions.assertTrue(containsComment(controllerFile,
                String.format("  # %s", "rhdm-7.8.0.redhat-201006-add-ons.zip")));
        // test env file name update
        controller.getEnvs().stream().forEach(env -> {
            if (env.getName().equals("CONTROLLER_DISTRIBUTION_ZIP")) {
                // rhdm-${shortenedVersion}-controller-ee7.zip
                String controllerEE7Zip = String.format("rhdm-%s-controller-ee7.zip", "7.9");
                // if the filename does not match the current shortened version, update it
                if (!env.getValue().equals(controllerEE7Zip)) {
                    env.setValue(controllerEE7Zip);
                }
                Assertions.assertEquals(controllerEE7Zip, env.getValue());
            }
        });


        String decisionCentralFile = cacherProperties.getGitDir() + "/rhdm-7-image/decisioncentral/modules/decisioncentral/module.yaml";
        Module decisionCentral = yamlFilesHelper.load(decisionCentralFile);
        yamlFilesHelper.writeModule(decisionCentral, decisionCentralFile);
        buildUtils.reAddComment(decisionCentralFile, "name: \"" + buildUtils.RHDM_DECISION_CENTRAL_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhdm-7.8.0.redhat-201006-decision-central-eap7-deployable.zip"));
        Assertions.assertTrue(containsComment(decisionCentralFile,
                String.format("  # %s", "rhdm-7.8.0.redhat-201006-decision-central-eap7-deployable.zip")));


        String kieserverFile = cacherProperties.getGitDir() + "/rhdm-7-image/kieserver/modules/kieserver/module.yaml";
        Module kieserver = yamlFilesHelper.load(kieserverFile);
        yamlFilesHelper.writeModule(kieserver, kieserverFile);
        buildUtils.reAddComment(kieserverFile, "name: \"" + buildUtils.RHDM_KIE_SERVER_DISTRIBUTION_ZIP + "\"",
                String.format("  # %s", "rhdm-7.8.0.redhat-201006-kie-server-ee8.zip"));
        buildUtils.reAddComment(kieserverFile, "name: \"slf4j-simple.jar\"", "  # slf4j-simple-1.7.22.redhat-2.jar");
        Assertions.assertTrue(containsComment(kieserverFile,
                String.format("  # %s", "rhdm-7.8.0.redhat-201006-kie-server-ee8.zip")));
        Assertions.assertTrue(containsComment(kieserverFile,
                String.format("  # %s", "slf4j-simple-1.7.22.redhat-2.jar")));
    }

    /**
     * verify if the comment was correctly added to the target file
     *
     * @param fileName
     * @param comment
     */
    private boolean containsComment(String fileName, String comment) {
        boolean containsString = false;
        String line;
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(comment)) {
                    containsString = true;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return containsString;
    }
}
