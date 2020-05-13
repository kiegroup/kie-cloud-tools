package org.kie.cekit.image.descriptors.module;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "url",
        "target",
        "md5"
})
@RegisterForReflection
public class Artifact {

    private String name;
    private String url;
    private String target;
    private String md5;

    public Artifact() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public String toString() {
        return "Artifact{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", target='" + target + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}