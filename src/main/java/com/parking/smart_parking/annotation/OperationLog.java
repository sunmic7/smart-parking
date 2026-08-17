package com.parking.smart_parking.annotation;

import java.lang.annotation.*;

/**
 * 【自定义注解】@OperationLog —— AOP 日志的"触发开关"
 *
 * 这是一个自定义注解，加在需要记录操作日志的 Controller 方法上。
 *
 * 【使用示例】
 *   @OperationLog(module = "停车场管理", action = "新增", description = "新增停车场")
 *   public Result<?> add(@RequestBody ParkLot parkLot) { ... }
 *
 * 工作原理
 * 注解本身只是一个"标记"，不做任何事情。
 * 真正的日志记录逻辑在 OperationLogAspect（AOP 切面）里。
 * 切面会"拦截"所有带 @OperationLog 注解的方法，在执行前后自动记录日志。
 *
 * 元注解说明（加在注解上的注解）：
 *   @Target(ElementType.METHOD)：
 *     表示 @OperationLog 只能加在方法上，加在类或字段上会编译报错。
 *   @Retention(RetentionPolicy.RUNTIME)：
 *     表示注解信息保留到运行时（JVM 运行期间）。
 *     如果是 SOURCE 或 CLASS，运行时就读不到注解了，AOP 切面就无法工作。
 *   @Documented：
 *     表示生成 JavaDoc 文档时，这个注解的信息也会被包含进去。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 功能模块名称。
     * 用于在日志表里标识这条日志属于哪个模块。
     * 例如："停车场管理"、"车辆管理"、"系统登录"、"车牌识别"
     *
     * default "" 表示不填时默认为空字符串，不是必填项。
     */
    String module() default "";

    /**
     * 操作动作。
     * 描述具体做了什么操作。
     * 例如："新增"、"编辑"、"删除"、"查询"、"入场"、"出场"、"登录"
     */
    String action() default "";

    /**
     * 操作描述。
     * 更详细的描述，会存入日志表的 description 字段。
     * 例如："车辆入场登记"、"用户登录系统"
     */
    String description() default "";
}
