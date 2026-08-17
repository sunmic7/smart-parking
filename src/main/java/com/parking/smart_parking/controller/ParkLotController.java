package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.ParkLot;
import com.parking.smart_parking.entity.ParkMonthlyCar;
import com.parking.smart_parking.service.IParkLotService;
import com.parking.smart_parking.service.IParkMonthlyCarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【停车场管理控制器】ParkLotController —— 停车场的增删改查
 *
 * 提供接口：
 *   POST   /api/park-lot/add          新增停车场
 *   DELETE /api/park-lot/delete/{id}  删除停车场
 *   PUT    /api/park-lot/update       编辑停车场
 *   GET    /api/park-lot/list         查询停车场列表（支持按名称模糊搜索）
 *
 * 统一前缀：/api/park-lot
 */
@RestController
@RequestMapping("/api/park-lot")
public class ParkLotController {

    /**
     * 注入停车场 Service（提供 save、removeById、updateById、list 等方法）
     */
    @Autowired
    private IParkLotService parkLotService;

    /**
     * 注入包月车 Service（list 接口中用于实时统计包月车占用车位数）
     */
    @Autowired
    private IParkMonthlyCarService monthlyCarService;

    /**
     * 【新增停车场】POST /api/park-lot/add
     *
     * 接收前端传来的停车场信息（JSON），插入 park_lot 表。
     * 强制把 usedSpaces（已用车位）设为 0，防止前端误传非 0 值导致数据错误。
     *
     * @OperationLog 注解：此操作会被 AOP 切面自动记录进操作日志表
     * @RequestBody ParkLot：Spring 把前端 JSON 自动反序列化为 ParkLot 对象
     */
    @OperationLog(module = "停车场管理", action = "新增", description = "新增停车场")
    @PostMapping("/add")
    public Result<?> add(@RequestBody ParkLot parkLot) {
        if (parkLot == null) return Result.error(400, "请求参数不能为空");

        // 新增时已用车位必须从 0 开始，不允许前端指定（防止数据被恶意篡改）
        parkLot.setUsedSpaces(0);

        boolean success = parkLotService.save(parkLot); // 执行 INSERT 语句
        return success ? Result.success("添加成功") : Result.error(500, "添加失败");
    }

