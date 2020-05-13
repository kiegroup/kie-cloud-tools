package org.kie.cekit.image.descriptors.common;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "user",
        "cmd",
        "workdir"
})
@RegisterForReflection
public class Run {

    @JsonProperty("user")
    private Integer user;
    @JsonProperty("cmd")
    private List<String> cmd;
    @JsonProperty("workdir")
    private String workdir;

    public Run(){}

    @JsonProperty("user")
    public Integer getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(Integer user) {
        this.user = user;
    }

    @JsonProperty("cmd")
    public List<String> getCmd() {
        return cmd;
    }

    @JsonProperty("cmd")
    public void setCmd(List<String> cmd) {
        this.cmd = cmd;
    }

    public String getWorkdir() {
        return workdir;
    }

    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }

    @Override
    public String toString() {
        return "Run{" +
                "user=" + user +
                ", cmd=" + cmd +
                ", workdir='" + workdir + '\'' +
                '}';
    }
}