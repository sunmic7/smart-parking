package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.ParkLot;
import com.parking.smart_parking.entity.ParkPayment;
import com.parking.smart_parking.entity.ParkRecord;
import com.parking.smart_parking.service.IParkLotService;
import com.parking.smart_parking.service.IParkPaymentService;
import com.parking.smart_parking.service.IParkRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 【停车记录控制器】ParkRecordController
 *
 * 管理 park_record 表，记录每辆车的完整停车过程（入场→在场→出场）。
 *
 * 提供接口：
 *   GET    /api/record/list               查询记录列表（可按车牌筛选）
 *   GET    /api/record/check-in-lot       查询车辆是否在指定停车场内
 *   POST   /api/record/add                手动入场登记
 *   POST   /api/record/exit               手动出场结算
 *   PUT    /api/record/update-entry-time  调整入场时间（演示计费用）
 *   DELETE /api/record/delete/{id}        删除停车记录
 *   GET    /api/record/detail/{id}        查询单条记录详情
 *
 * 停车记录状态（status）：
 *   0 = 在场中（已入场，未出场）
 *   1 = 已出场（完整停车过程结束）
 *
 * 注意：车牌识别页（PlateRecognizeController）也有入场/出场接口，
 * 本控制器的入场/出场是"手动管理"版本，
 * 逻辑相同但用于停车记录管理页的人工操作场景。
 */
@RestController
@RequestMapping("/api/record")
public class ParkRecordController {

    @Autowired
    private IParkRecordService recordService;

    @Autowired
    private IParkLotService parkLotService;

    @Autowired
    private IParkPaymentService paymentService;

