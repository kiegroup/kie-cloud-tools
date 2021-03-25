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

package org.kie.cekit.image.descriptors.packages;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "manager",
        "repositories",
        "content_sets_file",
        "install"
})
@RegisterForReflection
public class Packages {

    @JsonProperty("manager")
    private String manager;
    @JsonProperty("repositories")
    private List<Repository> repositories = null;
    @JsonProperty("content_sets_file")
    private String contentSetsFile;
    @JsonProperty("install")
    private List<String> install = null;

    public Packages(){}

    @JsonProperty("manager")
    public String getManager() {
        return manager;
    }

    @JsonProperty("manager")
    public void setManager(String manager) {
        this.manager = manager;
    }

    @JsonProperty("content_sets_file")
    public String getContentSetsFile() {
        return contentSetsFile;
    }

    @JsonProperty("content_sets_file")
    public void setContentSetsFile(String contentSetsFile) {
        this.contentSetsFile = contentSetsFile;
    }

    @JsonProperty("install")
    public List<String> getInstall() {
        return install;
    }

    @JsonProperty("install")
    public void setInstall(List<String> install) {
        this.install = install;
    }

}
