package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.SysUser;
import com.parking.smart_parking.mapper.SysUserMapper;
import com.parking.smart_parking.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 【登录控制器】LoginController.java —— 用户身份验证入口
 *
 * 负责处理用户登录请求，验证账号密码后签发 JWT token。
 *
 * 【本系统的认证方案】
 * 采用"JWT 无状态认证"，不依赖 Session 和 Cookie。
 * 整个登录流程：
 *   1. 前端发送用户名 + 密码（JSON 格式）
 *   2. 后端校验账号密码是否正确
 *   3. 校验通过 → 生成 JWT token → 连同用户信息一起返回给前端
 *   4. 前端把 token 存到 localStorage，之后每次请求带上它
 *   5. 后端解析 token 验证身份，无需查数据库、无需维护 Session
 *
 * 注解说明：
 *   @RestController：相当于 @Controller + @ResponseBody，
 *                   表示这个类里所有方法的返回值都直接序列化成 JSON 响应体。
 *   @RequestMapping：不加路径，表示没有统一的前缀，
 *                   具体路径由方法上的 @PostMapping 指定。
 */
@RestController
@RequestMapping
public class LoginController {

    /**
     * 注入用户数据库操作接口。
     *
     * @Autowired：Spring 自动从容器中找到 SysUserMapper 的实现类（MyBatis-Plus 自动生成），
     * 注入进来，不需要我们手动 new。
     */
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CaptchaController captchaController;

    /**
     * 【登录接口】POST /api/auth/login
     *
     * 统一登录路径前缀为 /api/auth。
     *
     * 验证流程（按顺序）：
     *   Step 1：校验请求参数不为空
     *   Step 2：根据用户名查数据库（QueryWrapper 条件查询）
     *   Step 3：判断用户是否存在
     *   Step 4：判断账号是否被停用（status == 0 表示停用）
     *   Step 5：比对密码（明文比对，生产环境建议改为 BCrypt 加密）
     *   Step 6：所有校验通过 → 调用 JwtUtils.generateToken() 生成 token
     *   Step 7：把 token 和用户基本信息打包返回给前端
     *
     * @OperationLog 注解：
     *   触发 AOP 切面（OperationLogAspect），
     *   此次登录操作会被自动记录到 sys_operation_log 表，
     *   管理员可以在"系统日志"页面查看谁在什么时间登录了系统。
     *
     * @param loginForm 前端传来的 JSON 请求体，自动映射到 LoginForm 对象
     * @return 成功：token + 用户信息；失败：错误码 + 错误原因
     */
    @OperationLog(module = "系统登录", action = "登录", description = "用户登录系统")
    @PostMapping("/api/auth/login")
    public Result<?> login(@RequestBody LoginForm loginForm) {

        // ---- Step 1：基本参数校验 ----
        // @RequestBody 把前端 JSON 自动解析成 LoginForm 对象，但如果请求体为空则为 null
        if (loginForm == null) return Result.error(400, "请求参数不能为空");
        if (loginForm.getUsername() == null || loginForm.getUsername().trim().isEmpty())
            return Result.error(400, "用户名不能为空");
        if (loginForm.getPassword() == null || loginForm.getPassword().trim().isEmpty())
            return Result.error(400, "密码不能为空");

        // ---- Step 1.5：验证码校验 ----
        if (!captchaController.validate(loginForm.getCaptchaKey(), loginForm.getCaptchaCode())) {
            return Result.error(400, "验证码错误或已过期");
        }

        // ---- Step 2：根据用户名查询用户 ----
        // QueryWrapper 是 MyBatis-Plus 提供的条件构造器，
        // .eq("username", xxx) 相当于 SQL 的 WHERE username = 'xxx'
        // 不需要手写 SQL！
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", loginForm.getUsername().trim()); // trim() 去掉前后空格
        SysUser user = sysUserMapper.selectOne(queryWrapper);         // 查单条记录

        // ---- Step 3：用户不存在 ----
        if (user == null) return Result.error(400, "用户不存在");

        // ---- Step 4：账号被停用 ----
        // status 字段：1 = 正常，0 = 停用
        // null 检查是防止老数据没有 status 字段时空指针异常
        if (user.getStatus() != null && user.getStatus() == 0)
            return Result.error(403, "账号已被停用，请联系超级管理员");

        // ---- Step 5：密码验证（BCrypt，兼容旧明文密码）----
        String rawPassword = loginForm.getPassword().trim();
        String storedPassword = user.getPassword();
        if (isBCrypt(storedPassword)) {
            if (!passwordEncoder.matches(rawPassword, storedPassword))
                return Result.error(400, "密码错误");
        } else {
            // 旧数据是明文，验证通过后自动升级为 BCrypt
            if (!storedPassword.equals(rawPassword))
                return Result.error(400, "密码错误");
            user.setPassword(passwordEncoder.encode(rawPassword));
            sysUserMapper.updateById(user);
        }

        // ---- Step 6：生成 JWT token ----
        // 把用户ID和用户名存入 token，有效期 24 小时
        // 之后前端每次请求都带上这个 token，后端解析 token 就知道是哪个用户在操作
        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        // ---- Step 7：组装返回数据 ----
        // 用 HashMap 把 token 和用户信息打包，一起返回给前端
        // 前端会把这些信息存到 localStorage，用于页面展示（欢迎语、角色菜单控制等）
        Map<String, Object> data = new HashMap<>();
        data.put("token",    token);                 // 最重要！前端存起来，之后每次请求带上
        data.put("userId",   user.getId());          // 用户 ID
        data.put("username", user.getUsername());    // 登录账号
        data.put("realName", user.getRealName());    // 真实姓名（用于页面显示"欢迎，张三"）
        data.put("role",     user.getRole());        // 角色编码（前端用来控制菜单是否显示）
        data.put("nickname", user.getNickname());    // 昵称

        return Result.success("登录成功", data);
    }

    private boolean isBCrypt(String password) {
        return password != null && password.startsWith("$2");
    }

    /**
     * 健康检查接口（测试用）。
     * 访问 http://localhost:8080/test/ping，如果返回 "backend ok"，
     * 说明后端服务启动正常、网络可达。
     * 调试前后端连接时很有用。
     */
    @GetMapping("/test/ping")
    public String ping() {
        return "backend ok";
    }

    /**
     * 登录请求体（内部静态类）。
     *
     * 使用内部类而不是单独新建文件，是因为这个类只在登录时用到，
     * 作用域限制在 LoginController 内部，不需要暴露给外部。
     *
     * @RequestBody 注解会让 Spring 把前端发来的 JSON 字符串：
     *   { "username": "admin", "password": "123456" }
     * 自动反序列化成这个 LoginForm 对象，不需要手动解析 JSON。
     */
    public static class LoginForm {
        private String username;     // 登录账号
        private String password;     // 登录密码
        private String captchaKey;   // 验证码 key（对应 CaptchaController 缓存）
        private String captchaCode;  // 用户输入的验证码

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCaptchaKey() { return captchaKey; }
        public void setCaptchaKey(String captchaKey) { this.captchaKey = captchaKey; }
        public String getCaptchaCode() { return captchaCode; }
        public void setCaptchaCode(String captchaCode) { this.captchaCode = captchaCode; }
    }
}