    /**
     * 【查询停车记录列表】GET /api/record/list?plateNumber=xxx&lotId=xxx&date=2025-07-06
     *
     * 支持多条件组合查询：
     *   - plateNumber：按车牌号模糊查询
     *   - lotId：按停车场精确查询
     *   - date：按入场日期查询（某一天，格式 yyyy-MM-dd）
     *
     * 结果按入场时间倒序（最新的在最前面）。
     *
     * 批量填充停车场名称（lotName）：
     *   park_record 表只存了 lot_id，没有 lot_name 字段。
     *   用 lotNameCache（HashMap 作缓存）避免重复查询同一停车场：
     *   computeIfAbsent(key, 查询函数)：如果缓存没有该 key，才执行查询并存入缓存。
     *   好处：同一停车场只查一次数据库，N 条记录最多只查 M 个停车场（M << N）。
     */
    @GetMapping("/list")
    public Result<List<ParkRecord>> list(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String date) {

        QueryWrapper<ParkRecord> query = new QueryWrapper<>();
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
                query.ge("entry_time", start).lt("entry_time", end);
            } catch (Exception ignored) {
                // 日期格式非法时跳过该条件，避免报错
            }
        }
        query.orderByDesc("entry_time"); // 最新入场的排最前面

        List<ParkRecord> records = recordService.list(query);

        // 用 HashMap 缓存已查过的停车场名，避免 N+1 查询问题
        Map<Long, String> lotNameCache = new HashMap<>();
        for (ParkRecord r : records) {
            if (r.getLotId() == null) continue;
            // computeIfAbsent：key 不存在时才执行 Lambda 里的查询，存在则直接返回缓存值
            lotNameCache.computeIfAbsent(r.getLotId(), id -> {
                ParkLot lot = parkLotService.getById(id);
                return lot != null ? lot.getLotName() : "（已删除停车场）";
            });
            r.setLotName(lotNameCache.get(r.getLotId())); // 把名称写入实体对象，序列化后传给前端
        }
        return Result.success("查询成功", records);
    }

    /**
     * 【查询车辆是否在场】GET /api/record/check-in-lot?plateNumber=xxx&lotId=xxx
     *
     * 车牌识别页识别到车牌后，调用此接口判断该车是否在场，
     * 以此决定自动执行"入场"还是"出场"操作。
     *
     * 在场判断条件：plate_number 匹配 + lot_id 匹配 + status=0（在场中）
     *
     * 返回数据：
     *   在场：{ inLot: true, recordId: 123, entryTime: "..." }
     *   不在场：{ inLot: false }
     *
     * @param plateNumber 车牌号（统一转大写，如 "粤B12345"）
     * @param lotId       停车场 ID
     */
    @GetMapping("/check-in-lot")
    public Result<?> checkInLot(@RequestParam String plateNumber,
                                @RequestParam Long lotId) {
        QueryWrapper<ParkRecord> query = new QueryWrapper<>();
        query.eq("plate_number", plateNumber.trim().toUpperCase()) // 统一大写
             .eq("lot_id", lotId)
             .eq("status", 0); // 只查在场中的记录

        ParkRecord record = recordService.getOne(query);

        if (record != null) {
            // 车辆在场：返回在场标志 + 记录 ID（出场时需要用 ID 定位记录）
            Map<String, Object> data = new HashMap<>();
            data.put("inLot",     true);
            data.put("recordId",  record.getId());
            data.put("entryTime", record.getEntryTime());
            return Result.success("车辆在场", data);
        }

        // 车辆不在场
        Map<String, Object> data = new HashMap<>();
        data.put("inLot", false);
        return Result.success("车辆不在场", data);
    }

    /**
     * 【手动入场登记】POST /api/record/add
     *
     * 在停车记录管理页手动新增入场记录（通常用于异常补录）。
     * 正常业务流程走车牌识别页的 PlateRecognizeController.entry()。
     *
     * 防重复入场：同一车牌不能同时有两条"在场中（status=0）"的记录。
     * （此处不区分停车场，只要在任意停车场有在场记录就不允许再次入场）
     */
    @OperationLog(module = "停车记录", action = "入场", description = "手动新增入场记录")
    @PostMapping("/add")
    public Result<?> add(@RequestBody ParkRecord record) {
        if (record.getLotId()      == null)
            return Result.error(400, "停车场不能为空");
        if (record.getPlateNumber() == null || record.getPlateNumber().trim().isEmpty())
            return Result.error(400, "车牌号不能为空");
        if (record.getCarType()    == null)
            return Result.error(400, "车辆类型不能为空");

        // 防重复入场检查
        QueryWrapper<ParkRecord> query = new QueryWrapper<>();
        query.eq("plate_number", record.getPlateNumber().trim()).eq("status", 0);
        if (recordService.getOne(query) != null)
            return Result.error(400, "该车辆已在场内，不能重复入场");

        record.setPlateNumber(record.getPlateNumber().trim());
        record.setEntryTime(new Date()); // 入场时间 = 当前时间
        record.setStatus(0);             // 状态 = 在场中

        boolean save = recordService.save(record);
        return save ? Result.success("入场登记成功") : Result.error(500, "入场登记失败");
    }

    /** 注入包月车 Service（出场时需要重新验证包月车有效性） */
    @Autowired
    private com.parking.smart_parking.service.IParkMonthlyCarService monthlyCarService;

    /**
     * 【手动出场结算】POST /api/record/exit
     *
     * 流程：
     *   1. 根据 record.getId() 查出数据库里的停车记录
     *   2. 校验记录存在且状态为"在场中"
     *   3. 重新验证是否为有效包月车（以出场时刻为准，不依赖入场时存的 carType）
     *   4. 计算停车时长（分钟）和应缴费用
     *   5. 更新停车记录（状态→已出场，写入时长、费用、出场图片）
     *   6. 临时车且费用>0 时，生成待支付缴费记录
     *
     * 为什么出场时要重新验证包月车？
     *   入场时是包月车，但停车期间可能到期（由定时任务把 status 改为 0），
     *   出场时重新查一次，以最新状态为准，更准确公平。
     *
     * 有效包月车的判断标准（与定时任务一致）：
     *   status=1 AND expire_date >= 今天零点
     *   用今天零点而不是当前时间，防止当天到期的包月车在当天被误判为临时车。
     */
    @OperationLog(module = "停车记录", action = "出场", description = "手动出场登记")
    @PostMapping("/exit")
    public Result<?> exit(@RequestBody ParkRecord record) {
        if (record.getId() == null) return Result.error(400, "记录ID不能为空");

        ParkRecord dbRecord = recordService.getById(record.getId());
        if (dbRecord == null)
            return Result.error(400, "停车记录不存在");
        if (dbRecord.getStatus() != null && dbRecord.getStatus() == 1)
            return Result.error(400, "该车辆已出场，无需重复登记");

        // 计算今天零点（作为包月车有效期的判断基准）
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date todayStart = todayCal.getTime();

        // 重新验证包月车有效性（以出场时刻为准）
        QueryWrapper<com.parking.smart_parking.entity.ParkMonthlyCar> mq = new QueryWrapper<>();
        mq.eq("lot_id",       dbRecord.getLotId())
          .eq("plate_number", dbRecord.getPlateNumber())
          .eq("status",       1)
          .ge("expire_date",  todayStart); // ge = greater than or equal，到期日>=今天零点
        boolean isValidMonthlyCar = monthlyCarService.getOne(mq) != null;
        int actualCarType = isValidMonthlyCar ? 1 : 2; // 1=包月车，2=临时车
        dbRecord.setCarType(actualCarType); // 用实际类型覆盖入场时存的类型

        Date exitTime = new Date();
        dbRecord.setExitTime(exitTime);
        dbRecord.setExitImgUrl(record.getExitImgUrl()); // 出场图片（可选）
        dbRecord.setStatus(1); // 状态改为已出场

        // 计算停车时长：(出场时间毫秒 - 入场时间毫秒) / 60000 = 分钟数
        int parkingMinutes = (int) ((exitTime.getTime() - dbRecord.getEntryTime().getTime()) / 60000);
        dbRecord.setParkingMinutes(parkingMinutes);

        // 计算应缴费用（包月车免费，临时车按规则计费）
        BigDecimal fee = BigDecimal.ZERO;
        if (!isValidMonthlyCar) {
            ParkLot lot = parkLotService.getById(dbRecord.getLotId());
            if (lot != null) {
                fee = calculateFee(dbRecord.getEntryTime(), exitTime, lot);
            }
        }
        dbRecord.setPayableAmount(fee);

        if (!recordService.updateById(dbRecord))
            return Result.error(500, "出场登记失败");

        // 包月车：免费出场，不生成缴费记录
        if (isValidMonthlyCar) {
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber",    dbRecord.getPlateNumber());
            data.put("carType",        1);
            data.put("amount",         BigDecimal.ZERO);
            data.put("entryTime",      dbRecord.getEntryTime());
            data.put("exitTime",       exitTime);
            data.put("parkingMinutes", parkingMinutes);
            return Result.success("包月车出场成功，无需缴费", data);
        }

        // 临时车：生成待支付缴费记录（费用>0 时）
        ParkLot lot = parkLotService.getById(dbRecord.getLotId());
        if (lot == null) return Result.success("出场成功，但未找到停车场计费规则，未生成缴费记录");

        Map<String, Object> data = new HashMap<>();
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            ParkPayment payment = new ParkPayment();
            payment.setRecordId(dbRecord.getId());
            payment.setLotId(dbRecord.getLotId());
            payment.setPlateNumber(dbRecord.getPlateNumber());
            payment.setCarType(2);       // 临时车
            payment.setAmount(fee);      // 应缴金额
            payment.setPayTime(new Date());
            payment.setPayStatus(0);     // 未支付，等前端确认
            paymentService.save(payment);
            data.put("paymentId", payment.getId()); // 返回给前端，用于确认支付
        }

        data.put("plateNumber",    dbRecord.getPlateNumber());
        data.put("carType",        actualCarType);
        data.put("amount",         fee);
        data.put("entryTime",      dbRecord.getEntryTime());
        data.put("exitTime",       exitTime);
        data.put("parkingMinutes", parkingMinutes);
        return Result.success("临时车出场成功，已自动生成缴费记录", data);
    }

    /**
     * 【调整入场时间（演示用）】PUT /api/record/update-entry-time
     *
     * 把某条在场记录的入场时间修改为指定时间，
     * 用于演示"停了N小时应收多少钱"的计费效果，不影响真实业务。
     * 只能修改状态为"在场中（status=0）"的记录，已出场的不允许修改。
     *
     * 时间格式要求：yyyy-MM-dd HH:mm:ss（用 SimpleDateFormat 解析）
     * 时区：GMT+8（北京时间），明确指定防止服务器时区不同导致解析偏差。
     */
    @OperationLog(module = "停车记录", action = "修改入场时间", description = "调整车辆入场时间（演示用）")
    @PutMapping("/update-entry-time")
    public Result<?> updateEntryTime(@RequestBody Map<String, Object> params) {
        Object idObj   = params.get("id");
        Object timeObj = params.get("entryTime");
        if (idObj == null || timeObj == null) return Result.error(400, "参数不完整");

        ParkRecord record = recordService.getById(Long.valueOf(idObj.toString()));
        if (record == null)
            return Result.error(400, "停车记录不存在");
        if (record.getStatus() != null && record.getStatus() == 1)
            return Result.error(400, "车辆已出场，无法修改入场时间");

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8")); // 明确指定北京时间
            record.setEntryTime(sdf.parse(timeObj.toString()));
        } catch (Exception e) {
            return Result.error(400, "时间格式错误，请使用 yyyy-MM-dd HH:mm:ss");
        }

        return recordService.updateById(record)
                ? Result.success("入场时间已更新")
                : Result.error(500, "更新失败");
    }

    /**
     * 【删除停车记录】DELETE /api/record/delete/{id}
     */
    @OperationLog(module = "停车记录", action = "删除", description = "删除停车记录")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean remove = recordService.removeById(id);
        return remove ? Result.success("删除成功") : Result.error(500, "删除失败");
    }

    /**
     * 【查询单条停车记录】GET /api/record/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result<ParkRecord> detail(@PathVariable Long id) {
        ParkRecord record = recordService.getById(id);
        if (record == null) return Result.error(400, "记录不存在");
        return Result.success("查询成功", record);
    }

    /**
     * 【计费方法】临时车停车费用计算
     *
     * 规则（与 PlateRecognizeController 中保持一致）：
     *   1. 停车时长 <= 免费时长 → 免费
     *   2. 超出部分按"计费单元"向上取整 → 乘以单价
     *   3. 每日费用超出"每日封顶金额" → 按当日封顶收费
     *   4. 跨天停车时，每天独立计算并累加，避免停多天只收一天费用
     *
     * 向上取整公式：(chargeMinutes + unitMinutes - 1) / unitMinutes
     * 等效于 Math.ceil(chargeMinutes / unitMinutes)，但用整数运算避免浮点误差。
     *
     * @param entryTime 入场时间
     * @param exitTime  出场时间
     * @param lot       停车场对象（含收费参数）
     * @return 应缴费用（保留 2 位小数，四舍五入）
     */
    private BigDecimal calculateFee(Date entryTime, Date exitTime, ParkLot lot) {
        if (entryTime == null || exitTime == null) return BigDecimal.ZERO;

        // 总停车分钟数
        long totalMinutes = (exitTime.getTime() - entryTime.getTime()) / (1000 * 60);

        int freeMinutes = lot.getFreeMinutes() == null ? 0 : lot.getFreeMinutes();
        // 未超过免费时长，直接免费
        if (totalMinutes <= freeMinutes) return BigDecimal.ZERO;

        // 计费单元（默认 60 分钟，即按小时计费）
        int unitMinutes = (lot.getUnitMinutes() == null || lot.getUnitMinutes() <= 0)
                ? 60 : lot.getUnitMinutes();

        BigDecimal unitPrice = lot.getUnitPrice() == null ? BigDecimal.ZERO : lot.getUnitPrice();
        BigDecimal maxFee = lot.getMaxFee(); // 每日封顶金额

        // 按日历天拆分停车时长，每天独立计费并应用封顶
        LocalDateTime entryDt = entryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime exitDt = exitTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDate entryDate = entryDt.toLocalDate();
        LocalDate exitDate = exitDt.toLocalDate();

        long remainingFree = freeMinutes;
        BigDecimal totalFee = BigDecimal.ZERO;

        for (LocalDate date = entryDate; !date.isAfter(exitDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            LocalDateTime actualStart = entryDt.isAfter(dayStart) ? entryDt : dayStart;
            LocalDateTime actualEnd = exitDt.isBefore(dayEnd) ? exitDt : dayEnd;

            if (!actualEnd.isAfter(actualStart)) continue;

            long dayMinutes = java.time.Duration.between(actualStart, actualEnd).toMinutes();

            // 免费时长只在开头抵扣一次
            long chargeMinutes = dayMinutes;
            if (remainingFree > 0) {
                if (dayMinutes <= remainingFree) {
                    remainingFree -= dayMinutes;
                    chargeMinutes = 0;
                } else {
                    chargeMinutes = dayMinutes - remainingFree;
                    remainingFree = 0;
                }
            }

            if (chargeMinutes <= 0) continue;

            long units = (chargeMinutes + unitMinutes - 1) / unitMinutes;
            BigDecimal dayFee = unitPrice.multiply(BigDecimal.valueOf(units));
            if (maxFee != null && dayFee.compareTo(maxFee) > 0) {
                dayFee = maxFee;
            }
            totalFee = totalFee.add(dayFee);
        }

        // 保留 2 位小数，四舍五入（BigDecimal 精确计算，不会有浮点误差）
        return totalFee.setScale(2, RoundingMode.HALF_UP);
    }
}
