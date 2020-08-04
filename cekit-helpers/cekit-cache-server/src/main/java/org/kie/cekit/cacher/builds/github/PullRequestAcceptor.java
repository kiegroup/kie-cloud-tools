package org.kie.cekit.cacher.builds.github;

import org.kie.cekit.cacher.builds.yaml.YamlFilesHelper;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.image.descriptors.module.Module;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * this class holds all operations related with rhdm/rhpam files changes
 */
@ApplicationScoped
public class PullRequestAcceptor implements BuildDateUpdatesInterceptor {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private Map<String, PlainArtifact> elements = new HashMap<>();

    @Inject
    GitRepository gitRepository;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    YamlFilesHelper yamlFilesHelper;

    @Inject
    PullRequestSender pullRequestSender;

    /**
     * {@link BuildDateUpdatesInterceptor}
     *
     * @param artifact
     */
    @Override
    public void onNewBuildReceived(PlainArtifact artifact, boolean force) {
        try {
            LocalDate upstreamBuildDate = LocalDate.parse(gitRepository.getCurrentProductBuildDate(artifact.getBranch(), force), cacherProperties.formatter);
            LocalDate buildDate = LocalDate.parse(artifact.getBuildDate(), cacherProperties.formatter);

            if (buildDate.isAfter(upstreamBuildDate)) {
                log.fine("File " + artifact.getFileName() + " received for PR.");
                elements.put(artifact.getFileName(), artifact);
            } else {
                log.fine(String.format("BuildDate received [%s] is before or equal than the upstream build date [%s]", buildDate, upstreamBuildDate));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link BuildDateUpdatesInterceptor}
     *
     * @param fileName
     * @param checkSum
     */
    @Override
    public void onFilePersisted(String fileName, String checkSum) {

        try {

            if (elements.containsKey(fileName)) {
                log.fine("File received for pull request " + fileName);
                elements.get(fileName).setChecksum(checkSum);

                if (isRhpamReadyForPR()) {
                    log.info("RHPAM is Ready to perform a Pull Request.");

                    // create a new branch
                    // only if all needed files are ready this step will be executed, any file is ok to retrieve
                    // the build date and branch.
                    String buildDate = elements.get(fileName).getBuildDate();
                    String version = elements.get(fileName).getVersion();
                    String baseBranch = elements.get(fileName).getBranch();
                    String branchName = elements.get(fileName).getBranch() + "-" + buildDate + "-" + (int) (Math.random() * 100);

                    gitRepository.handleBranch(BranchOperation.NEW_BRANCH, branchName, baseBranch, "rhpam-7-image");

                    String bcMonitoringFile = cacherProperties.getGitDir() + "/rhpam-7-image/businesscentral-monitoring/modules/businesscentral-monitoring/module.yaml";
                    Module bcMonitoring = yamlFilesHelper.load(bcMonitoringFile);

                    String businessCentralFile = cacherProperties.getGitDir() + "/rhpam-7-image/businesscentral/modules/businesscentral/module.yaml";
                    Module businessCentral = yamlFilesHelper.load(businessCentralFile);

                    String controllerFile = cacherProperties.getGitDir() + "/rhpam-7-image/controller/modules/controller/module.yaml";
                    Module controller = yamlFilesHelper.load(controllerFile);

                    String kieserverFile = cacherProperties.getGitDir() + "/rhpam-7-image/kieserver/modules/kieserver/module.yaml";
                    Module kieserver = yamlFilesHelper.load(kieserverFile);

                    String smartrouterFile = cacherProperties.getGitDir() + "/rhpam-7-image/smartrouter/modules/smartrouter/module.yaml";
                    Module smartrouter = yamlFilesHelper.load(smartrouterFile);

                    String processMigrationFile = cacherProperties.getGitDir() + "/rhpam-7-image/process-migration/modules/process-migration/module.yaml";
                    Module processMigration = yamlFilesHelper.load(processMigrationFile);

                    // Prepare Business Central Monitoring Changes
                    bcMonitoring.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("BUSINESS_CENTRAL_MONITORING_DISTRIBUTION_ZIP")) {
                            String bcMonitoringFileName = String.format("rhpam-%s.redhat-%s-monitoring-ee7.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                bcMonitoringFileName = String.format("rhpam-%s.PAM-redhat-%s-monitoring-ee7.zip", version, buildDate);
                            }
                            String bcMonitoringCheckSum;
                            try {
                                bcMonitoringCheckSum = elements.get(bcMonitoringFileName).getChecksum();

                                log.fine(String.format("Updating BC monitoring from [%s] to [%s]", artifact.getMd5(), bcMonitoringCheckSum));
                                artifact.setMd5(bcMonitoringCheckSum);
                                yamlFilesHelper.writeModule(bcMonitoring, bcMonitoringFile);

                                // find target: "business_central_monitoring_distribution.zip"
                                // and add comment on next line : rhpam-${version}.redhat-${buildDate}-monitoring-ee7.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-monitoring-ee7.zip depending on PAM version
                                reAddComment(bcMonitoringFile, "target: \"business_central_monitoring_distribution.zip\"",
                                        String.format("  # %s", bcMonitoringFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare Business Central Changes
                    businessCentral.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("BUSINESS_CENTRAL_DISTRIBUTION_ZIP")) {
                            String bcFileName = String.format("rhpam-%s.redhat-%s-business-central-eap7-deployable.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                bcFileName = String.format("rhpam-%s.PAM-redhat-%s-business-central-eap7-deployable.zip", version, buildDate);
                            }
                            String bcCheckSum;
                            try {
                                bcCheckSum = elements.get(bcFileName).getChecksum();

                                log.fine(String.format("Updating Business Central from [%s] to [%s]", artifact.getMd5(), bcCheckSum));
                                artifact.setMd5(bcCheckSum);
                                yamlFilesHelper.writeModule(businessCentral, businessCentralFile);

                                // find target: "business_central_distribution.zip"
                                // and add comment on next line : rhpam-${version}.redhat-${buildDate}-business-central-eap7-deployable.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-business-central-eap7-deployable.zip depending on PAM version
                                reAddComment(businessCentralFile, "target: \"business_central_distribution.zip\"",
                                        String.format("  # %s", bcFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare controller Changes - artifacts
                    controller.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("ADD_ONS_DISTRIBUTION_ZIP")) {
                            String controllerFileName = String.format("rhpam-%s.redhat-%s-add-ons.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                controllerFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String controllerCheckSum;
                            try {
                                controllerCheckSum = elements.get(controllerFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM Controller from [%s] to [%s]", artifact.getMd5(), controllerCheckSum));
                                artifact.setMd5(controllerCheckSum);
                                yamlFilesHelper.writeModule(controller, controllerFile);

                                // find target: "add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-add-ons.zip depending on PAM version
                                reAddComment(controllerFile, "target: \"add_ons_distribution.zip\"",
                                        String.format("  # %s", controllerFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    // Prepare controller changes - envs
                    controller.getEnvs().stream().forEach(env -> {
                        if (env.getName().equals("CONTROLLER_DISTRIBUTION_ZIP")) {
                            // rhpam-${shortenedVersion}-controller-ee7.zip
                            String controllerEE7Zip = String.format("rhpam-%s-controller-ee7.zip", cacherProperties.shortenedVersion(version));
                            // if the filename does not match the current shortened version, update it
                            if (!env.getValue().equals(controllerEE7Zip)) {
                                env.setValue(controllerEE7Zip);
                            }
                        }
                    });

                    // Prepare kieserver changes, jbpm-wb-kie-server-backend file
                    String backendFileName = String.format("jbpm-wb-kie-server-backend-%s.redhat-%s.jar", version, buildDate);
                    kieserver.getEnvs().stream().forEach(env -> {
                        if (env.getName().equals("JBPM_WB_KIE_SERVER_BACKEND_JAR")) {

                            log.fine(String.format("Update jbpm-wb-kie-server-backend file from [%s] to [%s]", env.getValue(), backendFileName));
                            env.setValue(backendFileName);
                            yamlFilesHelper.writeModule(kieserver, kieserverFile);
                        }
                    });
                    kieserver.getArtifacts().stream().forEach(artifact -> {
                        String kieServerFileName = String.format("rhpam-%s.redhat-%s-kie-server-ee8.zip", version, buildDate);
                        if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                            kieServerFileName = String.format("rhpam-%s.PAM-redhat-%s-kie-server-ee8.zip", version, buildDate);
                        }
                        if (artifact.getName().equals("KIE_SERVER_DISTRIBUTION_ZIP")) {

                            String kieServerCheckSum;
                            try {
                                kieServerCheckSum = elements.get(kieServerFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM kieserver KIE_SERVER_DISTRIBUTION_ZIP from [%s] to [%s]", artifact.getMd5(), kieServerCheckSum));
                                artifact.setMd5(kieServerCheckSum);
                                yamlFilesHelper.writeModule(kieserver, kieserverFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (artifact.getName().equals("BUSINESS_CENTRAL_DISTRIBUTION_ZIP")) {
                            String bcFileName = String.format("rhpam-%s.redhat-%s-business-central-eap7-deployable.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                bcFileName = String.format("rhpam-%s.PAM-redhat-%s-business-central-eap7-deployable.zip", version, buildDate);
                            }
                            String bcCheckSum;
                            try {
                                bcCheckSum = elements.get(bcFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM kieserver BUSINESS_CENTRAL_DISTRIBUTION_ZIP from [%s] to [%s]", artifact.getMd5(), bcCheckSum));
                                artifact.setMd5(bcCheckSum);
                                yamlFilesHelper.writeModule(kieserver, kieserverFile);

                                // Only add comments when the last write operation will be made.
                                // find target: "business_central_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.redhat-${buildDate}-business-central-eap7-deployable.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-business-central-eap7-deployable.zip depending on PAM version
                                reAddComment(kieserverFile, "target: \"business_central_distribution.zip\"",
                                        String.format("  # %s", bcFileName));

                                // find target: "kie_server_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.PAM-redhat-${buildDate}-kie-server-ee8.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-kie-server-ee8.zip depending on PAM version
                                reAddComment(kieserverFile, "target: \"kie_server_distribution.zip\"",
                                        String.format("  # %s", kieServerFileName));

                                // find target: "slf4j-simple.jar"
                                // and add comment on next line :  slf4j-simple-1.7.22.redhat-2.jar
                                reAddComment(kieserverFile, "target: \"slf4j-simple.jar\"", "  # slf4j-simple-1.7.22.redhat-2.jar");

                                // find target: "jbpm-wb-kie-server-backend-${version}.redhat-X.jar"
                                // and add comment on next line : # remember to also update "JBPM_WB_KIE_SERVER_BACKEND_JAR" value
                                reAddComment(kieserverFile, String.format("  value: \"%s\"", backendFileName),
                                        "# remember to also update \"JBPM_WB_KIE_SERVER_BACKEND_JAR\" value");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare smartrouter changes
                    smartrouter.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("ADD_ONS_DISTRIBUTION_ZIP")) {
                            String smartrouterFileName = String.format("rhpam-%s.redhat-%s-add-ons.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                smartrouterFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String smartrouterCheckSum;
                            try {
                                smartrouterCheckSum = elements.get(smartrouterFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM smartrouter ADD_ONS_DISTRIBUTION_ZIP from [%s] to [%s]", artifact.getMd5(), smartrouterCheckSum));
                                artifact.setMd5(smartrouterCheckSum);
                                yamlFilesHelper.writeModule(smartrouter, smartrouterFile);

                                // find target: "add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-add-ons.zip
                                // depending on PAM version
                                reAddComment(smartrouterFile, "target: \"add_ons_distribution.zip\"",
                                        String.format("  # %s", smartrouterFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare process-migration changes
                    processMigration.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("ADD_ONS_DISTRIBUTION_ZIP")) {
                            String processMigrationFileName = String.format("rhpam-%s.redhat-%s-add-ons.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                processMigrationFileName = String.format("rhpam-%s.PAM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String processMigrationCheckSum;
                            try {
                                processMigrationCheckSum = elements.get(processMigrationFileName).getChecksum();

                                log.fine(String.format("Updating RHPAM process-migration ADD_ONS_DISTRIBUTION_ZIP from [%s] to [%s]", artifact.getMd5(), processMigrationCheckSum));
                                artifact.setMd5(processMigrationCheckSum);
                                yamlFilesHelper.writeModule(processMigration, processMigrationFile);

                                // find target: "add_ons_distribution.zip"
                                // and add comment on next line :  rhpam-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhpam-${version}.PAM-redhat-${buildDate}-add-ons.zip depending on PAM version
                                reAddComment(processMigrationFile, "target: \"add_ons_distribution.zip\"",
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
                                " build date is " + buildDate;
                        pullRequestSender.performPullRequest("rhpam-7-image", baseBranch, branchName, prTittle, prDescription);

                        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, branchName, null, "rhpam-7-image");
                    } else {
                        log.warning("something went wrong while preparing the rhpam-7-image for the pull request");
                    }

                    // remove rhpam from element items
                    removeItems("rhpam");
                }

                if (isRhdmReadyForPR()) {
                    log.info("RHDM is Ready to perform a Pull Request.");

                    // create a new branch
                    // only if all needed files are ready this step will be executed, any file is ok to retrieve
                    // the build date, version and branch.
                    String buildDate = elements.get(fileName).getBuildDate();
                    String version = elements.get(fileName).getVersion();
                    String baseBranch = elements.get(fileName).getBranch();
                    String branchName = elements.get(fileName).getBranch() + "-" + buildDate + "-" + (int) (Math.random() * 100);

                    gitRepository.handleBranch(BranchOperation.NEW_BRANCH, branchName, baseBranch, "rhdm-7-image");

                    // load all required files:
                    String controllerFile = cacherProperties.getGitDir() + "/rhdm-7-image/controller/modules/controller/module.yaml";
                    Module controller = yamlFilesHelper.load(controllerFile);

                    String decisionCentralFile = cacherProperties.getGitDir() + "/rhdm-7-image/decisioncentral/modules/decisioncentral/module.yaml";
                    Module decisionCentral = yamlFilesHelper.load(decisionCentralFile);

                    String kieserverFile = cacherProperties.getGitDir() + "/rhdm-7-image/kieserver/modules/kieserver/module.yaml";
                    Module kieserver = yamlFilesHelper.load(kieserverFile);

                    // Prepare controller Changes - artifacts
                    controller.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("ADD_ONS_DISTRIBUTION_ZIP")) {
                            String controllerFileName = String.format("rhdm-%s.redhat-%s-add-ons.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                controllerFileName = String.format("rhdm-%s.DM-redhat-%s-add-ons.zip", version, buildDate);
                            }
                            String controllerCheckSum;
                            try {
                                controllerCheckSum = elements.get(controllerFileName).getChecksum();

                                log.fine(String.format("Updating RHDM Controller from [%s] to [%s]", artifact.getMd5(), controllerCheckSum));
                                artifact.setMd5(controllerCheckSum);
                                yamlFilesHelper.writeModule(controller, controllerFile);

                                // find target: "add_ons_distribution.zip"
                                // and add comment on next line :  rhdm-${version}.redhat-${buildDate}-add-ons.zip
                                // or rhdm-${version}.DM-redhat-${buildDate}-add-ons.zip depending on DM versin
                                reAddComment(controllerFile, "target: \"add_ons_distribution.zip\"",
                                        String.format("  # %s", controllerFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    // Prepare controller changes - envs
                    controller.getEnvs().stream().forEach(env -> {
                        if (env.getName().equals("CONTROLLER_DISTRIBUTION_ZIP")) {
                            // rhdm-${shortenedVersion}-controller-ee7.zip
                            String controllerEE7Zip = String.format("rhdm-%s-controller-ee7.zip", cacherProperties.shortenedVersion(version));
                            // if the filename does not match the current shortened version, update it
                            if (!env.getValue().equals(controllerEE7Zip)) {
                                env.setValue(controllerEE7Zip);
                            }
                        }
                    });

                    // Prepare Decision Central changes
                    decisionCentral.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("DECISION_CENTRAL_DISTRIBUTION_ZIP")) {
                            String decisionCentralFileName = String.format("rhdm-%s.redhat-%s-decision-central-eap7-deployable.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                decisionCentralFileName = String.format("rhdm-%s.DM-redhat-%s-decision-central-eap7-deployable.zip", version, buildDate);
                            }
                            try {
                                String decisionCentralCheckSum = elements.get(decisionCentralFileName).getChecksum();

                                log.fine(String.format("Updating RHDM Decision Central from [%s] to [%s]", artifact.getMd5(), decisionCentralCheckSum));
                                artifact.setMd5(decisionCentralCheckSum);
                                yamlFilesHelper.writeModule(decisionCentral, decisionCentralFile);

                                // find target: "decision_central_distribution.zip"
                                // and add comment on next line :  rhdm-${version}.redhat-${buildDate}-decision-central-eap7-deployable.zip
                                // or rhdm-${version}.DM-redhat-${buildDate}-decision-central-eap7-deployable.zip depending on DM version
                                reAddComment(decisionCentralFile, "target: \"decision_central_distribution.zip\"",
                                        String.format("  # %s", decisionCentralFileName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Prepare kieserver changes
                    kieserver.getArtifacts().stream().forEach(artifact -> {
                        if (artifact.getName().equals("KIE_SERVER_DISTRIBUTION_ZIP")) {
                            String kieserverFileName = String.format("rhdm-%s.redhat-%s-kie-server-ee8.zip", version, buildDate);
                            if (!version.equals("7.8.0") && !version.equals("7.9.0")) {
                                kieserverFileName = String.format("rhdm-%s.DM-redhat-%s-kie-server-ee8.zip", version, buildDate);
                            }
                            String kieserverCheckSum;
                            try {
                                kieserverCheckSum = elements.get(kieserverFileName).getChecksum();

                                log.fine(String.format("Updating RHDM Decision Central from [%s] to [%s]", artifact.getMd5(), kieserverCheckSum));
                                artifact.setMd5(kieserverCheckSum);
                                yamlFilesHelper.writeModule(kieserver, kieserverFile);

                                // find target: "kie_server_distribution.zip"
                                // and add comment on next line :  rhdm-${version}.redhat-${buildDate}-kie-server-ee8.zip
                                // or rhdm-${version}.DM-redhat-${buildDate}-kie-server-ee8.zip depending on DM version
                                reAddComment(kieserverFile, "target: \"kie_server_distribution.zip\"",
                                        String.format("  # %s", kieserverFileName));

                                // find target: "slf4j-simple.jar"
                                // and add comment on next line :  slf4j-simple-1.7.22.redhat-2.jar
                                reAddComment(kieserverFile, "target: \"slf4j-simple.jar\"", "  # slf4j-simple-1.7.22.redhat-2.jar");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    if (gitRepository.addChanges("rhdm-7-image")
                            && gitRepository.commitChanges("rhdm-7-image", branchName, "Applying RHDM nightly build for build date " + buildDate)) {

                        log.fine("About to send Pull Request on rhdm-7-image git repository on branch " + buildDate);

                        String prTittle = "Updating RHDM artifacts based on the latest nightly build  " + buildDate;
                        String prDescription = "This PR was created automatically, please review carefully before merge, the" +
                                " base build date is " + buildDate;
                        pullRequestSender.performPullRequest("rhdm-7-image", baseBranch, branchName, prTittle, prDescription);

                        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, branchName, null, "rhdm-7-image");
                    } else {
                        log.warning("something went wrong while preparing the rhdm-7-image for the pull request");
                    }

                    // remove RHDM files elements
                    removeItems("rhdm");
                }
            } else {
                log.info("File " + fileName + " not found on the elements map. ignoring...");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Re-add comments on the module.yaml file.
     *
     * @param fileName
     * @param linePattern
     * @param comment
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

    public void removeItems(String pattern) {
        log.fine("Element items are: " + Arrays.asList(elements));
        elements.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
    }

    /**
     * Expose the elements Map for test purpose
     */
    public Map<String, PlainArtifact> getElements() {
        return elements;
    }

    /**
     * Verify if the elements HashMap contains all required rhpam files
     *
     * @return true if the files are ready or false if its not ready
     */
    private boolean isRhpamReadyForPR() {
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
     *
     * @return true if the files are ready or false if its not ready
     */
    private boolean isRhdmReadyForPR() {
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
}
