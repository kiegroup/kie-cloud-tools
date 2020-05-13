package org.kie.cekit.image.descriptors.common;

import io.quarkus.runtime.annotations.RegisterForReflection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "value"
})
@RegisterForReflection
public class Label {

    @JsonProperty("name")
    private String name;
    @JsonProperty("value")
    private String value;

    public Label(){}

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Label{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}