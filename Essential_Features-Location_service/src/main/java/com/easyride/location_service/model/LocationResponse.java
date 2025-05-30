package com.easyride.location_service.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationResponse {

    private List<Result> results;
    private String status;

    // Getter和Setter方法

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 内部类Result
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String formatted_address;
        // 添加其他字段
        // 例如：
        // private Geometry geometry;

        // Getter和Setter方法

        public String getFormatted_address() {
            return formatted_address;
        }

        public void setFormatted_address(String formatted_address) {
            this.formatted_address = formatted_address;
        }

        // 其他字段的Getter和Setter方法
    }
}
