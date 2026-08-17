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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 【包月车管理控制器】ParkMonthlyCarController
 *
 * 提供接口：
 *   GET    /api/monthly-car/list          查询包月车列表（多条件筛选）
 *   POST   /api/monthly-car/save          新增或编辑包月车（saveOrUpdate）
 *   DELETE /api/monthly-car/delete/{id}   删除包月车
 *   POST   /api/monthly-car/renew         续费（延长到期日 + 生成缴费记录）
 *
 * 包月车的核心状态：
 *   status = 1：正常（到期日 >= 今天）
 *   status = 0：过期（到期日 < 今天）
 * 状态由后端定时任务（MonthlyCarExpireTask）每分钟自动更新，
 * 出场时也会实时校验，双重保险。
 */
@RestController
@RequestMapping("/api/monthly-car")
public class ParkMonthlyCarController {

    @Autowired
    private IParkMonthlyCarService monthlyCarService;

    @Autowired
    private IParkLotService parkLotService;

    /** 日期格式化：把 Date 对象格式化为 "yyyy-MM-dd" 字符串返回给前端 */
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 【查询包月车列表】GET /api/monthly-car/list
     *
     * 支持五个可选筛选条件，全部用 LIKE 模糊匹配（方便搜索）：
     *   lotName     停车场名称（先查停车场表，再用 lot_id 列表过滤）
     *   plateNumber 车牌号
     *   ownerName   车主姓名
     *   phone       手机号
     *   spaceNumber 已购车位号
     *
     * 按停车场名称筛选的特殊处理：
     *   park_monthly_car 表只存了 lot_id，没有 lot_name。
     *   所以先去 park_lot 表模糊查停车场名，得到 lot_id 列表，
     *   再用 WHERE lot_id IN (id1, id2, ...) 过滤包月车。
     *   如果没有匹配的停车场，直接返回空列表（短路优化，不查包月车表）。
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) String lotName,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String spaceNumber) {

        QueryWrapper<ParkMonthlyCar> wrapper = new QueryWrapper<>();

        // 按停车场名过滤（跨表查询：先查停车场，取 ID 列表，再 IN 查询）
        if (lotName != null && !lotName.trim().isEmpty()) {
            QueryWrapper<ParkLot> lotWrapper = new QueryWrapper<>();
            lotWrapper.like("lot_name", lotName.trim());
            List<ParkLot> matchedLots = parkLotService.list(lotWrapper);

            // 没有匹配的停车场，直接返回空列表，不需要再查包月车表
            if (matchedLots.isEmpty()) {
                return Result.success("查询成功", new ArrayList<>());
            }

            // 用 Java Stream 的 map() 把停车场列表转换为 ID 列表
            // 等效：for (ParkLot lot : matchedLots) { ids.add(lot.getId()); }
            List<Long> lotIds = matchedLots.stream()
                    .map(ParkLot::getId)   // 方法引用，等同于 lot -> lot.getId()
                    .collect(Collectors.toList());

            // IN 查询：WHERE lot_id IN (1, 2, 3)
            wrapper.in("lot_id", lotIds);
        }

        // 其余条件：非空时加入 WHERE，空则不加（不影响查询）
        if (plateNumber != null && !plateNumber.trim().isEmpty())
            wrapper.like("plate_number", plateNumber.trim());
        if (ownerName != null && !ownerName.trim().isEmpty())
            wrapper.like("owner_name", ownerName.trim());
        if (phone != null && !phone.trim().isEmpty())
            wrapper.like("phone", phone.trim());
        if (spaceNumber != null && !spaceNumber.trim().isEmpty())
            wrapper.like("space_number", spaceNumber.trim());

        List<ParkMonthlyCar> list = monthlyCarService.list(wrapper);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (ParkMonthlyCar car : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id",          car.getId());
            row.put("lotId",       car.getLotId());
            row.put("lotName",     getLotName(car));  // 调私有方法查停车场名
            row.put("plateNumber", car.getPlateNumber());
            row.put("ownerName",   car.getOwnerName());
            row.put("gender",      car.getGender());
            row.put("phone",       car.getPhone());
            row.put("spaceNumber", car.getSpaceNumber());
            // 日期格式化：Date → "yyyy-MM-dd"，null 时直接返回 null（三元表达式防止 NPE）
            row.put("expireDate",  car.getExpireDate() == null ? null : sdfDate.format(car.getExpireDate()));
            row.put("startDate",   car.getStartDate()  == null ? null : sdfDate.format(car.getStartDate()));
            row.put("status",      car.getStatus());
            resultList.add(row);
        }

        return Result.success("查询成功", resultList);
    }

    /**
     * 【新增/编辑包月车】POST /api/monthly-car/save
     *
     * 用 Map 接收参数而不是直接用 ParkMonthlyCar 实体，
     * 原因：前端传的字段格式不固定（如日期有时是字符串，有时是 null），
     * 用 Map 手动解析更可控，避免 JSON 反序列化异常。
     *
     * 新增 vs 编辑的区别：
     *   新增（params 里没有 id）：
     *     - 到期日 = 起始日（表示"待续费"，还没激活）
     *     - status 强制设为 0（过期/未激活），需要前端点续费后才变 1
     *   编辑（params 里有 id）：
     *     - 优先用前端传的 expireDate；
     *     - 若前端没传，则保留数据库里已有的到期日（不覆盖）
     *
     * 防重复检查：同一停车场内，同一车牌只能有一条记录（唯一约束）。
     * 编辑时排除自身（.ne("id", car.getId())），避免误报重复。
     */
    @OperationLog(module = "车辆管理", action = "新增", description = "新增/编辑包月车")
    @PostMapping("/save")
    public Result<?> save(@RequestBody Map<String, Object> params) {
        if (params == null) return Result.error(400, "请求参数不能为空");

        // 手动从 Map 取值并做 null 安全处理
        String plateNumberStr = params.get("plateNumber") == null ? null
                : params.get("plateNumber").toString().trim();
        Object lotIdObj = params.get("lotId");

        if (plateNumberStr == null || plateNumberStr.isEmpty())
            return Result.error(400, "车牌号不能为空");
        if (lotIdObj == null)
            return Result.error(400, "请选择停车场");

        Long lotId = Long.valueOf(lotIdObj.toString());
        ParkLot lot = parkLotService.getById(lotId);
        if (lot == null) return Result.error(400, "停车场不存在");

        // 构建包月车对象（逐个字段赋值，只赋非 null 的字段）
        ParkMonthlyCar car = new ParkMonthlyCar();
        car.setLotId(lotId);
        car.setPlateNumber(plateNumberStr);
        if (params.get("ownerName")   != null) car.setOwnerName(params.get("ownerName").toString().trim());
        if (params.get("phone")       != null) car.setPhone(params.get("phone").toString().trim());
        if (params.get("spaceNumber") != null) car.setSpaceNumber(params.get("spaceNumber").toString());
        if (params.get("gender")      != null) car.setGender(Integer.valueOf(params.get("gender").toString()));
        if (params.get("status")      != null) car.setStatus(Integer.valueOf(params.get("status").toString()));
        if (params.get("id")          != null) car.setId(Long.valueOf(params.get("id").toString()));

        // 解析起始日（默认今天）
        Date startDate = new Date();
        if (params.get("startDate") != null && !params.get("startDate").toString().isEmpty()) {
            try {
                startDate = new java.text.SimpleDateFormat("yyyy-MM-dd")
                        .parse(params.get("startDate").toString());
            } catch (Exception ignored) {} // 解析失败则用默认值（今天）
        }
        car.setStartDate(startDate);

        if (car.getId() == null) {
            // 新增：到期日 = 起始日，status=0（未激活，等待续费）
            car.setExpireDate(startDate);
            car.setStatus(0);
        } else {
            // 编辑：优先用前端传来的 expireDate
            if (params.get("expireDate") != null && !params.get("expireDate").toString().isEmpty()) {
                try {
                    car.setExpireDate(new java.text.SimpleDateFormat("yyyy-MM-dd")
                            .parse(params.get("expireDate").toString()));
                } catch (Exception ignored) {
                    // 解析失败：保留数据库里原有的到期日
                    ParkMonthlyCar exist = monthlyCarService.getById(car.getId());
                    if (exist != null) car.setExpireDate(exist.getExpireDate());
                }
            } else {
                // 前端没传 expireDate：保留数据库原有值
                ParkMonthlyCar exist = monthlyCarService.getById(car.getId());
                if (exist != null) car.setExpireDate(exist.getExpireDate());
            }
        }

        if (car.getStatus() == null) car.setStatus(1); // 默认正常状态

        // 防重复检查：同停车场 + 同车牌不能重复（编辑时排除自身）
        QueryWrapper<ParkMonthlyCar> dup = new QueryWrapper<>();
        dup.eq("lot_id", car.getLotId()).eq("plate_number", car.getPlateNumber());
        if (car.getId() != null) dup.ne("id", car.getId()); // ne = not equal，排除自身
        if (monthlyCarService.getOne(dup) != null)
            return Result.error(400, "同一停车场下该车牌已存在");

        // saveOrUpdate：有 id 则 UPDATE，无 id 则 INSERT（MyBatis-Plus 自动判断）
        boolean success = monthlyCarService.saveOrUpdate(car);
        return success ? Result.success("操作成功") : Result.error(500, "操作失败");
    }

    /**
     * 【删除包月车】DELETE /api/monthly-car/delete/{id}
     */
    @OperationLog(module = "车辆管理", action = "删除", description = "删除包月车记录")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = monthlyCarService.removeById(id);
        return success ? Result.success("删除成功") : Result.error(500, "删除失败");
    }

    /**
     * 注入缴费 Service（续费时需要生成一条缴费记录）
     * 写在这里而不是顶部，是因为这是后来新增的依赖，防止影响已有代码的阅读顺序
     */
    @Autowired
    private com.parking.smart_parking.service.IParkPaymentService paymentService;

    /**
     * 【包月车续费】POST /api/monthly-car/renew
     *
     * 两件事：
     *   1. 把该车的到期日往后推 months 个月，status 改为 1（正常）
     *   2. 生成一条缴费记录（payStatus=0 未支付），返回 paymentId 给前端
     *      前端拿到 paymentId 后，用户点"确认支付"再调 /api/payment/pay/{id}
     *
     * 续费基准时间逻辑：
     *   如果当前到期日 >= 今天（还没过期）→ 从到期日往后推（叠加续费）
     *   如果当前到期日 < 今天（已过期）→ 从今天往后推（重新激活）
     *
     * @param params 包含 id（包月车ID）、months（续费月数）、amount（实付金额，含优惠）
     */
    @OperationLog(module = "车辆管理", action = "续费", description = "包月车续费")
    @PostMapping("/renew")
    public Result<?> renew(@RequestBody Map<String, Object> params) {
        if (params == null || !params.containsKey("id") || !params.containsKey("months"))
            return Result.error(400, "请求参数不能为空");

        Long id     = Long.valueOf(params.get("id").toString());
        int  months = Integer.parseInt(params.get("months").toString());
        // amount 是前端计算好的实付金额（已减去优惠），后端直接存入缴费记录
        BigDecimal amount = params.get("amount") != null
                ? new BigDecimal(params.get("amount").toString())
                : BigDecimal.ZERO;

        ParkMonthlyCar car = monthlyCarService.getById(id);
        if (car == null) return Result.error(400, "车辆不存在");

        // 确定续费基准日期
        Date base = car.getExpireDate();
        Date now  = new Date();
        // 已过期（到期日早于今天）→ 从今天开始续，防止从过去的日期叠加
        if (base == null || base.before(now)) base = now;

        // Calendar 是 Java 处理日期计算的工具类，add(MONTH, n) 往后推 n 个月
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.MONTH, months); // 往后推 months 个月
        car.setExpireDate(cal.getTime());
        car.setStatus(1); // 续费后状态变为"正常"

        if (!monthlyCarService.updateById(car))
            return Result.error(500, "续费失败");

        // 生成缴费记录（payStatus=0 未支付，等前端用户点"确认支付"按钮）
        com.parking.smart_parking.entity.ParkPayment payment = new com.parking.smart_parking.entity.ParkPayment();
        payment.setLotId(car.getLotId());
        payment.setPlateNumber(car.getPlateNumber());
        payment.setCarType(1);       // 1 = 包月车续费
        payment.setAmount(amount);   // 含优惠后的实付金额
        payment.setPayTime(now);
        payment.setPayStatus(0);     // 0 = 未支付（待确认）
        paymentService.save(payment);

        // 把 paymentId 返回给前端，前端确认支付时需要用
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", payment.getId());
        return Result.success("续费成功", data);
    }

    /**
     * 私有辅助方法：根据包月车对象的 lotId 查询停车场名称
     * 在 list 接口中循环调用，用于给每条记录补充停车场名
     *
     * 注意：每条记录都查一次数据库（N+1 问题），数据量大时可以改为批量查询 + Map 缓存
     */
    private String getLotName(ParkMonthlyCar car) {
        if (car.getLotId() != null) {
            ParkLot lot = parkLotService.getById(car.getLotId());
            if (lot != null) return lot.getLotName();
        }
        return "";
    }
}
