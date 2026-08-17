package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.SysOperationLog;
import com.parking.smart_parking.mapper.SysOperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【操作日志控制器】SysOperationLogController
 *
 * 管理 sys_operation_log 表，展示所有 @OperationLog 切面自动记录的操作日志。
 *
 * 提供接口：
 *   GET    /api/log/list           分页查询日志（多条件筛选）
 *   DELETE /api/log/clear          清空全部日志（仅超管）
 *   DELETE /api/log/delete/{id}    删除单条日志（仅超管）
 *
 * 与其他列表接口的区别：
 *   本接口采用【后端分页】，每次请求只查当前页的数据，
 *   而不是像停车场列表那样一次性查全部再由前端分页。
 *   原因：日志数据量大，一次全量加载会很慢，后端分页效率更高。
 *
 * 直接注入 Mapper（而不是 Service）：
 *   日志功能简单，不需要复杂业务逻辑，直接用 Mapper 操作数据库更简洁。
 *   AOP 切面（OperationLogAspect）也直接用 Mapper 写日志，保持一致。
 */
@RestController
@RequestMapping("/api/log")
public class SysOperationLogController {

    /**
     * 直接注入 Mapper（跳过 Service 层）
     * 日志操作逻辑简单（纯增删查），不需要 Service 层的业务封装
     */
    @Autowired
    private SysOperationLogMapper logMapper;

    /**
     * 【分页查询操作日志】GET /api/log/list
     *
     * 支持多条件组合筛选：
     *   page      当前页码（默认第 1 页）
     *   pageSize  每页条数（默认 20 条）
     *   username  操作人账号（模糊匹配，用 LIKE）
     *   module    功能模块（精确匹配，用 =）
     *   action    操作类型（精确匹配，用 =）
     *   status    操作结果（0=失败，1=成功，精确匹配）
     *   startTime 开始日期（格式 yyyy-MM-dd，对应 >= 当天 00:00:00）
     *   endTime   结束日期（格式 yyyy-MM-dd，对应 <= 当天 23:59:59）
     *
     * MyBatis-Plus 分页：
     *   new Page<>(page, pageSize) 创建分页对象，自动生成 LIMIT 语句（需要分页插件支持）
     *   selectPage() 返回分页结果，包含 records（当前页数据）和 total（总记录数）
     *   前端用 total 来显示"共 N 条"和控制总页数
     *
     * 结果按 create_time 倒序：最新的日志排最前面
     *
     * @param page      当前页（从 1 开始）
     * @param pageSize  每页条数
     * @param username  操作人账号（可选）
     * @param module    功能模块（可选）
     * @param action    操作类型（可选）
     * @param status    结果状态（可选，0 或 1）
     * @param startTime 开始日期字符串 yyyy-MM-dd（可选）
     * @param endTime   结束日期字符串 yyyy-MM-dd（可选）
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1")  int page,      // 不传时默认第 1 页
            @RequestParam(defaultValue = "20") int pageSize,  // 不传时默认每页 20 条
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        QueryWrapper<SysOperationLog> wrapper = new QueryWrapper<>();

        // 非空字段才加入查询条件（空字符串不传，避免误过滤）
        if (username  != null && !username.trim().isEmpty())
            wrapper.like("username", username.trim());   // 模糊：LIKE '%xxx%'
        if (module    != null && !module.trim().isEmpty())
            wrapper.eq("module",    module.trim());      // 精确：= 'xxx'
        if (action    != null && !action.trim().isEmpty())
            wrapper.eq("action",    action.trim());      // 精确：= 'xxx'
        if (status    != null)
            wrapper.eq("status",    status);             // 精确：= 0 或 = 1

        // 日期范围：在字符串末尾拼上时分秒，实现"当天整天"的范围查询
        // startTime = "2025-06-01" → CREATE_TIME >= "2025-06-01 00:00:00"
        // endTime   = "2025-06-30" → CREATE_TIME <= "2025-06-30 23:59:59"
        if (startTime != null && !startTime.trim().isEmpty())
            wrapper.ge("create_time", startTime.trim() + " 00:00:00");
        if (endTime   != null && !endTime.trim().isEmpty())
            wrapper.le("create_time", endTime.trim()   + " 23:59:59");

        wrapper.orderByDesc("create_time"); // 最新日志在最前面

        // MyBatis-Plus 分页查询：传入 Page 对象和查询条件
        // 分页插件（MybatisPlusConfig 中配置的 PaginationInnerInterceptor）
        // 会自动在 SQL 末尾加 LIMIT offset, pageSize
        Page<SysOperationLog> pageResult = logMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        // pageResult 包含：records（当前页数据列表）、total（总记录数）、pages（总页数）等
        return Result.success("查询成功", pageResult);
    }

    /**
     * 【清空全部操作日志】DELETE /api/log/clear
     *
     * 删除 sys_operation_log 表中的所有记录（慎用！）。
     * 前端做了双重确认弹窗，后端直接执行，不可恢复。
     *
     * new QueryWrapper<>()：空条件，等效于 DELETE FROM sys_operation_log（无 WHERE 条件）
     *
     * 注意：此操作本身也会被 @OperationLog 切面记录，
     * 但因为是先记录操作、再执行清空，所以这条"清空"日志会在清空前写入，
     * 之后清空操作会把它和所有旧日志一起删掉。
     */
    @OperationLog(module = "日志管理", action = "清空", description = "清空全部操作日志")
    @DeleteMapping("/clear")
    public Result<?> clear() {
        logMapper.delete(new QueryWrapper<>()); // 空 wrapper = 不加 WHERE 条件 = 全部删除
        return Result.success("日志已清空");
    }

    /**
     * 【删除单条操作日志】DELETE /api/log/delete/{id}
     *
     * deleteById() 执行 DELETE FROM sys_operation_log WHERE id = ?
     * 返回受影响行数：> 0 表示删除成功，= 0 表示没找到该 ID
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return logMapper.deleteById(id) > 0
                ? Result.success("删除成功")
                : Result.error(500, "删除失败");
    }
}
