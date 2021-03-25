package org.kie.cekit.image.descriptors.image;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.kie.cekit.image.descriptors.common.Env;
import org.kie.cekit.image.descriptors.common.Label;
import org.kie.cekit.image.descriptors.common.Modules;
import org.kie.cekit.image.descriptors.packages.Packages;
import org.kie.cekit.image.descriptors.common.Port;
import org.kie.cekit.image.descriptors.common.Run;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schema_version",
        "name",
        "description",
        "version",
        "from",
        "labels",
        "envs",
        "ports",
        "modules",
        "packages",
        "osbs",
        "run"
})
@RegisterForReflection
public class Image {

    @JsonProperty("schema_version")
    private Integer schemaVersion;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("version")
    private String version;
    @JsonProperty("from")
    private String from;
    @JsonProperty("labels")
    private List<Label> labels;
    @JsonProperty("envs")
    private List<Env> envs;
    @JsonProperty("ports")
    private List<Port> ports;
    @JsonProperty("modules")
    private Modules modules;
    @JsonProperty("packages")
    private Packages packages;
    @JsonProperty("osbs")
    private Osbs osbs;
    @JsonProperty("run")
    private Run run;

    public Image(){}

    @JsonProperty("schema_version")
    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    @JsonProperty("schema_version")
    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("from")
    public String getFrom() {
        return from;
    }

    @JsonProperty("from")
    public void setFrom(String from) {
        this.from = from;
    }

    @JsonProperty("labels")
    public List<Label> getLabels() {
        return labels;
    }

    @JsonProperty("labels")
    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    @JsonProperty("envs")
    public List<Env> getEnvs() {
        return envs;
    }

    @JsonProperty("envs")
    public void setEnvs(List<Env> envs) {
        this.envs = envs;
    }

    @JsonProperty("ports")
    public List<Port> getPorts() {
        return ports;
    }

    @JsonProperty("ports")
    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    @JsonProperty("modules")
    public Modules getModules() {
        return modules;
    }

    @JsonProperty("modules")
    public void setModules(Modules modules) {
        this.modules = modules;
    }

    @JsonProperty("packages")
    public Packages getPackages() {
        return packages;
    }

    @JsonProperty("packages")
    public void setPackages(Packages packages) {
        this.packages = packages;
    }

    @JsonProperty("osbs")
    public Osbs getOsbs() {
        return osbs;
    }

    @JsonProperty("osbs")
    public void setOsbs(Osbs osbs) {
        this.osbs = osbs;
    }

    @JsonProperty("run")
    public Run getRun() {
        return run;
    }

    @JsonProperty("run")
    public void setRun(Run run) {
        this.run = run;
    }

}