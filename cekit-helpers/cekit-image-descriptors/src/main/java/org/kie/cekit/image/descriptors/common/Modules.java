/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.cekit.image.descriptors.common;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.kie.cekit.image.descriptors.image.Install;
import org.kie.cekit.image.descriptors.image.Repository;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "repositories",
        "install"
})
@RegisterForReflection
public class Modules {

    @JsonProperty("repositories")
    private List<Repository> repositories;
    @JsonProperty("install")
    private List<Install> install;

    public Modules(){}

    @JsonProperty("repositories")
    public List<Repository> getRepositories() {
        return repositories;
    }

    @JsonProperty("repositories")
    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }

    @JsonProperty("install")
    public List<Install> getInstall() {
        return install;
    }

    @JsonProperty("install")
    public void setInstall(List<Install> install) {
        this.install = install;
    }

}
