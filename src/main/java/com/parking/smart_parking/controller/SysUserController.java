package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.SysRole;
import com.parking.smart_parking.entity.SysUser;
import com.parking.smart_parking.mapper.SysRoleMapper;
import com.parking.smart_parking.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 【用户管理控制器】SysUserController
 *
 * 管理 sys_user 表，每条记录对应一个可登录系统的管理员账号。
 *
 * 提供接口：
 *   GET    /api/user/list              查询用户列表（可按账号模糊搜索）
 *   GET    /api/user/roles             查询所有可选角色（用于下拉框）
 *   POST   /api/user/add               新增管理员
 *   POST   /api/user/update            编辑管理员信息
 *   DELETE /api/user/delete/{id}       删除管理员（id=1 超管不可删）
 *   POST   /api/user/updatePassword    用户自己修改密码（需验证旧密码）
 *   POST   /api/user/resetPassword     超管强制重置任意用户密码（不需旧密码）
 *   GET    /api/user/detail/{id}       查询单个用户详情
 *
 * 用户状态（status）：
 *   1 = 正常（可以登录）
 *   0 = 停用（LoginController 里会拒绝登录，返回 403）
 *
 * 密码存储：
 *   当前直接存明文密码（开发阶段简化处理）。
 *   生产环境建议改为 BCrypt 哈希存储：
 *     存储：BCryptPasswordEncoder.encode(rawPassword)
 *     验证：BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
 */
@RestController
@RequestMapping("/api/user")
public class SysUserController {

    /**
     * 直接注入 Mapper（用户管理逻辑简单，不需要 Service 层封装）
     */
    @Autowired
    private SysUserMapper userMapper;

