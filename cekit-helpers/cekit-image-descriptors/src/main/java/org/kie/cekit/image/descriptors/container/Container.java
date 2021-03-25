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

package org.kie.cekit.image.descriptors.container;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "platforms",
        "compose"
})
@RegisterForReflection
public class Container {

    @JsonProperty("platforms")
    private Platforms platforms;
    @JsonProperty("compose")
    private Compose compose;

    public Container(){}

    @JsonProperty("platforms")
    public Platforms getPlatforms() {
        return platforms;
    }

    @JsonProperty("platforms")
    public void setPlatforms(Platforms platforms) {
        this.platforms = platforms;
    }

    @JsonProperty("compose")
    public Compose getCompose() {
        return compose;
    }

    @JsonProperty("compose")
    public void setCompose(Compose compose) {
        this.compose = compose;
    }

}