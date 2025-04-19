package com.yingzi.nacos.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenApiDoc {

    @JsonProperty("paths")
    private Map<String, PathItem> paths;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PathItem {
        @JsonProperty("get")
        private Operation getOperation;
        public Operation operation() {
            if (getOperation != null) {
                return getOperation;
            }
            return null;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Operation {
            @JsonProperty("operationId")
            private String methodName;
            @JsonProperty("summary")
            private String description;
            @JsonProperty("parameters")
            private List<Parameter> parameters;

            public String getMethodName() {
                return methodName;
            }

            public void setMethodName(String methodName) {
                this.methodName = methodName;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public List<Parameter> getParameters() {
                return parameters;
            }

            public void setParameters(List<Parameter> parameters) {
                this.parameters = parameters;
            }
        }

        public Operation getGetOperation() {
            return getOperation;
        }

        public void setGetOperation(Operation getOperation) {
            this.getOperation = getOperation;
        }
    }

    public Map<String, PathItem> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, PathItem> paths) {
        this.paths = paths;
    }
}