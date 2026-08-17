package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.SysRole;
import com.parking.smart_parking.mapper.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 【角色管理控制器】SysRoleController
 *
 * 管理 sys_role 表，每个用户（sys_user）关联一个角色，
 * 角色决定了该用户能看到哪些菜单、有没有编辑权限。
 *
 * 提供接口：
 *   GET    /api/role/list            查询所有角色列表
 *   POST   /api/role/add             新增角色
 *   POST   /api/role/update          编辑角色（含权限配置）
 *   DELETE /api/role/delete/{id}     删除角色（有保护：有用户使用时不允许删）
 *   GET    /api/role/detail/{id}     查询单个角色
 *   GET    /api/role/permissions     查询指定角色编码的权限配置（Layout.vue 启动时调用）
 *
 * 权限字符串格式（存入 sys_role.permissions 字段）：
 *   新版（推荐）："key1:mode,key2:mode,..."
 *     每个菜单单独配置 edit=可编辑 或 read=只读
 *     例如："park-lot:edit,monthly-car:read,park-lot-map:read"
 *   旧版（兼容）："mode|key1,key2,..."
 *     竖线前是全局 mode，竖线后是所有授权菜单 key
 *     例如："edit|park-lot,monthly-car"
 *   SUPER_ADMIN 不存权限字符串（permissions=null），代码中直接给全部权限
 */
@RestController
@RequestMapping("/api/role")
public class SysRoleController {

