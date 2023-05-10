package org.kie.cekit.cacher.builds.cr;

import java.lang.invoke.MethodHandles;
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
import org.kie.cekit.image.descriptors.module.Module;

@ApplicationScoped
public class CRBuildsPullRequestAcceptor implements CRBuildInterceptor {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final Map<String, PlainArtifact> elements = new HashMap<>();

    @Inject
    CacherProperties cacherProperties;

    @Inject
    BuildUtils buildUtils;

    @Inject
    YamlFilesHelper yamlFilesHelper;

    @Inject
    GitRepository gitRepository;

    @Inject
    PullRequestSender pullRequestSender;

    @Override
    public void onRequestReceived(PlainArtifact artifact) {
        log.fine("Artifact received for CR build update --> " + artifact.toString());
        log.fine("KIE_VERSION retrieve from CR properties file --> " + cacherProperties.getKieVersion());
        elements.put(artifact.getFileName(), artifact);
    }

    @Override
    public void onFilePersisted(String fileName, String checkSum, int crBuild) {
        try {
            if (elements.containsKey(fileName)) {

                log.fine("File received for pull request [" + fileName + " - " + checkSum + "].");
                elements.get(fileName).setChecksum(checkSum);


                // RHPAM/BAMOE artifacts
                if (buildUtils.isRhpamReadyForPR(elements)) {
                    log.info("RHPAM CR [" + crBuild + "] is ready for PR.");

                    Version version = buildUtils.getVersion(elements.get(fileName).getVersion().split("[.]"));
                    String baseBranch = elements.get(fileName).getBranch();
                    String branchName = elements.get(fileName).getVersion() + "-CR" + crBuild + "-" + (int) (Math.random() * 100);
                    String product = fileName.startsWith("bamoe") ? "bamoe" : "rhpam";

                    gitRepository.handleBranch(BranchOperation.NEW_BRANCH,
                                               branchName,
                                               elements.get(fileName).getBranch(),
                                               "rhpam-7-image");

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
                            String bcMonitoringFileName = String.format(buildUtils.MONITORING_EE7_ZIP, product, version);

                            try {
                                log.fine(String.format("Updating BC monitoring %s from [%s] to [%s]",
                                                       bcMonitoringZip,
                                                       artifact.getMd5(),
                                                       elements.get(bcMonitoringFileName).getChecksum()));
                                artifact.setMd5(elements.get(bcMonitoringFileName).getChecksum());
                                yamlFilesHelper.writeModule(bcMonitoring, buildUtils.bcMonitoringFile());

                                // find name: "rhpam|bamoe_business_central_monitoring_distribution.zip"
                                // and add comment on next line : rhpam-${version}-monitoring-ee7.zip
                                buildUtils.reAddComment(buildUtils.bcMonitoringFile(), "name: \"" + bcMonitoringZip + "\"",
                                                        String.format("  # %s", bcMonitoringFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare Business Central Changes
                    String bcZip = String.format(buildUtils.BUSINESS_CENTRAL_DISTRIBUTION_ZIP, product);
                    String bcFileName = String.format(buildUtils.BUSINESS_CENTRAL_EAP7_DEPLOYABLE_ZIP, product, version);
                    businessCentral.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(bcZip)) {


                            try {

                                log.fine(String.format("Updating Business Central %s from [%s] to [%s]",
                                                       bcZip,
                                                       artifact.getMd5(),
                                                       elements.get(bcFileName).getChecksum()));
                                artifact.setMd5(elements.get(bcFileName).getChecksum());
                                yamlFilesHelper.writeModule(businessCentral, buildUtils.businessCentralFile());

                                // find name: "rhpam_business_central_distribution.zip"
                                // and add comment on next line : rhpam-${version}-business-central-eap7-deployable.zip
                                buildUtils.reAddComment(buildUtils.businessCentralFile(), "name: \"" + bcZip + "\"",
                                                        String.format("  # %s", bcFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare controller Changes - artifacts
                    String addonsDistributionZip = String.format(buildUtils.ADD_ONS_DISTRIBUTION_ZIP, product);
                    String addonsZip = String.format(buildUtils.ADD_ONS_ZIP, product, version);
                    pamController.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(addonsDistributionZip)) {
                            //String controllerFileName = String.format(buildUtils.ADD_ONS_ZIP, product, version);

                            try {

                                log.fine(String.format("Updating RHPAM Controller %s from [%s] to [%s]",
                                                       addonsDistributionZip,
                                                       artifact.getMd5(),
                                                       elements.get(addonsZip).getChecksum()));
                                artifact.setMd5(elements.get(addonsZip).getChecksum());
                                yamlFilesHelper.writeModule(pamController, buildUtils.pamControllerFile());

                                // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-%s-add-ons.zip
                                buildUtils.reAddComment(buildUtils.pamControllerFile(), "name: \"" + addonsDistributionZip + "\"",
                                                        String.format("  # %s", addonsZip));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare dashbuilder Changes - artifacts
                    // Dashbuilder is supported only on pam >= 7.10.0
                    if (version.compareTo(cacherProperties.pam710) >= 0) {
                        dashbuilder.getArtifacts().forEach(artifact -> {
                            if (artifact.getName().equals(addonsDistributionZip)) {
                                //String dashbuilderAddOnsFileName = String.format(buildUtils.ADD_ONS_ZIP, product, version);

                                try {
                                    log.fine(String.format("Updating RHPAM Dashbuilder %s from [%s] to [%s]",
                                                           addonsDistributionZip,
                                                           artifact.getMd5(),
                                                           elements.get(addonsZip).getChecksum()));

                                    artifact.setMd5(elements.get(addonsZip).getChecksum());
                                    yamlFilesHelper.writeModule(dashbuilder, buildUtils.dashbuilderFile());

                                    // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                    // and add comment on next line :  rhpam|bamoe-${version}-add-ons.zip
                                    buildUtils.reAddComment(buildUtils.dashbuilderFile(), "name: \"" + addonsDistributionZip + "\"",
                                                            String.format("  # %s", addonsZip));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    String backendFileName = buildUtils.getJbpmWbKieBackendVersion(
                            bcFileName,
                            Optional.empty());

                    pamKieserver.getEnvs().forEach(env -> {
                        if (env.getName().equals("JBPM_WB_KIE_SERVER_BACKEND_JAR")) {
                            log.fine(String.format("Updating jbpm-wb-kie-server-backend file from [%s] to [%s]", env.getValue(), backendFileName));
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
                                                                                    Optional.of("cr"),
                                                                                    crBuild);

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
                                                                                    Optional.of("cr"),
                                                                                    crBuild);

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

                    String kieServerDistributionZip = String.format(buildUtils.KIE_SERVER_DISTRIBUTION_ZIP, product);
                    pamKieserver.getArtifacts().forEach(artifact -> {
                        String kieServerFileName = String.format(buildUtils.KIE_SERVER_EE8_ZIP, product, version);

                        if (artifact.getName().equals(kieServerDistributionZip)) {

                            try {
                                log.fine(String.format("Updating KIE Server %s from [%s] to [%s]",
                                                       kieServerDistributionZip,
                                                       artifact.getMd5(),
                                                       elements.get(kieServerFileName).getChecksum()));

                                artifact.setMd5(elements.get(kieServerFileName).getChecksum());
                                yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (artifact.getName().equals(bcZip)) {
                            try {
                                log.fine(String.format("Updating Business Central zip on KIE Server %s from [%s] to [%s]",
                                                       bcZip,
                                                       artifact.getMd5(),
                                                       elements.get(bcFileName).getChecksum()));

                                artifact.setMd5(elements.get(bcFileName).getChecksum());
                                yamlFilesHelper.writeModule(pamKieserver, buildUtils.pamKieserverFile());

                                // Only add comments when the last write operation will be made.
                                // find name: "rhpam|bamoe_business_central_distribution.zip"
                                // and add comment on next line :  rhpam-${version}-business-central-eap7-deployable.zip
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), "name: \"" + bcZip + "\"",
                                                        String.format("  # %s", bcFileName));

                                // find name: "rhpam|bamoe_kie_server_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-${version}-kie-server-ee8.zip
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
                        if (artifact.getName().equals(addonsDistributionZip)) {
                           // String smartrouterFileName = String.format(buildUtils.ADD_ONS_ZIP, product, version);

                            try {
                                log.fine(String.format("Updating Smartrouter %s from [%s] to [%s]",
                                                       addonsDistributionZip,
                                                       artifact.getMd5(),
                                                       elements.get(addonsZip).getChecksum()));

                                artifact.setMd5(elements.get(addonsZip).getChecksum());
                                yamlFilesHelper.writeModule(smartrouter, buildUtils.smartrouterFile());

                                // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-${version}-add-ons.zip
                                buildUtils.reAddComment(buildUtils.smartrouterFile(), "name: \"" + addonsDistributionZip + "\"",
                                                        String.format("  # %s", addonsZip));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare process-migration changes
                    processMigration.getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(addonsDistributionZip)) {
                            //String processMigrationFileName = String.format(buildUtils.ADD_ONS_ZIP, product, version);

                            try {
                                log.fine(String.format("Updating RHPAM process-migration %s from [%s] to [%s]",
                                                       addonsDistributionZip,
                                                       artifact.getMd5(),
                                                       elements.get(addonsZip).getChecksum()));

                                artifact.setMd5(elements.get(addonsZip).getChecksum());
                                yamlFilesHelper.writeModule(processMigration, buildUtils.processMigrationFile());

                                // find name: "rhpam|bamoe_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam|bamoe-${version}-add-ons.zip
                                buildUtils.reAddComment(buildUtils.processMigrationFile(), "name: \"" + addonsDistributionZip + "\"",
                                                        String.format("  # %s", addonsZip));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if (gitRepository.addChanges("rhpam-7-image")
                            && gitRepository.commitChanges("rhpam-7-image", branchName, "Applying " + product.toUpperCase() + " CR build CR" + crBuild)) {

                        log.fine("About to send Pull Request on rhpam-7-image git repository cr CR build on branch " + branchName);
                        String prTittle = "Updating " + product.toUpperCase() + " artifacts based on the latest CR build " + crBuild;
                        String prDescription = "This PR was created automatically, please review carefully before merge, the" +
                                " CR build is CR" + crBuild + ".";
                        pullRequestSender.performPullRequest("rhpam-7-image", baseBranch, branchName, prTittle, prDescription);

                        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, branchName, null, "rhpam-7-image");
                    } else {
                        log.warning("something went wrong while preparing the rhpam-7-image for the pull request");
                    }
                    // remove rhpam from element items
                    removeItems("rhpam");
                }
            } else {
                log.info("File " + fileName + " not found on the cr build elements map. ignoring...");
            }
        } catch (
                final Exception e) {
            e.printStackTrace();
        }
    }

    public void removeItems(String pattern) {
        elements.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
        log.fine("Element items After Removal are: " + Collections.singletonList(elements));
    }
}
