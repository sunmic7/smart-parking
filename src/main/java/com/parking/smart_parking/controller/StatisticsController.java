package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.ParkLot;
import com.parking.smart_parking.entity.ParkPayment;
import com.parking.smart_parking.entity.ParkRecord;
import com.parking.smart_parking.service.IParkLotService;
import com.parking.smart_parking.service.IParkPaymentService;
import com.parking.smart_parking.service.IParkRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

/**
 * 【统计控制器】StatisticsController —— Dashboard 数据来源
 *
 * 提供接口：
 *   GET /api/statistics/revenue?year=2025&month=6
 *   查询指定年月内每个停车场的收入统计（包月收入 + 临停收入）
 *
 * Dashboard.vue 调用此接口，把数据渲染成柱状图展示。
 *
 * 统计逻辑：
 *   从 park_payment 表中查找指定月份内所有 payStatus=1（已支付）的记录，
 *   按 lot_id 和 carType 分组累加金额，最终输出每个停车场的收入明细。
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private IParkPaymentService paymentService;

    @Autowired
    private IParkLotService lotService;

    @Autowired
    private IParkRecordService recordService;

    /**
     * 【月度收入统计】GET /api/statistics/revenue?year=2025&month=6
     *
     * 统计指定年月内每个停车场的收入，分为包月和临停两类。
     *
     * 执行步骤：
     *   Step 1：根据 year 和 month 计算当月的起止时间（[monthStart, monthEnd)）
     *   Step 2：查 park_payment 表，过滤条件：pay_time 在当月 且 payStatus=1（已支付）
     *   Step 3：遍历支付记录，按 lot_id 分组，分别累加 carType=1（包月）和 carType=2（临停）的金额
     *   Step 4：查所有停车场列表，逐个填充统计金额（没有收入的停车场也要显示，金额为 0）
     *   Step 5：返回结果给 Dashboard.vue 渲染成柱状图
     *
     * 为什么用两个 Map 而不是分组查询 SQL？
     *   Java 层分组比写复杂 SQL 更直观易维护，数据量不大时性能差异可忽略。
     *
     * 缓存说明：
     *   使用 @Cacheable 将统计结果缓存到 Redis，key = "statistics::2025:6"。
     *   历史月份数据几乎不变，可大幅提升 Dashboard 加载速度。
     *
     * @param year  年份，如 2025
     * @param month 月份，如 6（1-12）
     */
    @Cacheable(value = "statistics", key = "#year + ':' + #month")
    @GetMapping("/revenue")
    public Result<?> revenue(@RequestParam int year, @RequestParam int month) {

        // ---- Step 1：构造当月起止时间 ----
        // 月份起始：当月 1 日 00:00:00.000
        Calendar start = Calendar.getInstance();
        start.set(year, month - 1, 1, 0, 0, 0); // month - 1 因为 Calendar 月份从 0 开始
        start.set(Calendar.MILLISECOND, 0);

        // 月份结束：下个月 1 日 00:00:00.000（使用"左闭右开"区间，不包含下月数据）
        Calendar end = Calendar.getInstance();
        end.set(year, month - 1, 1, 0, 0, 0);
        end.set(Calendar.MILLISECOND, 0);
        end.add(Calendar.MONTH, 1); // 加 1 个月得到下月 1 日

        // ---- Step 2：查询当月所有已支付记录 ----
        // ge("pay_time", start) → pay_time >= 月初
        // lt("pay_time", end)   → pay_time <  下月初（即 <= 本月最后一天）
        // eq("pay_status", 1)   → 只统计已支付的（未支付的不计入收入）
        QueryWrapper<ParkPayment> qw = new QueryWrapper<>();
        qw.ge("pay_time",   start.getTime())
          .lt("pay_time",   end.getTime())
          .eq("pay_status", 1);
        List<ParkPayment> payments = paymentService.list(qw);

        // ---- Step 3：按停车场分组累加（用两个 Map 分别存包月和临停收入）----
        // key = lot_id，value = 该停车场该类型的总收入（BigDecimal 精确计算）
        Map<Long, BigDecimal> monthlyMap = new HashMap<>(); // carType=1 包月续费收入
        Map<Long, BigDecimal> tempMap    = new HashMap<>(); // carType=2 临时停车收入

        for (ParkPayment p : payments) {
            if (p.getLotId() == null || p.getAmount() == null) continue;

            if (p.getCarType() != null && p.getCarType() == 1) {
                // merge(key, value, 合并函数)：
                // 如果 key 不存在，put(key, value)；
                // 如果 key 已存在，执行合并函数（这里是两个 BigDecimal 相加）
                monthlyMap.merge(p.getLotId(), p.getAmount(), BigDecimal::add);
            } else {
                tempMap.merge(p.getLotId(), p.getAmount(), BigDecimal::add);
            }
        }

        // ---- Step 4：查所有停车场，填充收入数据 ----
        // 即使某停车场当月没有收入，也要出现在结果里（金额显示 0），
        // 否则 Dashboard 柱状图会缺少该停车场的柱子
        List<ParkLot> lots = lotService.list(
                new QueryWrapper<ParkLot>().orderByAsc("id")); // 按 ID 升序

        List<Map<String, Object>> result = new ArrayList<>();
        for (ParkLot lot : lots) {
            Map<String, Object> row = new HashMap<>();
            row.put("lotId",   lot.getId());
            row.put("lotName", lot.getLotName());
            // getOrDefault：如果 Map 里没有该停车场的收入记录，默认返回 0
            row.put("monthlyIncome", monthlyMap.getOrDefault(lot.getId(), BigDecimal.ZERO));
            row.put("tempIncome",    tempMap.getOrDefault(lot.getId(),    BigDecimal.ZERO));
            // 总收入 = 包月收入 + 临停收入
            row.put("total", monthlyMap.getOrDefault(lot.getId(), BigDecimal.ZERO)
                    .add(tempMap.getOrDefault(lot.getId(), BigDecimal.ZERO)));
            result.add(row);
        }

        return Result.success("查询成功", result);
    }

    /**
     * 【各停车场临时车入场数量统计】GET /api/statistics/temp-entry-count?year=2025&month=6
     *
     * 统计指定年月内每个停车场的临时车入场数量。
     *
     * 执行步骤：
     *   Step 1：根据 year 和 month 计算当月起止时间（[monthStart, monthEnd)）
     *   Step 2：查 park_record 表，过滤条件：entry_time 在当月 且 car_type = 2（临时车）
     *   Step 3：按 lot_id 分组计数
     *   Step 4：查所有停车场列表，逐个填充数量（没有记录的停车场显示 0）
     *
     * @param year  年份，如 2025
     * @param month 月份，如 6（1-12）
     */
    @GetMapping("/temp-entry-count")
    public Result<?> tempEntryCount(@RequestParam int year, @RequestParam int month) {

        // ---- Step 1：构造当月起止时间 ----
        Calendar start = Calendar.getInstance();
        start.set(year, month - 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(year, month - 1, 1, 0, 0, 0);
        end.set(Calendar.MILLISECOND, 0);
        end.add(Calendar.MONTH, 1);

        // ---- Step 2：查询当月所有临时车入场记录 ----
        QueryWrapper<ParkRecord> qw = new QueryWrapper<>();
        qw.ge("entry_time", start.getTime())
          .lt("entry_time", end.getTime())
          .eq("car_type", 2);
        List<ParkRecord> records = recordService.list(qw);

        // ---- Step 3：按停车场分组计数 ----
        Map<Long, Long> countMap = new HashMap<>();
        for (ParkRecord r : records) {
            if (r.getLotId() == null) continue;
            countMap.merge(r.getLotId(), 1L, Long::sum);
        }

        // ---- Step 4：查所有停车场，填充统计数量 ----
        List<ParkLot> lots = lotService.list(
                new QueryWrapper<ParkLot>().orderByAsc("id"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ParkLot lot : lots) {
            Map<String, Object> row = new HashMap<>();
            row.put("lotId", lot.getId());
            row.put("lotName", lot.getLotName());
            row.put("tempEntryCount", countMap.getOrDefault(lot.getId(), 0L));
            result.add(row);
        }

        return Result.success("查询成功", result);
    }
}
