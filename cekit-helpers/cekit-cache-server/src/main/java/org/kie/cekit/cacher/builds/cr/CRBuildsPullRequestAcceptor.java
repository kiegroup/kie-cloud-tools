package org.kie.cekit.cacher.builds.cr;

import com.fasterxml.jackson.core.Version;
import org.kie.cekit.cacher.builds.github.BranchOperation;
import org.kie.cekit.cacher.builds.github.GitRepository;
import org.kie.cekit.cacher.builds.github.PullRequestSender;
import org.kie.cekit.cacher.builds.yaml.YamlFilesHelper;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.BuildUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

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
        elements.put(artifact.getFileName(), artifact);
    }

    @Override
    public void onFilePersisted(String fileName, String checkSum, int crBuild) {
        try {
            if (elements.containsKey(fileName)) {

                log.fine("File received for pull request [" + fileName + " - " + checkSum + "].");
                elements.get(fileName).setChecksum(checkSum);
                Version version = buildUtils.getVersion(elements.get(fileName).getVersion().split("[.]"));
                String baseBranch = elements.get(fileName).getBranch();
                String branchName = elements.get(fileName).getVersion() + "-CR" + crBuild + "-" + (int) (Math.random() * 100);

                if (buildUtils.isRhdmReadyForPR(elements)) {
                    log.info("RHDM CR [" + crBuild + "] is ready for PR.");

                    gitRepository.handleBranch(BranchOperation.NEW_BRANCH,
                            branchName,
                            elements.get(fileName).getBranch(),
                            "rhdm-7-image");

                    // Prepare controller Changes - artifacts
                    buildUtils.dmController().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHDM_ADD_ONS_DISTRIBUTION_ZIP)) {
                            String controllerFileName = String.format(buildUtils.RHDM_ADD_ONS_ZIP, version);
                            try {
                                log.fine(String.format("Updating RHDM Controller %s from [%s] to [%s]",
                                        buildUtils.RHDM_ADD_ONS_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(controllerFileName).getChecksum()));

                                artifact.setMd5(elements.get(controllerFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.dmController(), buildUtils.dmControllerFile());

                                // find name: "rhdm_add_ons_distribution.zip"
                                // and add comment on next line :  rhdm-${version}-add-ons.zip
                                buildUtils.reAddComment(buildUtils.dmControllerFile(), "name: \"" + buildUtils.RHDM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", controllerFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare Decision Central changes
                    buildUtils.decisionCentral().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHDM_DECISION_CENTRAL_DISTRIBUTION_ZIP)) {
                            String decisionCentralFileName = String.format(buildUtils.RHDM_DECISION_CENTRAL_EAP7_DEPLOYABLE_ZIP, version);

                            try {
                                log.fine(String.format("Updating RHDM Decision Central %s from [%s] to [%s]",
                                        buildUtils.RHDM_DECISION_CENTRAL_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(decisionCentralFileName).getChecksum()));

                                artifact.setMd5(elements.get(decisionCentralFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.decisionCentral(), buildUtils.decisionCentralFile());

                                // find name: "rhdm_decision_central_distribution.zip"
                                // and add comment on next line :  rhdm-${version}-decision-central-eap7-deployable.zip
                                buildUtils.reAddComment(buildUtils.decisionCentralFile(), "name: \"" + buildUtils.RHDM_DECISION_CENTRAL_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", decisionCentralFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare kieserver changes
                    buildUtils.dmKieserver().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHDM_KIE_SERVER_DISTRIBUTION_ZIP)) {
                            String kieserverFileName = String.format(buildUtils.RHDM_KIE_SERVER_EE8_ZIP, version);

                            try {
                                log.fine(String.format("Updating RHDM KieServer %s from [%s] to [%s]",
                                        buildUtils.RHDM_KIE_SERVER_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(kieserverFileName).getChecksum()));

                                artifact.setMd5(elements.get(kieserverFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.dmKieserver(), buildUtils.dmKieserverFile());

                                // find name: "rhdm_kie_server_distribution.zip"
                                // and add comment on next line :  rhdm-${version}-kie-server-ee8.zip
                                buildUtils.reAddComment(buildUtils.dmKieserverFile(), "name: \"" + buildUtils.RHDM_KIE_SERVER_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", kieserverFileName));

                                // find name: "slf4j-simple.jar"
                                // and add comment on next line :  slf4j-simple-1.7.22.redhat-2.jar
                                buildUtils.reAddComment(buildUtils.dmKieserverFile(), "name: \"slf4j-simple.jar\"",
                                        "  # slf4j-simple-1.7.22.redhat-2.jar");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if (gitRepository.addChanges("rhdm-7-image")
                            && gitRepository.commitChanges("rhdm-7-image", branchName, "Applying RHPAM CR build CR" + crBuild)) {

                        log.fine("About to send Pull Request on rhdm-7-image git repository on branch " + branchName);

                        log.fine("About to send Pull Request on rhdm-7-image git repository cr CR build on branch " + branchName);
                        String prTittle = "Updating RHDM artifacts based on the latest CR build " + crBuild;
                        String prDescription = "This PR was created automatically, please review carefully before merge, the" +
                                " CR build is CR" + crBuild + ". Do not merge if RHDM and RHPAM does not have the same CR build applied.";
                        pullRequestSender.performPullRequest("rhdm-7-image", baseBranch, branchName, prTittle, prDescription);

                        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, branchName, null, "rhdm-7-image");
                    } else {
                        log.warning("something went wrong while preparing the rhdm-7-image for the pull request");
                    }

                    // remove RHDM files elements
                    removeItems("rhdm");

                }

                // RHPAM artifacts
                if (buildUtils.isRhpamReadyForPR(elements)) {
                    log.info("RHPAM CR [" + crBuild + "] is ready for PR.");

                    gitRepository.handleBranch(BranchOperation.NEW_BRANCH,
                            branchName,
                            elements.get(fileName).getBranch(),
                            "rhpam-7-image");

                    // Prepare Business Central Monitoring Changes
                    buildUtils.bcMonitoring().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHPAM_BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP)) {
                            String bcMonitoringFileName = String.format(buildUtils.RHPAM_MONITORING_EE7_ZIP, version);

                            try {
                                log.fine(String.format("Updating BC monitoring %s from [%s] to [%s]",
                                        buildUtils.RHPAM_BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(bcMonitoringFileName).getChecksum()));
                                artifact.setMd5(elements.get(bcMonitoringFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.bcMonitoring(), buildUtils.bcMonitoringFile());

                                // find name: "rhpam_business_central_monitoring_distribution.zip"
                                // and add comment on next line : rhpam-${version}-monitoring-ee7.zip
                                buildUtils.reAddComment(buildUtils.bcMonitoringFile(), "name: \"" + buildUtils.RHPAM_BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", bcMonitoringFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare Business Central Changes
                    buildUtils.businessCentral().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP)) {
                            String bcFileName = String.format(buildUtils.RHPAM_BUSINESS_CENTRAL_EAP7_DEPLOYABLE_ZIP, version);

                            try {

                                log.fine(String.format("Updating Business Central %s from [%s] to [%s]",
                                        buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(bcFileName).getChecksum()));
                                artifact.setMd5(elements.get(bcFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.businessCentral(), buildUtils.businessCentralFile());

                                // find name: "rhpam_business_central_distribution.zip"
                                // and add comment on next line : rhpam-${version}-business-central-eap7-deployable.zip
                                buildUtils.reAddComment(buildUtils.businessCentralFile(), "name: \"" + buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", bcFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare controller Changes - artifacts
                    buildUtils.pamController().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP)) {
                            String controllerFileName = String.format(buildUtils.RHPAM_ADD_ONS_ZIP, version);

                            try {

                                log.fine(String.format("Updating RHPAM Controller %s from [%s] to [%s]",
                                        buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(controllerFileName).getChecksum()));
                                artifact.setMd5(elements.get(controllerFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.pamController(), buildUtils.pamControllerFile());

                                // find name: "rhpam_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-%s-add-ons.zip
                                buildUtils.reAddComment(buildUtils.pamControllerFile(), "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", controllerFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare dashbuilder Changes - artifacts
                    // Dashbuilder is supported only on pam >= 7.10.0
                    if (version.compareTo(cacherProperties.pam710) >= 0) {
                        buildUtils.dashbuilder().getArtifacts().forEach(artifact -> {
                            if (artifact.getName().equals(buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP)) {
                                String dashbuilderAddOnsFileName = String.format(buildUtils.RHPAM_ADD_ONS_ZIP, version);

                                try {
                                    log.fine(String.format("Updating RHPAM Dashbuilder %s from [%s] to [%s]",
                                            buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP,
                                            artifact.getMd5(),
                                            elements.get(dashbuilderAddOnsFileName).getChecksum()));

                                    artifact.setMd5(elements.get(dashbuilderAddOnsFileName).getChecksum());
                                    yamlFilesHelper.writeModule(buildUtils.dashbuilder(), buildUtils.dashbuilderFile());

                                    // find name: "rhpam_add_ons_distribution.zip"
                                    // and add comment on next line :  rhpam-${version}-add-ons.zip
                                    buildUtils.reAddComment(buildUtils.dashbuilderFile(), "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                                            String.format("  # %s", dashbuilderAddOnsFileName));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }


                    String backendFileName = buildUtils.getJbpmWbKieBackendVersion(
                            String.format(buildUtils.RHPAM_BUSINESS_CENTRAL_EAP7_DEPLOYABLE_ZIP, version),
                            Optional.empty());

                    buildUtils.pamKieserver().getEnvs().forEach(env -> {
                        if (env.getName().equals("JBPM_WB_KIE_SERVER_BACKEND_JAR")) {
                            log.fine(String.format("Updating jbpm-wb-kie-server-backend file from [%s] to [%s]", env.getValue(), backendFileName));
                            env.setValue(backendFileName);
                            yamlFilesHelper.writeModule(buildUtils.pamKieserver(), buildUtils.pamKieserverFile());
                        }
                    });
                    buildUtils.pamKieserver().getArtifacts().forEach(artifact -> {
                        String kieServerFileName = String.format(buildUtils.RHPAM_KIE_SERVER_EE8_ZIP, version);

                        if (artifact.getName().equals(buildUtils.RHPAM_KIE_SERVER_DISTRIBUTION_ZIP)) {

                            try {
                                log.fine(String.format("Updating RHPAM kieserver %s from [%s] to [%s]",
                                        buildUtils.RHPAM_KIE_SERVER_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(kieServerFileName).getChecksum()));

                                artifact.setMd5(elements.get(kieServerFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.pamKieserver(), buildUtils.pamKieserverFile());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (artifact.getName().equals(buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP)) {
                            String bcFileName = String.format(buildUtils.RHPAM_BUSINESS_CENTRAL_EAP7_DEPLOYABLE_ZIP, version);

                            try {
                                log.fine(String.format("Updating RHPAM kieserver %s from [%s] to [%s]",
                                        buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(bcFileName).getChecksum()));

                                artifact.setMd5(elements.get(bcFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.pamKieserver(), buildUtils.pamKieserverFile());

                                // Only add comments when the last write operation will be made.
                                // find name: "rhpam_business_central_distribution.zip"
                                // and add comment on next line :  rhpam-${version}-business-central-eap7-deployable.zip
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), "name: \"" + buildUtils.RHPAM_BUSINESS_CENTRAL_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", bcFileName));

                                // find name: "rhpam_kie_server_distribution.zip"
                                // and add comment on next line :  rhpam-${version}-kie-server-ee8.zip
                                buildUtils.reAddComment(buildUtils.pamKieserverFile(), "name: \"" + buildUtils.RHPAM_KIE_SERVER_DISTRIBUTION_ZIP + "\"",
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
                    buildUtils.smartrouter().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP)) {
                            String smartrouterFileName = String.format(buildUtils.RHPAM_ADD_ONS_ZIP, version);

                            try {
                                log.fine(String.format("Updating RHPAM smartrouter %s from [%s] to [%s]",
                                        buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(smartrouterFileName).getChecksum()));

                                artifact.setMd5(elements.get(smartrouterFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.smartrouter(), buildUtils.smartrouterFile());

                                // find name: "rhpam_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-${version}-add-ons.zip
                                buildUtils.reAddComment(buildUtils.smartrouterFile(), "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", smartrouterFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare process-migration changes
                    buildUtils.processMigration().getArtifacts().forEach(artifact -> {
                        if (artifact.getName().equals(buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP)) {
                            String processMigrationFileName = String.format(buildUtils.RHPAM_ADD_ONS_ZIP, version);

                            try {
                                log.fine(String.format("Updating RHPAM process-migration %s from [%s] to [%s]",
                                        buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP,
                                        artifact.getMd5(),
                                        elements.get(processMigrationFileName).getChecksum()));

                                artifact.setMd5(elements.get(processMigrationFileName).getChecksum());
                                yamlFilesHelper.writeModule(buildUtils.processMigration(), buildUtils.processMigrationFile());

                                // find name: "rhpam_add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-${version}-add-ons.zip
                                buildUtils.reAddComment(buildUtils.processMigrationFile(), "name: \"" + buildUtils.RHPAM_ADD_ONS_DISTRIBUTION_ZIP + "\"",
                                        String.format("  # %s", processMigrationFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if (gitRepository.addChanges("rhpam-7-image")
                            && gitRepository.commitChanges("rhpam-7-image", branchName, "Applying RHPAM CR build CR" + crBuild)) {

                        log.fine("About to send Pull Request on rhpam-7-image git repository cr CR build on branch " + branchName);
                        String prTittle = "Updating RHPAM artifacts based on the latest CR build " + crBuild;
                        String prDescription = "This PR was created automatically, please review carefully before merge, the" +
                                " CR build is CR" + crBuild + ". Do not merge if RHDM and RHPAM does not have the same CR build applied.";
                        pullRequestSender.performPullRequest("rhpam-7-image", baseBranch, branchName, prTittle, prDescription);

                        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, branchName, null, "rhpam-7-image");
                    } else {
                        log.warning("something went wrong while preparing the rhpam-7-image for the pull request");
                    }
                    // remove rhpam from element items
                    removeItems("rhpam");
                }

            } else {
                log.info("File " + fileName + " not found on the elements map. ignoring...");
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
