package com.yingzi.nacos.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yingzi
 * @date 2025/4/6:15:59
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter {
    @JsonProperty("name")
    String parameteNname;
    @JsonProperty("description")
    String description;
    @JsonProperty("required")
    boolean required;

    @JsonProperty("schema")
    Schema schema;

    public String getParameteNname() {
        return parameteNname;
    }

    public void setParameteNname(String parameteNname) {
        this.parameteNname = parameteNname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public static class Schema {
        @JsonProperty("type")
        String type;
        @JsonProperty("format")
        String format;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }
}