    /**
     * 所有可配置菜单的 key 列表（超管默认拥有全部，普通管理员按配置）
     * 与前端 SysRole.vue 的 PERM_OPTIONS 保持一致
     */
    private static final List<String> ALL_KEYS = Arrays.asList(
            "park-lot", "park-lot-map", "monthly-car", "plate-recognize",
            "record", "payment", "user", "role"
    );

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 【查询角色列表】GET /api/role/list?roleName=xxx
     *
     * 可选按角色名称模糊查询，按 id 升序返回所有角色（超管在最前，自定义角色在后）。
     * 前端用户管理页的"角色"下拉选择框用此接口。
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String roleName) {
        QueryWrapper<SysRole> q = new QueryWrapper<>();
        if (roleName != null && !roleName.trim().isEmpty()) {
            q.like("role_name", roleName.trim());
        }
        q.orderByAsc("id");
        return Result.success("查询成功", roleMapper.selectList(q));
    }

    /**
     * 【新增角色】POST /api/role/add
     *
     * 校验：角色名和角色编码不能为空，编码不能重复（唯一约束在代码层实现）。
     * 编码全大写（如 ADMIN、MANAGER），由前端传入，保存后不可修改（已关联用户）。
     */
    @OperationLog(module = "角色管理", action = "新增", description = "新增角色")
    @PostMapping("/add")
    public Result<?> add(@RequestBody SysRole role) {
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty())
            return Result.error(400, "角色名称不能为空");
        if (role.getRoleCode() == null || role.getRoleCode().trim().isEmpty())
            return Result.error(400, "角色编码不能为空");

        // 检查编码唯一性（数据库没有加唯一索引，在代码层做校验）
        SysRole exist = roleMapper.selectOne(
                new QueryWrapper<SysRole>().eq("role_code", role.getRoleCode().trim()));
        if (exist != null) return Result.error(400, "角色编码已存在");

        roleMapper.insert(role);
        return Result.success("添加成功");
    }

    /**
     * 【编辑角色】POST /api/role/update
     *
     * 可修改：角色名称、角色编码、备注、权限配置（permissions 字符串）。
     * 编辑时从数据库取出原对象再逐字段覆盖，避免误把其他字段清空。
     *
     * 权限配置（permissions）由前端 SysRole.vue 组装成字符串传过来，
     * 新版格式如："park-lot:edit,monthly-car:read"，后端直接存入数据库，不做解析。
     */
    @CacheEvict(value = "role", key = "'permissions:' + #role.getRoleCode()")
    @OperationLog(module = "角色管理", action = "编辑", description = "编辑角色权限")
    @PostMapping("/update")
    public Result<?> update(@RequestBody SysRole role) {
        if (role.getId() == null) return Result.error(400, "角色ID不能为空");

        SysRole db = roleMapper.selectById(role.getId());
        if (db == null) return Result.error(400, "角色不存在");

        // 逐字段更新（只更新有变化的字段，不影响 createTime 等其他字段）
        db.setRoleName(role.getRoleName());
        db.setRoleCode(role.getRoleCode());
        db.setRemark(role.getRemark());
        db.setPermissions(role.getPermissions()); // 前端组装好的权限字符串，直接存

        roleMapper.updateById(db);
        return Result.success("修改成功");
    }

    /**
     * 注入用户 Mapper（删除角色时需要检查是否有用户在使用该角色）
     */
    @Autowired
    private com.parking.smart_parking.mapper.SysUserMapper userMapper;

    /**
     * 【删除角色】DELETE /api/role/delete/{id}
     *
     * 两个保护机制：
     *   1. id=1 或 id=2 是系统内置角色（超级管理员、默认管理员），禁止删除
     *   2. 如果该角色下还有用户，提示先修改用户角色，防止"孤儿用户"（没有角色的用户）
     *
     * 答辩可能被问：为什么不直接级联删除用户？
     *   答：角色和用户是多对一关系，删角色同时删用户风险太大（可能误删重要账号）。
     *       强制要求先手动改用户角色，操作更安全、更受控。
     */
    @CacheEvict(value = "role", allEntries = true)
    @OperationLog(module = "角色管理", action = "删除", description = "删除角色")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // 保护内置角色（id=1 超管，id=2 默认管理员）
        if (id == 1 || id == 2) return Result.error(400, "默认角色不可删除");

        SysRole role = roleMapper.selectById(id);
        if (role == null) return Result.error(400, "角色不存在");

        // 检查是否有用户正在使用此角色（查 sys_user 表的 role_id 字段）
        Long count = userMapper.selectCount(
                new QueryWrapper<com.parking.smart_parking.entity.SysUser>()
                        .eq("role_id", id));
        if (count > 0) {
            return Result.error(400,
                    "该角色下还有 " + count + " 个用户，请先修改这些用户的角色后再删除");
        }

        return roleMapper.deleteById(id) > 0
                ? Result.success("删除成功")
                : Result.error(500, "删除失败");
    }

    /**
     * 【查询角色详情】GET /api/role/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        SysRole role = roleMapper.selectById(id);
        return role == null
                ? Result.error(400, "角色不存在")
                : Result.success("查询成功", role);
    }

    /**
     * 【查询角色权限配置】GET /api/role/permissions?roleCode=ADMIN
     *
     * Layout.vue 在组件挂载时调用此接口，获取当前登录用户的菜单权限。
     *
     * 解析逻辑：
     *   SUPER_ADMIN：直接返回全部 key 和 edit 模式，不查数据库
     *   普通角色：从 permissions 字段解析
     *     新版格式："key1:mode,key2:mode,..."  每个菜单独立配置 edit/read
     *     旧版格式："mode|key1,key2,..."      全局 mode 应用到所有 key
     *     如果没有配置权限（null 或空）：返回 edit 模式 + 空 key 列表
     *
     * 返回格式：
     *   {
     *     mode: "edit",
     *     keys: ["park-lot", "monthly-car"],
     *     perms: { "park-lot": "edit", "monthly-car": "read" }
     *   }
     *
     * 缓存说明：
     *   权限配置变更极少，使用 @Cacheable 缓存到 Redis，key = "role::permissions:ADMIN"。
     *   修改或删除角色时通过 @CacheEvict 主动失效缓存，保证数据一致性。
     *
     * @param roleCode 角色编码，从前端 localStorage 取（登录时存入）
     */
    @Cacheable(value = "role", key = "'permissions:' + #roleCode")
    @GetMapping("/permissions")
    public Result<?> permissions(@RequestParam String roleCode) {
        Map<String, Object> result = new HashMap<>();

        // 超管：固定全部权限，不查数据库
        if ("SUPER_ADMIN".equals(roleCode)) {
            Map<String, String> perms = new HashMap<>();
            for (String key : ALL_KEYS) {
                perms.put(key, "edit");
            }
            result.put("mode", "edit");
            result.put("keys", ALL_KEYS);
            result.put("perms", perms);
            return Result.success("查询成功", result);
        }

        // 查该角色的权限配置
        SysRole role = roleMapper.selectOne(
                new QueryWrapper<SysRole>().eq("role_code", roleCode));

        if (role == null || role.getPermissions() == null
                || role.getPermissions().trim().isEmpty()) {
            // 没有配置权限：默认编辑模式但没有任何菜单权限
            result.put("mode", "edit");
            result.put("keys", Collections.emptyList());
            result.put("perms", Collections.emptyMap());
            return Result.success("查询成功", result);
        }

        String raw = role.getPermissions().trim();

        // 旧版兼容："mode|key1,key2,..."
        if (raw.contains("|")) {
            String[] parts = raw.split("\\|", 2);
            String mode = parts[0].trim();
            String keysPart = parts.length > 1 ? parts[1].trim() : "";

            List<String> keys = new ArrayList<>();
            Map<String, String> perms = new HashMap<>();
            if (!keysPart.isEmpty()) {
                for (String k : keysPart.split(",")) {
                    String trimmed = k.trim();
                    if (!trimmed.isEmpty()) {
                        keys.add(trimmed);
                        perms.put(trimmed, mode);
                    }
                }
            }
            result.put("mode", mode);
            result.put("keys", keys);
            result.put("perms", perms);
            return Result.success("查询成功", result);
        }

        // 新版格式："key1:mode,key2:mode,..."
        List<String> keys = new ArrayList<>();
        Map<String, String> perms = new HashMap<>();
        String defaultMode = "edit";

        for (String item : raw.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.contains(":")) {
                String[] kv = trimmed.split(":", 2);
                String key = kv[0].trim();
                String mode = kv[1].trim();
                if (!key.isEmpty()) {
                    keys.add(key);
                    perms.put(key, "edit".equals(mode) || "read".equals(mode) ? mode : "edit");
                }
            } else {
                // 没有冒号，按 key 处理，默认 edit
                keys.add(trimmed);
                perms.put(trimmed, "edit");
            }
        }

        result.put("mode", defaultMode);
        result.put("keys", keys);
        result.put("perms", perms);
        return Result.success("查询成功", result);
    }
}
