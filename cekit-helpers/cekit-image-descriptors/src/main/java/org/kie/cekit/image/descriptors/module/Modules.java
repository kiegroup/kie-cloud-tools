package org.kie.cekit.image.descriptors.module;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.kie.cekit.image.descriptors.common.Env;
import org.kie.cekit.image.descriptors.common.Label;
import org.kie.cekit.image.descriptors.common.Port;
import org.kie.cekit.image.descriptors.common.Run;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schemaVersion",
        "name",
        "version",
        "description",
        "labels",
        "envs",
        "ports",
        "artifacts",
        "run",
        "execute"
})
@RegisterForReflection
public class Modules {

    @JsonProperty("schema_version")
    private Integer schemaVersion;
    private String name;
    private String version;
    private String description;
    private List<Label> labels;
    private List<Env> envs;
    private List<Port> ports;
    private List<Artifact> artifacts;
    private Run run;
    private List<Execute> execute;

    public Modules(){}

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public List<Env> getEnvs() {
        return envs;
    }

    public void setEnvs(List<Env> envs) {
        this.envs = envs;
    }

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public Run getRun() {
        return run;
    }

    public void setRun(Run run) {
        this.run = run;
    }

    public List<Execute> getExecute() {
        return execute;
    }

    public void setExecute(List<Execute> execute) {
        this.execute = execute;
    }

    @Override
    public String toString() {
        return "Modules{" +
                "schemaVersion=" + schemaVersion +
                ", name='" + name +
                ", description='" + description +
                ", labels=" + labels.toString() +
                ", envs=" + envs.toString() +
                ", artifacts=" + artifacts.toString() +
                ", run=" + run.toString() +
                ", execute=" + execute.toString() +
                '}';
    }
}