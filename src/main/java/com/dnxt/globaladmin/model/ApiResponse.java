package com.dnxt.globaladmin.model;

import java.util.Map;

public class ApiResponse {

    private String status;
    private Object data;
    private String message;

    private ApiResponse() {}

    public static ApiResponse ok(Object data) {
        ApiResponse r = new ApiResponse();
        r.status = "ok";
        r.data = data;
        return r;
    }

    public static ApiResponse error(String message) {
        ApiResponse r = new ApiResponse();
        r.status = "error";
        r.message = message;
        return r;
    }

    public static ApiResponse ok() {
        return ok(Map.of());
    }

    public String getStatus() { return status; }
    public Object getData() { return data; }
    public String getMessage() { return message; }
}
