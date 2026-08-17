package com.parking.smart_parking.common;

/**
 * 【统一响应类】Result.java
 * 项目中所有接口（Controller 方法）的返回值都是这个类的对象。
 * 它规定了前后端之间数据传输的统一格式，前端只需要判断 code 是否为 200
 * 就能知道请求是否成功，不需要针对每个接口写不同的解析逻辑。
 *
 * 返回给前端的 JSON 格式示例：
 *   成功：{ "code": 200, "message": "操作成功", "data": { ... } }
 *   失败：{ "code": 400, "message": "车牌号不能为空", "data": null }
 *
 * 泛型 <T> 的作用：让 data 字段可以装任意类型的数据，
 * 比如 Result<ParkLot>、Result<List<ParkRecord>> 等，灵活通用。
 *
 * 封装统一响应类原因
 *   统一格式方便前端统一处理，不用每个接口单独判断。
 *       如果以后要改返回格式（比如加一个 traceId 字段），只改这一个类就行。
 */
public class Result<T> {
    /**
     * 业务状态码（不是 HTTP 状态码）。
     *
     * 本项目约定：
     *   200 = 操作成功
     *   400 = 客户端请求有问题（参数错误、业务校验失败等）
     *   401 = 未登录 / Token 无效
     *   403 = 没有权限（账号被禁用等）
     *   500 = 服务器内部错误
     */
    private Integer code;
    /**
     * 提示信息。
     * 成功时如："操作成功"、"入场成功"；
     * 失败时如："车牌号不能为空"、"账号已被停用"。
     * 前端会把这个信息直接弹出来给用户看。
     */
    private String message;
    /**
     * 实际返回的数据。
     * 查询列表时是 List；查询单条时是对象；纯操作成功时是 null。
     * 使用泛型 T，可以装任意类型。
     */
    private T data;

    // 构造方法

    /** 无参构造，MyBatis-Plus 和 Jackson（JSON 序列化库）需要用到 */
    public Result() {
    }

    /** 全参构造，内部静态工厂方法会调用它 */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getter / Setter

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMessage() { return message; }

    /**
     * 兼容前端写 res.msg 的情况。
     * 前端 JavaScript 有时用 res.msg，有时用 res.message，
     * 这里两个方法都提供，Jackson 序列化时会生成两个字段，前端两种写法都能拿到值。
     */
    public String getMsg() { return message; }

    public void setMessage(String message) { this.message = message; }
    public void setMsg(String msg) { this.message = msg; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    // 静态工厂方法（最常用）

    /**
     * 操作成功，并返回数据。
     * 用法示例：return Result.success(parkLot);
     *          → { "code": 200, "message": "操作成功", "data": {...} }
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 操作成功，只返回提示信息，不返回数据（比如删除成功）。
     * 用法示例：return Result.success("删除成功");
     *          → { "code": 200, "message": "删除成功", "data": null }
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    /**
     * 操作成功，同时返回提示信息和数据（最完整的形式）。
     * 用法示例：return Result.success("登录成功", userInfo);
     *          → { "code": 200, "message": "登录成功", "data": {...} }
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 操作失败，返回错误码和错误信息。
     * 用法示例：return Result.error(400, "车牌号不能为空");
     *          → { "code": 400, "message": "车牌号不能为空", "data": null }
     *
     * @param code    错误码，建议参考 HTTP 状态码语义（400客户端错误，500服务器错误）
     * @param message 给用户看的错误描述
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
