package org.kie.cekit.image.descriptors.image;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "content_sets_file",
        "install"
})
@RegisterForReflection
public class Packages {

    @JsonProperty("manager")
    private String manager;
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
