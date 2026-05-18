package com.ior.domain.vo;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Result<T> {
    @JsonProperty("code")    private Integer code;
    @JsonProperty("message") private String message;
    @JsonProperty("data")    private T data;

    // 成功（无数据）
    public static <T> Result<T> ok() {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "success";
        return result;
    }

    // 成功（带数据）
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "success";
        result.data = data;
        return result;
    }

    // 成功（自定义消息）
    public static <T> Result<T> ok(String message, T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = message;
        result.data = data;
        return result;
    }

    // 失败
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    // 常用错误码
    public static <T> Result<T> error400(String message) {
        return error(400, message);
    }

    public static <T> Result<T> error401(String message) {
        return error(401, message);
    }

    public static <T> Result<T> error403(String message) {
        return error(403, message);
    }

    public static <T> Result<T> error404(String message) {
        return error(404, message);
    }

    public static <T> Result<T> error500(String message) {
        return error(500, message);
    }
}