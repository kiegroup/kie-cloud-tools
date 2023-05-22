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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "x86_64",
        "ppc64le"
})
@RegisterForReflection
public class ContentSets {

    @JsonProperty("x86_64")
    private List<String> x8664;

    @JsonProperty("ppc64le")
    private List<String> ppc64le;

    public ContentSets(){}

    @JsonProperty("x86_64")
    public List<String> getX8664() {
        return x8664;
    }

    @JsonProperty("x86_64")
    public void setX8664(List<String> x8664) {
        this.x8664 = x8664;
    }

    @JsonProperty("ppc64le")
    public List<String> getPPC64LE() {
        return ppc64le;
    }

    @JsonProperty("ppc64le")
    public void setPPC64LE(List<String> ppc64le) {
        this.ppc64le = ppc64le;
    }
}