    /**
     * 注入角色 Mapper（查询可选角色列表 + 编辑用户时验证角色）
     */
    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 【查询用户列表】GET /api/user/list?username=xxx&realName=xxx&phone=xxx&role=xxx
     *
     * 支持多条件组合查询：
     *   - username：按账号模糊搜索
     *   - realName：按联系人模糊搜索
     *   - phone：按联系电话模糊搜索
     *   - role：按角色编码精确匹配
     *
     * 结果按 id 倒序（最新创建的排在最前面）。
     * 注意：返回的数据包含 password 字段（明文），
     * 生产环境应在实体类加 @JsonIgnore 注解，防止密码泄露到前端。
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String role) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        if (username != null && !username.trim().isEmpty()) {
            // like 模糊匹配：输入 "adm" 可以查到 "admin"
            query.like("username", username.trim());
        }
        if (realName != null && !realName.trim().isEmpty()) {
            query.like("real_name", realName.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            query.like("phone", phone.trim());
        }
        if (role != null && !role.trim().isEmpty()) {
            query.eq("role", role.trim());
        }
        query.orderByDesc("id"); // 最新创建的用户排前面
        return Result.success("查询成功", userMapper.selectList(query));
    }

    /**
     * 【查询可选角色列表】GET /api/user/roles
     *
     * 新增/编辑用户时，角色下拉框的数据来源。
     * 按 id 升序，超管角色（id=1）排第一。
     * 返回完整角色对象（含 id、roleName、roleCode），
     * 前端 onRoleChange 里通过选择的 roleId 反查 roleCode 存入用户对象。
     */
    @GetMapping("/roles")
    public Result<?> roles() {
        QueryWrapper<SysRole> q = new QueryWrapper<>();
        q.orderByAsc("id");
        List<SysRole> list = roleMapper.selectList(q);
        return Result.success("查询成功", list);
    }

    /**
     * 【新增管理员】POST /api/user/add
     *
     * 校验流程：
     *   1. 账号和密码不能为空
     *   2. 账号不能重复（查库校验，数据库没有加唯一索引，在代码层保证）
     *   3. 角色默认 ADMIN，roleId 默认 2（若前端未传）
     *   4. 新账号默认 status=1（正常可登录）
     *
     * @RequestBody SysUser：Spring 把前端 JSON 自动反序列化为 SysUser 对象
     */
    @OperationLog(module = "用户管理", action = "新增", description = "新增管理员")
    @PostMapping("/add")
    public Result<?> add(@RequestBody SysUser user) {
        // 参数校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty())
            return Result.error(400, "用户名不能为空");
        if (user.getPassword() == null || user.getPassword().trim().isEmpty())
            return Result.error(400, "密码不能为空");

        // 账号唯一性校验：查数据库是否已存在同名账号
        SysUser exist = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", user.getUsername().trim()));
        if (exist != null) return Result.error(400, "用户名已存在");

        // 数据规范化，密码使用 BCrypt 加密存储
        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));

        // 角色默认值（前端未传时使用）
        if (user.getRole()   == null || user.getRole().trim().isEmpty())
            user.setRole("ADMIN");
        if (user.getRoleId() == null)
            user.setRoleId(2L); // id=2 通常是普通管理员角色

        user.setStatus(1); // 新用户默认正常状态，可直接登录

        userMapper.insert(user);
        return Result.success("添加成功");
    }

    /**
     * 【编辑管理员信息】POST /api/user/update
     *
     * 可修改字段：真实姓名（realName）、手机号（phone）、角色（role/roleId）、状态（status）。
     * 不可修改字段：账号（username）、密码（password）。
     *   - 账号是登录凭证，修改可能导致正在登录的用户失效
     *   - 密码修改走专用接口（updatePassword / resetPassword），有额外安全校验
     *
     * 实现方式：先从数据库查出原对象，再逐字段覆盖，最后更新。
     * 好处：不会误把未传字段清空（如传了 realName 但没传 phone，phone 不会变成 null）。
     */
    @OperationLog(module = "用户管理", action = "编辑", description = "编辑管理员信息")
    @PostMapping("/update")
    public Result<?> update(@RequestBody SysUser user) {
        if (user.getId() == null) return Result.error(400, "用户ID不能为空");

        // 从数据库取出原对象（防止前端漏传字段导致数据被清空）
        SysUser db = userMapper.selectById(user.getId());
        if (db == null) return Result.error(400, "用户不存在");

        // 只更新允许修改的字段
        db.setRealName(user.getRealName()); // 真实姓名（顶部欢迎语显示用）
        db.setPhone(user.getPhone());       // 联系电话
        db.setStatus(user.getStatus());     // 账号状态（0=停用，1=正常）

        // 角色更新（roleCode 和 roleId 要同步修改，保持一致）
        if (user.getRole() != null && !user.getRole().trim().isEmpty())
            db.setRole(user.getRole().trim());
        if (user.getRoleId() != null)
            db.setRoleId(user.getRoleId());

        userMapper.updateById(db);
        return Result.success("修改成功");
    }

    /**
     * 【删除管理员】DELETE /api/user/delete/{id}
     *
     * 保护规则：id=1 是系统超级管理员，禁止删除。
     * 删除后该账号立即无法登录（LoginController 查不到用户会返回"用户不存在"）。
     *
     * @param id 用户主键 ID（从 URL 路径取）
     */
    @OperationLog(module = "用户管理", action = "删除", description = "删除管理员")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // 保护超管账号，防止误删导致系统无法登录
        if (id == 1) return Result.error(400, "默认超级管理员不可删除");

        return userMapper.deleteById(id) > 0
                ? Result.success("删除成功")
                : Result.error(500, "删除失败");
    }

    /**
     * 【用户自己修改密码】POST /api/user/updatePassword
     *
     * 需要验证旧密码，确保是本人操作（而不是别人偷用已登录的浏览器修改）。
     * 修改成功后前端会清除 localStorage 并跳转到登录页，强制重新登录。
     *
     * 安全提示：当前是明文比对，生产环境应改为 BCrypt 哈希比对：
     *   encoder.matches(oldPwd, db.getPassword())
     *
     * @param map 包含 username（账号）、oldPwd（旧密码）、newPwd（新密码）
     */
    @PostMapping("/updatePassword")
    public Result<?> updatePassword(@RequestBody Map<String, String> map) {
        String username = map.get("username");
        String oldPwd   = map.get("oldPwd");
        String newPwd   = map.get("newPwd");

        // 参数校验
        if (username == null || username.trim().isEmpty())
            return Result.error(400, "用户名不能为空");
        if (oldPwd   == null || oldPwd.trim().isEmpty())
            return Result.error(400, "原密码不能为空");
        if (newPwd   == null || newPwd.trim().isEmpty())
            return Result.error(400, "新密码不能为空");

        // 查用户
        SysUser user = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", username.trim()));
        if (user == null) return Result.error(400, "用户不存在");

        // 验证旧密码（BCrypt 比对）
        if (!passwordEncoder.matches(oldPwd.trim(), user.getPassword()))
            return Result.error(400, "原密码不正确");

        // 更新为新密码
        user.setPassword(passwordEncoder.encode(newPwd.trim()));
        userMapper.updateById(user);
        return Result.success("密码修改成功");
    }

    /**
     * 【超管强制重置密码】POST /api/user/resetPassword
     *
     * 与 updatePassword 的区别：
     *   updatePassword：用户自己改，需要提供旧密码验证身份
     *   resetPassword：超管帮别人改，不需要旧密码（有管理员权限就能操作）
     *
     * 使用场景：员工忘记密码，超管帮其重置为临时密码，员工再自行修改。
     *
     * 校验：新密码长度不能少于 6 位（最基本的密码复杂度要求）。
     *
     * @param map 包含 userId（目标用户 ID）、newPwd（新密码）
     */
    @OperationLog(module = "用户管理", action = "编辑", description = "超管重置用户密码")
    @PostMapping("/resetPassword")
    public Result<?> resetPassword(@RequestBody Map<String, String> map) {
        String userIdStr = map.get("userId");
        String newPwd    = map.get("newPwd");

        if (userIdStr == null || userIdStr.trim().isEmpty())
            return Result.error(400, "用户ID不能为空");
        if (newPwd    == null || newPwd.trim().isEmpty())
            return Result.error(400, "新密码不能为空");
        if (newPwd.trim().length() < 6)
            return Result.error(400, "新密码长度不能少于 6 位");

        // Long.parseLong() 把字符串转成 Long 类型，作为主键查询
        SysUser user = userMapper.selectById(Long.parseLong(userIdStr.trim()));
        if (user == null) return Result.error(400, "用户不存在");

        user.setPassword(passwordEncoder.encode(newPwd.trim()));
        userMapper.updateById(user);
        return Result.success("密码重置成功");
    }

    /**
     * 【查询单个用户详情】GET /api/user/detail/{id}
     *
     * 根据主键 ID 查单条记录，用于详情展示或编辑回显。
     */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        return user == null
                ? Result.error(400, "用户不存在")
                : Result.success("查询成功", user);
    }
}
