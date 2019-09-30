/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.cloud.tests.logs;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.xtf.core.openshift.OpenShift;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.kie.cloud.tests.core.resources.Project;
import rx.Observable;
import rx.observables.StringObservable;

public class PodEntity {

    private Project project;
    private String podName;

    /**
     * @param project
     * @param podName
     */
    public PodEntity(Project project, String podName) {
        super();
        this.project = project;
        this.podName = podName;
    }

    public String getName() {
        return podName;
    }

    public boolean exists() {
        return Objects.nonNull(getOpenShift().getPod(podName));
    }

    // Section here after could be modified/deleted when https://github.com/xtf-cz/xtf/issues/296 is implemented
    // This is a fix in case many containers are present in one pod

    public Map<String, Observable<String>> observeAllContainersLogs() {
        return getContainers()
                .stream()
                .map(Container::getName)
                .collect(Collectors.toMap(Function.identity(), this::observeContainerLogs));
    }

    public Observable<String> observeContainerLogs(String containerName) {
        if (Objects.nonNull(containerName)) {
            LogWatch watcher = getOpenShift().pods().withName(podName).inContainer(containerName).watchLog();
            return StringObservable.byLine(StringObservable.from(new InputStreamReader(watcher.getOutput())));
        } else {
            return getOpenShift().observePodLog(getOpenShift().getPod(podName));
        }
    }

    public String getLogs() {
        // Get logs from first container (or null if none ?...)
        return getLogs(getContainers()
                .stream()
                .findFirst()
                .map(Container::getName)
                .orElse(null));
    }

    /**
     * Return a map (containerName/logs) of all containers logs from the pod
     * @return
     */
    public Map<String, String> getAllContainerLogs() {
        return getContainers()
                .stream()
                .map(Container::getName)
                .collect(Collectors.toMap(Function.identity(), this::getLogs));
    }

    /**
     * Return logs from a specific container of the pod
     * @param containerName
     * @return
     */
    public String getLogs(String containerName) {
        if (Objects.nonNull(containerName)) {
            return getOpenShift().pods().withName(podName).inContainer(containerName).getLog();
        } else {
            return getOpenShift().getPodLog(getOpenShift().getPod(podName));
        }
    }

    public List<Container> getContainers() {
        return Optional.ofNullable(getOpenShift().getPod(podName).getSpec())
                .map(PodSpec::getContainers)
                .orElse(new ArrayList<>());
    }

    private OpenShift getOpenShift() {
        return project.getOpenShift();
    }
}
