/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.tests.core.resources;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cz.xtf.builder.builders.ImageStreamBuilder;
import cz.xtf.core.openshift.OpenShift;
import cz.xtf.core.openshift.OpenShiftBinary;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.waiting.SimpleWaiter;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.ImageStream;
import org.kie.cloud.tests.core.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.jca.GetInstance.Instance;

import static java.util.stream.Collectors.toList;

public class Project implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Project.class);

    public static final String POD_STATUS_PENDING = "Pending";

    private String projectName;
    private OpenShift openShift;
    private OpenShift openShiftAdmin;

    private Project(String projectName) {
        this.projectName = projectName;
        this.openShift = OpenShifts.master(projectName);
        this.openShiftAdmin = OpenShifts.admin(projectName);
    }

    public static final Project getOrCreate(String projectName) {
        OpenShift openShift = OpenShifts.master();
        if (openShift.getProject(projectName) == null) {
            LOGGER.info("Create project {}", projectName);
            openShift.createProjectRequest(projectName);
            new SimpleWaiter(() -> openShift.getProject(projectName) != null)
                    .reason("Waiting for project " + projectName + " to be created.")
                    .timeout(TimeUnit.MINUTES, 5)
                    .waitFor();
        }
        return new Project(projectName);
    }

    /**
     * @return Project name.
     */
    public String getName() {
        return projectName;
    }

    /**
     * @return OpenShift client.
     */
    public OpenShift getOpenShift() {
        return openShift;
    }

    /**
     * @return OpenShift admin client.
     */
    public OpenShift getOpenShiftAdmin() {
        return openShiftAdmin;
    }

    /**
     * Delete OpenShift project.
     */
    public void delete() {
        openShift.deleteProject();
    }

    /**
     * Process template and create all resources defined there.
     *
     * @param templateUrl URL of template to be processed
     * @param envVariables Map of environment variables to override default values from the template
     */
    public void processTemplateAndCreateResources(URL templateUrl, Map<String, String> envVariables) {
        boolean templateIsFile = templateUrl.getProtocol().equals("file");

        // Used to log into OpenShift
        OpenShiftBinary oc = openShiftBinaryClient();

        List<String> commandParameters = new ArrayList<>();
        commandParameters.add(openShiftBinaryPath());
        commandParameters.add("process");
        commandParameters.add("-f");
        commandParameters.add(templateIsFile ? templateUrl.getPath() : templateUrl.toExternalForm());
        commandParameters.add("--local");
        commandParameters.add("--ignore-unknown-parameters=true");
        commandParameters.add("-o");
        commandParameters.add("yaml");
        for (Entry<String, String> envVariable : envVariables.entrySet()) {
            commandParameters.add("-p");
            commandParameters.add(envVariable.getKey() + "=" + envVariable.getValue());
        }
        String completeProcessingCommand = commandParameters.stream().collect(Collectors.joining(" "));

        try (ProcessExecutor executor = new ProcessExecutor()) {
            File processedTemplate = executor.executeProcessCommandToTempFile(completeProcessingCommand);
            oc.execute("create", "-n", getName(), "-f", processedTemplate.getAbsolutePath());
            //            openShift.load(Files.newInputStream(processedTemplate.toPath()))
            //                .;
        } catch (Exception e) {
            throw new RuntimeException("Error while processing template", e);
        }

        // TODO: Temporary workaround to wait until scenario is completely initialized as there is a delay between finishing template creation command
        // and actual creation of resources on OpenShift. This should be removed when deployments won't be scaled in the beginning and will contain availability check.
        try {
            TimeUnit.SECONDS.sleep(10L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for scenario to be initialized.", e);
        }
    }

    static Semaphore semaphore = new Semaphore(1);

    /**
     * Process APB and create all resources defined there.
     * @param image APB Image to be provision
     * @param extraVars Map of extra vars to override default values from the APB image
     */
    public synchronized void processApbRun(String image, Map<String, String> extraVars) {
        String podName = "apb-pod-" + UUID.randomUUID().toString().substring(0, 4);

        try {
            semaphore.acquire();
            OpenShiftBinary oc = openShiftBinaryClient();
            if (openShift.getServiceAccount("apb") == null) {
                oc.execute("create", "serviceaccount", "apb");
                oc.execute("create", "rolebinding", "apb", "--clusterrole=admin", "--serviceaccount=" + projectName + ":apb");
            }

            List<String> args = new ArrayList<>();
            args.add("run");
            args.add(podName);
            args.add("--env=POD_NAME=" + podName);
            args.add("--env=POD_NAMESPACE=" + projectName);
            args.add("--image=" + image);
            args.add("--restart=Never");
            args.add("--attach=true");
            args.add("--serviceaccount=apb");
            args.add("--");
            args.add("provision");
            args.add("--extra-vars");
            args.add(formatExtraVars(extraVars));

            LOGGER.info("Executing command: oc {}", getApbCommand(args));
            oc.execute(args.toArray(new String[0]));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for scenario to be initialized.", e);
        } finally {
            semaphore.release();
        }
    }

    private String getApbCommand(List<String> args) {
        return args.stream().collect(Collectors.joining(" "));
    }

    private String formatExtraVars(Map<String, String> extraVars) {
        return extraVars.entrySet()
                .stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * Create all resources defined in resource URL.
     *
     * @param resourceUrl URL of resource list to be created
     */
    public void createResources(String resourceUrl) {
        try {
            KubernetesList resourceList = openShift.lists().inNamespace(projectName).load(new URL(resourceUrl)).get();
            openShift.lists().inNamespace(projectName).create(resourceList);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed resource URL", e);
        }
    }

    /**
     * Create resources from YAML file using command line client.
     * @param yamlUrl Url to yaml file with resources
     */
    public void createResourcesFromYaml(String yamlUrl) {
        final String output = openShiftBinaryClient().execute("create", "-f", yamlUrl);
        LOGGER.info("Yaml resources from file {} were created by oc client. Output = {}", yamlUrl, output);
    }

    /**
     * Create resources from YAML files using command line client.
     * @param yamlUrls Urls to yaml files with resources
     */
    public void createResourcesFromYaml(List<String> yamlUrls) {
        final OpenShiftBinary oc = openShiftBinaryClient();
        for (String url : yamlUrls) {
            final String output = oc.execute("create", "-f", url);
            LOGGER.info("Yaml resources from file {} were created by oc client. Output = {}", url, output);
        }
    }

    /**
     * Create resources from YAML files as admin using command line client.
     * @param yamlUrl Url to yaml files with resources
     */
    public void createResourcesFromYamlAsAdmin(String yamlUrl) {
        final String output = openShiftBinaryAdminClient().execute("create", "-f", yamlUrl);
        LOGGER.info("Yaml resources from file {} were created by oc client. Output = {}", yamlUrl, output);
    }

    /**
     * Create resources from YAML files as admin using command line client.
     * @param yamlUrls Url to yaml files with resources
     */
    public void createResourcesFromYamlAsAdmin(List<String> yamlUrls) {
        final OpenShiftBinary oc = openShiftBinaryAdminClient();
        for (String url : yamlUrls) {
            final String output = oc.execute("create", "-f", url);
            LOGGER.info("Yaml resources from file {} were created by oc client. Output = {}", url, output);
        }
    }

    /**
     * Create all resources defined in resource URL.
     *
     * @param inputStream Input stream with resource list to be created
     */
    public void createResources(InputStream inputStream) {
        KubernetesList resourceList = openShift.lists().inNamespace(projectName).load(inputStream).get();
        openShift.lists().inNamespace(projectName).create(resourceList);
    }

    /**
     * Create image stream in current project.
     *
     * @param imageStreamName Name of image stream
     * @param imageTag Image tag used to resolve image,for example Docker tag.
     */
    public void createImageStream(String imageStreamName, String imageTag) {
        ImageStream driverImageStream = new ImageStreamBuilder(imageStreamName).fromExternalImage(imageTag).build();
        openShift.createImageStream(driverImageStream);
    }

    /**
     * Run oc command
     * @param args Command parameters
     * @return Output of oc
     */
    public String runOcCommand(String... args) {
        return openShiftBinaryClient().execute(args);
    }

    /**
     * Run oc command as admin
     * @param args Command parameters
     * @return Output of oc
     */
    public String runOcCommandAsAdmin(String... args) {
        return openShiftBinaryAdminClient().execute(args);
    }

    @Override
    public void close() {
        try {
            openShift.close();
        } catch (Exception e) {
            LOGGER.warn("Exception while closing OpenShift client.", e);
        }
    }

    /**
     * Return list of all scheduled instances in the project.
     *
     * @return List of Instances
     * @see Instance
     */
    public List<Pod> getAllPods() {
        return openShift
                .getPods()
                .stream()
                .filter(this::isScheduledPod)
                .collect(toList());
    }

    private boolean isScheduledPod(Pod pod) {
        return !POD_STATUS_PENDING.equals(pod.getStatus().getPhase());
    }

    private OpenShiftBinary openShiftBinaryClient() {
        return OpenShifts.masterBinary(this.getName());
    }

    private OpenShiftBinary openShiftBinaryAdminClient() {
        return OpenShifts.adminBinary(this.getName());
    }

    private String openShiftBinaryPath() {
        return OpenShifts.getBinaryPath();
    }
}
