package org.kie.cekit.cacher.builds.nightly;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.Version;
import org.kie.cekit.cacher.builds.github.BranchOperation;
import org.kie.cekit.cacher.builds.github.GitRepository;
import org.kie.cekit.cacher.builds.github.PullRequestSender;
import org.kie.cekit.cacher.builds.yaml.YamlFilesHelper;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.BuildUtils;
import org.kie.cekit.cacher.utils.CacherUtils;
import org.kie.cekit.image.descriptors.module.Module;

/**
 * this class holds all operations related with nightly rhdm/rhpam files changes
 */
@ApplicationScoped
public class NightlyBuildsPullRequestAcceptor implements NightlyBuildUpdatesInterceptor {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final Map<String, PlainArtifact> elements = new HashMap<>();

    @Inject
    GitRepository gitRepository;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    YamlFilesHelper yamlFilesHelper;

    @Inject
    PullRequestSender pullRequestSender;

    @Inject
    CacherUtils cacherUtils;

    @Inject
    BuildUtils buildUtils;

    /**
     * {@link NightlyBuildUpdatesInterceptor}
     *
     * @param artifact received to be persisted.
     */
    @Override
    public void onNewBuildReceived(PlainArtifact artifact, boolean force) {
        try {
            Version version = buildUtils.getVersion(artifact.getVersion().split("[.]"));

            log.fine("KIE_VERSION retrieve from Nightly properties file --> " + cacherProperties.getKieVersion());

            // for upstream compare the build date size to make sure the correct formatter is being used
            Optional<String> bDate = gitRepository.getCurrentProductBuildDate(artifact.getBranch());
            if (bDate.isEmpty()) {
                log.fine("Not able to determine the current build date, continuing...");
            } else {
                LocalDate upstreamBuildDate = LocalDate.parse(
                        bDate.get(),
                        buildUtils.formatter(bDate.get()));

                LocalDate buildDate = LocalDate.parse(artifact.getBuildDate(), buildUtils.formatter(version));

                if (buildDate.isAfter(upstreamBuildDate) || force) {
                    log.fine("File " + artifact.getFileName() + " received for PR.");
                    elements.put(artifact.getFileName(), artifact);
                } else {
                    log.fine(String.format("BuildDate received [%s] is before or equal than the upstream build date [%s]", buildDate, upstreamBuildDate));
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link NightlyBuildUpdatesInterceptor}
     *
     * @param fileName that was persisted and will be used to create a Pull request to update the artifact on its repository
     * @param checkSum for the given file
     */
    @Override
    public void onFilePersisted(String fileName, String checkSum) {

        try {

            if (elements.containsKey(fileName)) {

                log.fine("File received for pull request [" + fileName + " -  " + checkSum + "].");
                elements.get(fileName).setChecksum(checkSum);
                if (buildUtils.isRhpamReadyForPR(elements)) {
                    log.info("RHPAM is Ready to perform a Pull Request.");

                    // create a new branch
                    // only if all needed files are ready this step will be executed, any file is ok to retrieve
                    // the build date and branch.
                    String buildDate = elements.get(fileName).getBuildDate();

                    Version version = buildUtils.getVersion(elements.get(fileName).getVersion().split("[.]"));
                    String baseBranch = elements.get(fileName).getBranch();
                    String branchName = elements.get(fileName).getBranch() + "-" + buildDate + "-" + (int) (Math.random() * 100);
                    String product = fileName.startsWith("bamoe") ? "bamoe" : "rhpam";

                    gitRepository.handleBranch(BranchOperation.NEW_BRANCH, branchName, baseBranch, "rhpam-7-image");

                    Module bcMonitoring = yamlFilesHelper.load(buildUtils.bcMonitoringFile());
                    Module businessCentral = yamlFilesHelper.load(buildUtils.businessCentralFile());
                    Module pamController = yamlFilesHelper.load(buildUtils.pamControllerFile());
                    Module dashbuilder = yamlFilesHelper.load(buildUtils.dashbuilderFile());
                    Module pamKieserver = yamlFilesHelper.load(buildUtils.pamKieserverFile());
                    Module smartrouter = yamlFilesHelper.load(buildUtils.smartrouterFile());
                    Module processMigration = yamlFilesHelper.load(buildUtils.processMigrationFile());

                    // Prepare Business Central Monitoring Changes
                    String bcMonitoringZip = String.format(buildUtils.BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP, product);
                    bcMonitoring.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(bcMonitoringZip)) {
                            String bcMonitoringFileName = String.format(buildUtils.MONITORING_EE7_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                bcMonitoringFileName = String.format("rhpam-%s.PAM-redhat-%s-monitoring-ee7.zip", version, buildDate);
                            }
                            String bcMonitoringCheckSum;
                            try {
                                bcMonitoringCheckSum = elements.get(bcMonitoringFileName).getChecksum();

                                log.fine(String.format("Updating BC monitoring %s from [%s] to [%s]",
                                                       bcMonitoringZip,
                                                       artifact.getMd5(),
                                                       bcMonitoringCheckSum));

                                artifact.setMd5(bcMonitoringCheckSum);
                                yamlFilesHelper.writeModule(bcMonitoring, buildUtils.bcMonitoringFile());

                                // find name: "rhpam_business_central_monitoring_distribution.zip"
                                // and add comment on next line : rhpam-${version}.redhat-${buildDate}-monitoring-ee7.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-monitoring-ee7.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.bcMonitoringFile(), "name: \"" + bcMonitoringZip + "\"",
                                                        String.format("  # %s", bcMonitoringFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare Business Central Changes
                    String bcZip = String.format(buildUtils.BUSINESS_CENTRAL_DISTRIBUTION_ZIP, product);
                    businessCentral.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(bcZip)) {
                            String bcFileName = String.format(buildUtils.BUSINESS_CENTRAL_EAP7_DEPLOYABLE_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                bcFileName = String.format("rhpam-%s.PAM-redhat-%s-business-central-eap7-deployable.zip", version, buildDate);
                            }
                            String bcCheckSum;
                            try {
                                bcCheckSum = elements.get(bcFileName).getChecksum();

                                log.fine(String.format("Updating Business Central %s from [%s] to [%s]",
                                                       bcZip,
                                                       artifact.getMd5(),
                                                       bcCheckSum));

                                artifact.setMd5(bcCheckSum);
                                yamlFilesHelper.writeModule(businessCentral, buildUtils.businessCentralFile());

                                // find name: "rhpam_business_central_distribution.zip"
                                // and add comment on next line : rhpam-${version}.redhat-${buildDate}-business-central-eap7-deployable.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-business-central-eap7-deployable.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.businessCentralFile(), "name: \"" + bcZip + "\"",
                                                        String.format("  # %s", bcFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare controller Changes - artifacts
                    String addonsZip = String.format(buildUtils.ADD_ONS_DISTRIBUTION_ZIP, product);
                    pamController.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(addonsZip)) {
                            String controllerFileName = String.format(buildUtils.ADD_ONS_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                controllerFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String controllerCheckSum;
                            try {
                                controllerCheckSum = elements.get(controllerFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM Controller %s from [%s] to [%s]",
                                                       addonsZip,
                                                       artifact.getMd5(),
                                                       controllerCheckSum));

                                artifact.setMd5(controllerCheckSum);
                                yamlFilesHelper.writeModule(pamController, buildUtils.pamControllerFile());

                                // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam|bamoe-${version}.PAM-redhat-${buildDate}-add-ons.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.pamControllerFile(), "name: \"" + addonsZip + "\"",
                                                        String.format("  # %s", controllerFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare controller changes - envs
                    pamController.getEnvs().forEach(env -> {
                        if (env.getName().equals("CONTROLLER_DISTRIBUTION_ZIP")) {
                            // rhpam-${shortenedVersion}-controller-ee7.zip
                            String controllerEE7Zip = String.format("rhpam-%s-controller-ee7.zip", cacherProperties.shortenedVersion(version.toString()));
                            // if the filename does not match the current shortened version, update it
                            if (!env.getValue().equals(controllerEE7Zip)) {
                                env.setValue(controllerEE7Zip);
                            }
                        }
                    });

                    // Prepare dashbuilder Changes - artifacts
                    dashbuilder.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(addonsZip)) {
                            String dashbuilderAddOnsFileName = String.format(buildUtils.ADD_ONS_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                dashbuilderAddOnsFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            try {
                                String dashbuilderCheckSum = elements.get(dashbuilderAddOnsFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM Dashbuilder %s from [%s] to [%s]",
                                                       addonsZip,
                                                       artifact.getMd5(),
                                                       dashbuilderCheckSum));

                                artifact.setMd5(dashbuilderCheckSum);
                                yamlFilesHelper.writeModule(dashbuilder, buildUtils.dashbuilderFile());

                                // find name: "rhpam_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-add-ons.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.dashbuilderFile(), "name: \"" + addonsZip + "\"",
                                                        String.format("  # %s", dashbuilderAddOnsFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare dashbuilder changes - envs
                    dashbuilder.getEnvs().forEach(env -> {
                        if (env.getName().equals("DASHBUILDER_DISTRIBUTION_ZIP")) {
                            // rhpam-${version}-dashbuilder-runtime.zip
                            String dashbuilderEE7Zip = String.format("rhpam-%s-dashbuilder-runtime.zip", version.toString());
                            // if the filename does not match the current shortened version, update it
                            if (!env.getValue().equals(dashbuilderEE7Zip)) {
                                env.setValue(dashbuilderEE7Zip);
                            }
                        }
                    });

                    // Prepare kieserver changes, jbpm-wb-kie-server-backend file
                    String jbpmWbKieServerBackendSourceFile = String.format(buildUtils.BUSINESS_CENTRAL_EAP7_DEPLOYABLE_NIGHTLY_ZIP, product, version, buildDate);
                    String jbpmWbKieServerBackendVersion = cacherUtils.detectJarVersion("jbpm-wb-kie-server-backend", jbpmWbKieServerBackendSourceFile);
                    String backendFileName = String.format("jbpm-wb-kie-server-backend-%s.redhat-%s.jar", jbpmWbKieServerBackendVersion, buildDate);
                    pamKieserver.getEnvs().forEach(env -> {
                        if (env.getName().equals("JBPM_WB_KIE_SERVER_BACKEND_JAR")) {
                            log.fine(String.format("Update jbpm-wb-kie-server-backend file from [%s] to [%s]", env.getValue(), backendFileName));
                            env.setValue(backendFileName);
                            yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());
                        }
                    });

                    pamKieserver.getArtifacts().forEach(ar -> {
                        // compare the name without the string placeholder
                        // handle services-jbpm-cluster jar
                        String jbpmClusterJarPrefix = buildUtils.KIE_SEVER_SERVICES_JBPM_CLUSTER_JAR.split("-%s")[0];
                        if (ar.getName().contains(jbpmClusterJarPrefix)) {
                            String newJarName = String.format(buildUtils.KIE_SEVER_SERVICES_JBPM_CLUSTER_JAR, cacherProperties.getKieVersion());
                            String checksum = buildUtils.checkStandaloneJarChecksum(jbpmClusterJarPrefix,
                                                                                    cacherProperties.getKieVersion(),
                                                                                    ar.getMd5(),
                                                                                    Optional.of("nightly"),
                                                                                    0);

                            if (checksum != ar.getMd5()) {
                                ar.setName(newJarName);
                                ar.setMd5(checksum);
                                log.info("Found " + newJarName + " updating checksum to " + checksum);

                                yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());
                            } else {
                                log.info("Artifact " + jbpmClusterJarPrefix + " wil not be update, checksum didn't change. Check for previous errors if it is something not expected.");
                            }
                        }

                        // compare the name without the string placeholder
                        // handle jbpm-kafka-emitter jar
                        String jbpmEmitterKafkaJarPrefix = buildUtils.JBPM_EVENTS_EMITTERS_KAFKA_JAR.split("-%s")[0];
                        if (ar.getName().contains(jbpmEmitterKafkaJarPrefix)) {
                            String newJarName = String.format(buildUtils.JBPM_EVENTS_EMITTERS_KAFKA_JAR, cacherProperties.getKieVersion());
                            String checksum = buildUtils.checkStandaloneJarChecksum(jbpmEmitterKafkaJarPrefix,
                                                                                    cacherProperties.getKieVersion(),
                                                                                    ar.getMd5(),
                                                                                    Optional.of("nightly"),
                                                                                    0);

                            if (checksum != ar.getMd5()) {
                                ar.setName(newJarName);
                                ar.setMd5(checksum);
                                log.info("Found " + newJarName + " updating checksum to " + checksum);

                                yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());
                            } else {
                                log.info("Artifact " + jbpmEmitterKafkaJarPrefix + " wil not be update, checksum didn't change.");
                            }
                        }
                    });

                    String kieServerDistributionZip = String.format(buildUtils.KIE_SERVER_DISTRIBUTION_ZIP, "rhpam");
                    pamKieserver.getArtifacts().forEach(artifact -> {
                        String kieServerFileName = String.format(buildUtils.KIE_SERVER_EE8_NIGHTLY_ZIP, "rhpam", version, buildDate);
                        if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                            kieServerFileName = String.format("rhpam-%s.PAM-redhat-%s-kie-server-ee8.zip", version, buildDate);
                        }
                        if (artifact.getName().equals(kieServerDistributionZip)) {

                            String kieServerCheckSum;
                            try {
                                kieServerCheckSum = elements.get(kieServerFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM kieserver %s from [%s] to [%s]",
                                                       kieServerDistributionZip,
                                                       artifact.getMd5(),
                                                       kieServerCheckSum));

                                artifact.setMd5(kieServerCheckSum);
                                yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (artifact.getName().equals(bcZip)) {
                            String bcFileName = String.format(buildUtils.BUSINESS_CENTRAL_EAP7_DEPLOYABLE_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                bcFileName = String.format("rhpam-%s.PAM-redhat-%s-business-central-eap7-deployable.zip", version, buildDate);
                            }
                            String bcCheckSum;
                            try {
                                bcCheckSum = elements.get(bcFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM kieserver %s from [%s] to [%s]",
                                                       bcZip,
                                                       artifact.getMd5(),
                                                       bcCheckSum));

                                artifact.setMd5(bcCheckSum);
                                yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());

                                // Only add comments when the last write operation will be made.
                                // find name: "rhpam_business_central_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.redhat-${buildDate}-business-central-eap7-deployable.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-business-central-eap7-deployable.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), "name: \"" + bcZip + "\"",
                                                        String.format("  # %s", bcFileName));

                                // find name: "rhpam_kie_server_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.PAM-redhat-${buildDate}-kie-server-ee8.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-kie-server-ee8.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), "name: \"" + kieServerDistributionZip + "\"",
                                                        String.format("  # %s", kieServerFileName));

                                // find name: "slf4j-simple.jar"
                                // and add comment on next line :  slf4j-simple-1.7.22.redhat-2.jar
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), "name: \"slf4j-simple.jar\"", "  # slf4j-simple-1.7.22.redhat-2.jar");

                                // find value: "jbpm-wb-kie-server-backend-${version}.redhat-X.jar"
                                // and add comment on next line : # remember to also update "JBPM_WB_KIE_SERVER_BACKEND_JAR" value
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), String.format("  value: \"%s\"", backendFileName),
                                                        "# remember to also update \"JBPM_WB_KIE_SERVER_BACKEND_JAR\" value");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare smartrouter changes
                    smartrouter.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(addonsZip)) {
                            String smartrouterFileName = String.format(buildUtils.ADD_ONS_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                smartrouterFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String smartrouterCheckSum;
                            try {
                                smartrouterCheckSum = elements.get(smartrouterFileName).getChecksum();

                                log.fine(String.format("Updating Smartrouter %s from [%s] to [%s]",
                                                       addonsZip,
                                                       artifact.getMd5(),
                                                       smartrouterCheckSum));

                                artifact.setMd5(smartrouterCheckSum);
                                yamlFilesHelper.writeModule(smartrouter, buildUtils.smartrouterFile());

                                // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam|bamoe-${version}.PAM-redhat-${buildDate}-add-ons.zip
                                // depending on PAM version
                                buildUtils.reAddComment(buildUtils.smartrouterFile(), "name: \"" + addonsZip + "\"",
                                                        String.format("  # %s", smartrouterFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare process-migration changes
                    processMigration.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(addonsZip)) {
                            String processMigrationFileName = String.format(buildUtils.ADD_ONS_NIGHTLY_ZIP, product, version, buildDate);
                            if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
                                processMigrationFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String processMigrationCheckSum;
                            try {
                                processMigrationCheckSum = elements.get(processMigrationFileName).getChecksum();

                                log.fine(String.format("Updating Process-migration %s from [%s] to [%s]",
                                                       addonsZip,
                                                       artifact.getMd5(),
                                                       processMigrationCheckSum));

                                artifact.setMd5(processMigrationCheckSum);
                                yamlFilesHelper.writeModule(processMigration, buildUtils.processMigrationFile());

                                // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam|bamoe-${version}.PAM-redhat-${buildDate}-add-ons.zip depending on PAM version
                                buildUtils.reAddComment(buildUtils.processMigrationFile(), "name: \"" +addonsZip + "\"",
                                                        String.format("  # %s", processMigrationFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if (gitRepository.addChanges("rhpam-7-image")
                            && gitRepository.commitChanges("rhpam-7-image", branchName, "Applying RHPAM nightly build for build date " + buildDate)) {
                        log.fine("About to send Pull Request on rhpam-7-image git repository on branch " + branchName);

                        String prTittle = "Updating RHPAM artifacts based on the latest nightly build " + buildDate;
                        String prDescription = "This PR was created automatically, please review carefully before merge, the" +
                                " build date is " + buildDate + ". Do not merge if RHDM and RHPAM does not have the same build date.";
                        pullRequestSender.performPullRequest("rhpam-7-image", baseBranch, branchName, prTittle, prDescription);

                        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, branchName, null, "rhpam-7-image");
                    } else {
                        log.warning("something went wrong while preparing the rhpam-7-image for the pull request");
                    }

                    // remove rhpam from element items
                    removeItems("rhpam");
                }
            } else {
                log.info("File " + fileName + " not found on the nightly build elements map. ignoring...");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void removeItems(String pattern) {
        log.fine("Element items are: " + Collections.singletonList(elements));
        elements.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
    }

    /**
     * Expose the elements Map for test purpose
     */
    public Map<String, PlainArtifact> getElements() {
        return elements;
    }
}