    /**
     * 【删除停车场】DELETE /api/park-lot/delete/{id}
     *
     * @PathVariable Long id：从 URL 路径中提取 id，如 /delete/5 → id=5
     *
     * 注意：删除停车场前建议先检查是否有关联的停车记录和包月车，
     * 本接口暂未做关联校验（答辩时如被问到可以说"后续迭代可以加"）。
     */
    @OperationLog(module = "停车场管理", action = "删除", description = "删除停车场")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = parkLotService.removeById(id); // 执行 DELETE 语句
        return success ? Result.success("删除成功") : Result.error(500, "删除失败");
    }

    /**
     * 【编辑停车场】PUT /api/park-lot/update
     *
     * 更新停车场信息。注意：强制把 usedSpaces 设为 null，
     * 这样 MyBatis-Plus 的 updateById 不会覆盖数据库里的已用车位数。
     * （MyBatis-Plus 默认忽略 null 字段，不会生成对应的 SET 语句）
     *
     * 为什么不能让前端更新 usedSpaces？
     * 因为已用车位是由后端入场/出场接口实时维护的，
     * 如果前端编辑时随意改这个值会导致数据不一致。
     */
    @OperationLog(module = "停车场管理", action = "编辑", description = "编辑停车场信息")
    @PutMapping("/update")
    public Result<?> update(@RequestBody ParkLot parkLot) {
        if (parkLot == null || parkLot.getId() == null)
            return Result.error(400, "请求参数不能为空");

        // 置 null：保护 usedSpaces 不被前端覆盖（MyBatis-Plus 的 updateById 会忽略 null 字段）
        parkLot.setUsedSpaces(null);

        boolean success = parkLotService.updateById(parkLot); // 执行 UPDATE 语句
        return success ? Result.success("修改成功") : Result.error(500, "修改失败");
    }

    /**
     * 【查询停车场列表】GET /api/park-lot/list?lotName=xxx
     *
     * 支持按停车场名称模糊查询（like），不传 lotName 则查全部。
     *
     * 返回值是 List<Map<String, Object>> 而不是直接返回 List<ParkLot>，
     * 原因：需要在每行里额外注入"实时已用车位数"（从包月车表实时 count），
     * 而 ParkLot 实体类字段是固定的，不方便直接塞额外数据，所以用 Map 灵活封装。
     *
     * 实时统计逻辑：
     *   对每个停车场，查包月车表中 lot_id=当前停车场 且 status=1（正常）的记录数，
     *   作为"已占用包月车位数"返回给前端展示。
     *
     * @param lotName 停车场名称关键词（可选，前端搜索框的输入值）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String lotName) {

        // 构建查询条件：有 lotName 则模糊匹配，无则查全部
        QueryWrapper<ParkLot> wrapper = new QueryWrapper<>();
        if (lotName != null && !lotName.trim().isEmpty()) {
            // like("lot_name", "万达") 生成 SQL：WHERE lot_name LIKE '%万达%'
            wrapper.like("lot_name", lotName.trim());
        }

        List<ParkLot> lots = parkLotService.list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ParkLot lot : lots) {
            // 实时统计该停车场的有效包月车数量（status=1 代表正常未过期）
            // 这比直接用 park_lot.used_spaces 字段更准确（用于展示包月车占用情况）
            QueryWrapper<ParkMonthlyCar> mq = new QueryWrapper<>();
            mq.eq("lot_id", lot.getId()).eq("status", 1);
            long usedSpaces = monthlyCarService.count(mq); // count() 执行 SELECT COUNT(*)

            // 用 HashMap 手动组装返回的字段，可以灵活控制哪些字段暴露给前端
            Map<String, Object> row = new HashMap<>();
            row.put("id",          lot.getId());
            row.put("lotName",     lot.getLotName());
            row.put("totalSpaces", lot.getTotalSpaces());
            row.put("usedSpaces",  (int) usedSpaces);    // 实时计算的已用车位数
            row.put("monthlyFee",  lot.getMonthlyFee()); // 包月费用（元/月）
            row.put("freeMinutes", lot.getFreeMinutes()); // 免费时长（分钟）
            row.put("unitMinutes", lot.getUnitMinutes()); // 计费单元（分钟，一般为60）
            row.put("unitPrice",   lot.getUnitPrice());   // 每单元单价（元）
            row.put("maxFee",      lot.getMaxFee());      // 每日封顶金额（元）
            row.put("longitude",   lot.getLongitude());   // 经度
            row.put("latitude",    lot.getLatitude());    // 纬度
            row.put("address",     lot.getAddress());     // 详细地址
            row.put("discounts",   lot.getDiscounts());   // 续费优惠规则（JSON字符串）
            row.put("createTime",  lot.getCreateTime());
            row.put("updateTime",  lot.getUpdateTime());
            result.add(row);
        }

        return Result.success("查询成功", result);
    }

    /**
     * 【更新停车场坐标】PUT /api/park-lot/update-location
     *
     * 仅更新经度、纬度、地址字段，用于地图标注。
     * 避免调用通用 update 接口时误改其他字段。
     */
    @OperationLog(module = "停车场地图", action = "标注", description = "更新停车场坐标")
    @PutMapping("/update-location")
    public Result<?> updateLocation(@RequestBody ParkLot parkLot) {
        if (parkLot == null || parkLot.getId() == null)
            return Result.error(400, "请求参数不能为空");

        ParkLot update = new ParkLot();
        update.setId(parkLot.getId());
        update.setLongitude(parkLot.getLongitude());
        update.setLatitude(parkLot.getLatitude());
        update.setAddress(parkLot.getAddress());

        boolean success = parkLotService.updateById(update);
        return success ? Result.success("坐标更新成功") : Result.error(500, "坐标更新失败");
    }
}
