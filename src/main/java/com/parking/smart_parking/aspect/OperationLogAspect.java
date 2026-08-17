package com.parking.smart_parking.aspect;

import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.entity.SysOperationLog;
import com.parking.smart_parking.mapper.SysOperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 【AOP 日志切面】OperationLogAspect.java —— 自动记录操作日志的"幕后拦截器"
 *
 * AOP（Aspect Oriented Programming，面向切面编程）是 Spring 的核心功能之一。
 *
 * 通俗理解：
 * 一家超市，每个收银台都需要"打印小票"。
 * 普通做法：在每个收银台的代码里手动写"打印小票"的逻辑 → 重复100遍。
 * AOP 做法：在收银台旁边安一个"自动打票机"，每次有人结账自动触发 → 只写一遍。
 *
 * 在本项目中：
 *   - "收银台"= 带 @OperationLog 注解的业务方法（如入场、出场、登录）
 *   - "自动打票机"= 本切面类（OperationLogAspect）
 *   - "小票"= sys_operation_log 表里的一条日志记录
 *
 * 业务代码里不需要写任何日志逻辑，只需加一个 @OperationLog 注解，
 * 切面自动在方法执行前后插入日志代码。
 *
 * 【技术原理：动态代理】
 * Spring 在启动时，会为带有切面的类生成一个"代理对象"（Proxy）。
 * 你调用 Controller 的方法时，实际上调用的是代理对象的方法，
 * 代理对象先执行切面的前置逻辑，再调用真实方法，再执行切面的后置逻辑。
 *
 * 类注解说明：
 *   @Aspect：标记这个类是一个 AOP 切面类
 *   @Component：让 Spring 把这个类注册为 Bean，才能被容器管理和生效
 */
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 注入日志表的 Mapper，用于把日志对象插入数据库。
     */
    @Autowired
    private SysOperationLogMapper logMapper;

    /**
     * 【环绕通知】@Around —— 切面的核心方法
     *
     * "环绕"的意思是：在目标方法执行的"前面"和"后面"都可以插入逻辑。
     * 类似于一个 try-catch-finally 包裹着目标方法：
     *
     *   → 进入切面（记录开始时间）
     *   → try { 执行目标方法（业务逻辑） }
     *   → catch { 记录失败状态和错误信息 }
     *   → finally { 无论成功还是失败，都写入日志到数据库 }
     *
     * 切点表达式 @annotation(com.parking.smart_parking.annotation.OperationLog)：
     * 意思是"拦截所有标注了 @OperationLog 注解的方法"。
     * 每当某个带 @OperationLog 注解的方法被调用时，都会先进入这里。
     *
     * @param point 连接点对象，可以获取目标方法的所有信息（方法名、参数等），
     *              以及调用 point.proceed() 来执行目标方法
     * @return 目标方法的返回值（透明地传递，不影响正常流程）
     * @throws Throwable 目标方法抛出的异常会继续向上传播，不被切面吞掉
     */
    @Around("@annotation(com.parking.smart_parking.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {

        // 记录方法开始执行的时间戳，用于在 finally 块里计算执行耗时
        long start = System.currentTimeMillis();

        // ---- 通过反射获取方法上的 @OperationLog 注解信息 ----
        // 反射：在程序运行时动态地读取类、方法、注解等信息（不是编译时确定）
        MethodSignature signature = (MethodSignature) point.getSignature(); // 获取方法签名
        Method method = signature.getMethod();                               // 获取方法对象
        OperationLog annotation = method.getAnnotation(OperationLog.class); // 读取注解上的值

        // 初始化日志相关字段
        String requestUrl = ""; // 请求路径，如 /api/plate/entry
        String requestIp  = ""; // 客户端 IP 地址
        String username   = ""; // 操作人账号（从请求头 X-Username 取）
        String realName   = ""; // 操作人真实姓名（从请求头 X-RealName 取）

        try {
            // ---- 从 Spring 请求上下文获取当前 HTTP 请求对象 ----
            // RequestContextHolder 是 Spring 提供的工具，可以在任意位置获取当前请求
            // 不需要在方法参数里声明 HttpServletRequest，就能拿到请求对象
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                Object req = attrs.getRequest(); // 获取 HttpServletRequest 对象

                // 获取请求路径（如 /api/plate/exit）
                requestUrl = invokeMethod(req, "getRequestURI");

                // 获取客户端真实 IP（需穿透反向代理，见 getClientIp 方法注释）
                requestIp = getClientIp(req);

                // 从请求头中获取操作人账号
                // 这个请求头是前端 request.js 的请求拦截器统一添加的：
                //   config.headers['X-Username'] = localStorage.getItem('username')
                String rawUsername = invokeHeader(req, "X-Username");
                if (rawUsername != null) username = rawUsername;

                // 从请求头中获取操作人真实姓名
                // 真实姓名可能含中文，前端用 encodeURIComponent 编码后放入请求头
                // （HTTP 请求头默认只支持 ISO-8859-1，中文会乱码）
                // 这里用 URLDecoder.decode 解码还原
                String rawRealName = invokeHeader(req, "X-RealName");
                if (rawRealName != null && !rawRealName.isEmpty()) {
                    try {
                        realName = URLDecoder.decode(rawRealName, StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        realName = rawRealName; // 解码失败则原样使用
                    }
                }
            }
        } catch (Exception ignored) {
            // 获取请求信息失败不影响业务，忽略异常即可
        }

        // ---- 构建日志对象（还没写入数据库，在 finally 里统一写入）----
        SysOperationLog log = new SysOperationLog();
        log.setUsername(username);                         // 操作人账号
        log.setRealName(realName);                         // 操作人真实姓名
        log.setModule(annotation.module());                // 注解上的 module 值，如"车牌识别"
        log.setAction(annotation.action());                // 注解上的 action 值，如"入场"
        log.setRequestUrl(requestUrl);                     // 请求路径
        log.setRequestIp(requestIp);                       // 客户端 IP
        // 记录完整的方法名，如：com.parking.xxx.PlateRecognizeController.entry
        log.setMethod(point.getTarget().getClass().getName() + "." + method.getName());
        log.setCreateTime(new Date());                     // 操作时间

        // 如果注解上有 description，就用它；否则用"模块-动作"拼接
        String desc = annotation.description();
        if (desc == null || desc.isEmpty()) {
            desc = annotation.module() + " - " + annotation.action();
        }
        log.setDescription(desc);

        try {
            // ---- 执行目标业务方法（这才是真正的业务代码！）----
            // point.proceed() 就是在这里调用被拦截的方法（如 entry()、exit()、login()）
            Object result = point.proceed();
            log.setStatus(1); // 1 = 执行成功
            return result;    // 把业务方法的返回值原样返回，切面对调用方透明

        } catch (Throwable t) {
            // 业务方法抛出异常时，记录失败状态和错误信息
            log.setStatus(0); // 0 = 执行失败
            String errMsg = t.getMessage();
            // 错误信息截断为 490 字符，防止超出数据库字段长度（一般设 500）
            log.setErrorMsg(errMsg != null && errMsg.length() > 490
                    ? errMsg.substring(0, 490) : errMsg);
            throw t; // 重新抛出异常，不能在切面里把异常吞掉，业务层需要感知到错误

        } finally {
            // ---- finally 块：无论成功还是失败，都会执行 ----
            // 计算方法执行耗时（毫秒）
            log.setCostTime((int) (System.currentTimeMillis() - start));
            try {
                // 把日志对象插入数据库（sys_operation_log 表）
                logMapper.insert(log);
            } catch (Exception e) {
                // 日志写入失败不能影响主业务，只打印异常，不向上抛出
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过反射调用 request.getHeader(headerName) 获取请求头的值。
     *
     * 为什么用反射而不直接调用？
     * Spring Boot 2.x 使用 javax.servlet 包，Spring Boot 3.x 改用 jakarta.servlet 包，
     * 两者方法签名相同但包名不同。用反射调用可以兼容两个版本，
     * 不会因为版本不同而编译失败。
     *
     * @param req        HttpServletRequest 对象
     * @param headerName 请求头名称，如 "X-Username"
     * @return 请求头的值，不存在时返回 null
     */
    private String invokeHeader(Object req, String headerName) {
        try {
            // 通过反射获取 getHeader(String) 方法并调用
            Method m = req.getClass().getMethod("getHeader", String.class);
            Object result = m.invoke(req, headerName);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过反射调用 request 的无参方法（如 getRequestURI、getRemoteAddr）。
     * 同样是为了兼容 javax/jakarta 双版本。
     *
     * @param req        HttpServletRequest 对象
     * @param methodName 方法名，如 "getRequestURI"
     * @return 方法返回值的字符串形式
     */
    private String invokeMethod(Object req, String methodName) {
        try {
            Method m = req.getClass().getMethod(methodName);
            Object result = m.invoke(req);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取客户端真实 IP 地址。
     *
     * 【为什么不直接用 request.getRemoteAddr()？】
     * 如果服务器前面有 Nginx 等反向代理，getRemoteAddr() 拿到的是
     * Nginx 的 IP，不是用户真实 IP。
     * 反向代理通常会把真实 IP 放在 X-Forwarded-For 等请求头里，
     * 所以要优先检查这些请求头。
     *
     * X-Forwarded-For 格式：真实IP, 代理1IP, 代理2IP
     * 取第一个就是真实客户端 IP。
     *
     * @param req HttpServletRequest 对象
     * @return 客户端真实 IP 地址
     */
    private String getClientIp(Object req) {
        // 按优先级依次检查这几个请求头
        String[] headers = {
                "X-Forwarded-For",      // 最常见，Nginx/负载均衡器设置
                "Proxy-Client-IP",      // Apache 代理
                "WL-Proxy-Client-IP",   // WebLogic 代理
                "HTTP_CLIENT_IP",       // 某些代理
                "X-Real-IP"             // Nginx 配置的真实IP头
        };
        for (String h : headers) {
            String ip = invokeHeader(req, h);
            // "unknown" 表示代理无法获取真实IP，跳过
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim(); // 多级代理时取第一个 IP（即真实客户端）
            }
        }
        // 以上都没有时，直接取 TCP 连接的远端地址（可能是代理IP）
        return invokeMethod(req, "getRemoteAddr");
    }
}
