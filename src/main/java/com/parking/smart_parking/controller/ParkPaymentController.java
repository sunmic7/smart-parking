package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.ParkPayment;
import com.parking.smart_parking.service.IParkPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 【缴费记录控制器】ParkPaymentController
 * 管理 park_payment 表，记录所有临停缴费和包月续费的支付情况。
 *
 * 提供接口：
 *   GET    /api/payment/list         查询缴费记录列表（可按车牌筛选）
 *   POST   /api/payment/add          手动新增缴费记录（补录用）
 *   PUT    /api/payment/pay/{id}     确认支付（把 payStatus 从 0 改为 1）
 *   DELETE /api/payment/delete/{id}  删除缴费记录
 *   GET    /api/payment/detail/{id}  查询单条缴费记录详情
 *
 * 支付状态（payStatus）：
 *   0 = 未支付（出场/续费时自动生成，等待用户点"确认支付"）
 *   1 = 已支付（用户点确认 或 手动新增时直接置 1）
 */
@RestController
@RequestMapping("/api/payment")
public class ParkPaymentController {

    @Autowired
    private IParkPaymentService paymentService;

    /**
     * 【查询缴费记录列表】GET /api/payment/list?plateNumber=xx&lotId=xx&date=2025-07-06&payStatus=1
     *
     * 支持多条件组合查询：
     *   - plateNumber：按车牌号模糊查询
     *   - lotId：按停车场精确查询
     *   - date：按支付日期查询（某一天，格式 yyyy-MM-dd）
     *   - payStatus：按支付状态查询（0=未支付，1=已支付）
     *
     * 结果按支付时间倒序排列（最新的在最前面）。
     */
    @GetMapping("/list")
    public Result<List<ParkPayment>> list(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer payStatus) {

        QueryWrapper<ParkPayment> query = new QueryWrapper<>();
        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            query.like("plate_number", plateNumber.trim());
        }
        if (lotId != null) {
            query.eq("lot_id", lotId);
        }
        if (date != null && !date.trim().isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(date.trim());
                Date start = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date end   = Date.from(localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                query.ge("pay_time", start).lt("pay_time", end);
            } catch (Exception ignored) {
                // 日期格式非法时跳过该条件，避免报错
            }
        }
        if (payStatus != null) {
            query.eq("pay_status", payStatus);
        }
        // 按 pay_time 倒序：最新的支付记录排在最前面
        query.orderByDesc("pay_time");
        return Result.success("查询成功", paymentService.list(query));
    }

    /**
     * 【手动新增缴费记录】POST /api/payment/add
     * 用于手动补录缴费（如系统异常未自动生成记录时）。
     * 手动新增的记录直接设为"已支付"（payStatus=1），时间为当前时间。
     *
     * 与自动生成的缴费记录的区别：
     *   自动生成（出场/续费时）：payStatus=0（未支付），等用户确认
     *   手动新增：payStatus=1（已支付），视为收款已完成
     *
     * 缓存说明：
     *   缴费记录变更会影响月度收入统计，因此手动新增后清除 statistics 缓存。
     */
    @CacheEvict(value = "statistics", allEntries = true)
    @OperationLog(module = "缴费记录", action = "新增", description = "手动新增缴费记录")
    @PostMapping("/add")
    public Result<?> add(@RequestBody ParkPayment payment) {
        // 参数校验：必填字段不能为空
        if (payment.getLotId()      == null)
            return Result.error(400, "停车场不能为空");
        if (payment.getPlateNumber() == null || payment.getPlateNumber().trim().isEmpty())
            return Result.error(400, "车牌号不能为空");
        if (payment.getCarType()    == null)
            return Result.error(400, "车辆类型不能为空");
        if (payment.getAmount()     == null)
            return Result.error(400, "支付金额不能为空");

        payment.setPlateNumber(payment.getPlateNumber().trim()); // 去掉前后空格
        payment.setPayTime(new Date());   // 支付时间 = 当前时间
        payment.setPayStatus(1);          // 手动新增默认已支付

        boolean save = paymentService.save(payment);
        return save ? Result.success("缴费成功") : Result.error(500, "缴费失败");
    }

    /**
     * 【确认支付】PUT /api/payment/pay/{id}
     * 出场或续费后，用户点"确认支付"按钮时调用此接口。
     * 把对应缴费记录的 payStatus 从 0（未支付）改为 1（已支付），
     * 并记录实际支付时间（new Date()）。
     *
     * 防重复支付校验：如果已经是 payStatus=1，直接返回错误，不重复操作。
     *
     * 缓存说明：
     *   确认支付会改变当月收入统计，因此清除 statistics 缓存。
     *
     * @param id 缴费记录的主键 ID（从 URL 路径取，如 /pay/18 → id=18）
     */
    @CacheEvict(value = "statistics", allEntries = true)
    @OperationLog(module = "缴费记录", action = "支付", description = "确认支付")
    @PutMapping("/pay/{id}")
    public Result<?> pay(@PathVariable Long id) {
        ParkPayment payment = paymentService.getById(id);
        if (payment == null)
            return Result.error(400, "缴费记录不存在");
        if (payment.getPayStatus() == 1)
            return Result.error(400, "该记录已支付，请勿重复操作");

        payment.setPayStatus(1);         // 标记已支付
        payment.setPayTime(new Date());  // 记录实际支付时间

        return paymentService.updateById(payment)
                ? Result.success("支付成功")
                : Result.error(500, "支付失败");
    }

    /**
     * 【删除缴费记录】DELETE /api/payment/delete/{id}
     */
    @OperationLog(module = "缴费记录", action = "删除", description = "删除缴费记录")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean remove = paymentService.removeById(id);
        return remove ? Result.success("删除成功") : Result.error(500, "删除失败");
    }

    /**
     * 【查询单条缴费记录】GET /api/payment/detail/{id}
     *
     * 根据主键 ID 查询单条记录，用于详情展示或二次确认场景。
     */
    @GetMapping("/detail/{id}")
    public Result<ParkPayment> detail(@PathVariable Long id) {
        ParkPayment payment = paymentService.getById(id);
        if (payment == null) return Result.error(400, "缴费记录不存在");
        return Result.success("查询成功", payment);
    }
}
