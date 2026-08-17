package com.parking.smart_parking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parking.smart_parking.annotation.OperationLog;
import com.parking.smart_parking.common.Result;
import com.parking.smart_parking.entity.ParkLot;
import com.parking.smart_parking.entity.ParkMonthlyCar;
import com.parking.smart_parking.entity.ParkPayment;
import com.parking.smart_parking.entity.ParkRecord;
import com.parking.smart_parking.service.IParkLotService;
import com.parking.smart_parking.service.IParkMonthlyCarService;
import com.parking.smart_parking.service.IParkPaymentService;
import com.parking.smart_parking.service.IParkRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 【车牌识别控制器】PlateRecognizeController.java —— 系统最核心的控制器
 *
 * 本控制器是整个停车场系统的业务核心，负责三件最关键的事：
 *
 *   1. POST /api/plate/recognize —— 调用百度 OCR 识别车牌号
 *   2. POST /api/plate/entry     —— 车辆入场登记
 *   3. POST /api/plate/exit      —— 车辆出场结算（含计费逻辑）
 *
 * 【整体业务流程】
 *   前端上传车辆图片
 *     ↓
 *   recognize()：图片 → Base64 → 百度OCR → 车牌号 → 判断包月/临时车
 *     ↓
 *   entry()：创建停车记录（status=0，在场中），更新停车场已用车位+1
 *     ↓
 *   exit()：查找在场记录 → 重新验证包月车状态 → 计算费用 →
 *           更新记录状态为已出场 → 生成待支付记录（临时车）→ 车位-1
 *
 * @RestController：这个类的所有方法返回值都直接序列化为 JSON
 * @RequestMapping("/api/plate")：所有接口路径前缀都是 /api/plate
 */
@RestController
@RequestMapping("/api/plate")
public class PlateRecognizeController {

    // ---- 从 application.yml 读取百度AI配置 ----
    // @Value("${baidu.ai.apiKey}") 表示从配置文件的 baidu.ai.apiKey 字段注入值
    // 这样修改 apiKey 只需改配置文件，不需要改代码（方便部署时替换密钥）
    @Value("${baidu.ai.apiKey}")
    private String apiKey;      // 百度AI平台的 API Key（即 client_id）

    @Value("${baidu.ai.secretKey}")
    private String secretKey;   // 百度AI平台的 Secret Key（即 client_secret）

    @Value("${upload.path}")
    private String uploadPath;  // 车辆图片保存路径，如 D:/parking-uploads/

    // RestTemplate：Spring 提供的 HTTP 客户端，用于在后端代码里发 HTTP 请求
    // 本项目用它调用百度AI的 REST API 接口
    private final RestTemplate restTemplate = new RestTemplate();

    // ObjectMapper：Jackson 库的 JSON 工具类，用于解析百度AI返回的 JSON 字符串
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 百度 access_token 缓存机制。
     *
     * 【为什么要缓存？】
     * 百度 OCR 接口需要先用 apiKey+secretKey 换取 access_token，
     * 再用 access_token 调用识别接口。
     * 如果每次识别都重新申请 token，会浪费网络请求，增加响应时间。
     * 百度 token 有效期约 30 天，缓存在内存里，
     * 未过期直接复用，过期了再重新申请。
     */
    private String cachedToken     = null;  // 缓存的 token 字符串
    private long   tokenExpireTime = 0;     // token 过期的时间戳（毫秒）

    // 注入四个业务 Service
    @Autowired private IParkMonthlyCarService monthlyCarService; // 包月车管理
    @Autowired private IParkRecordService     recordService;     // 停车记录管理
    @Autowired private IParkLotService        lotService;        // 停车场管理
    @Autowired private IParkPaymentService    paymentService;    // 缴费记录管理


    // 接口一：车牌识别 POST /api/plate/recognize


    /**
     * 【车牌识别接口】
     *
     * 前端上传车辆图片，后端调用百度 OCR 识别出车牌号，
     * 并判断该车牌是"包月车"还是"临时车"，结果返回给前端。
     *
     * 识别流程：
     *   Step 1：校验参数（停车场ID 和 图片文件不能为空）
     *   Step 2：把上传的图片保存到服务器本地磁盘
     *   Step 3：获取百度 access_token（优先用缓存）
     *   Step 4：把图片转成 Base64 字符串，调用百度 OCR 接口
     *   Step 5：解析百度返回的 JSON，取出车牌号
     *   Step 6：查包月车表，判断是包月车(1)还是临时车(2)
     *   Step 7：返回结果给前端
     *
     * 注意：不加 @OperationLog 是故意的。
     * 摄像头模式下每秒可能调用多次，加日志会往数据库狂写，性能浪费。
     *
     * @param lotId 停车场 ID（前端通过表单参数传入）
     * @param file  上传的车辆图片文件（MultipartFile 是 Spring 的文件上传类型）
     */
    @PostMapping("/recognize")
    public Result<?> recognize(@RequestParam("lotId") Long lotId,
                               @RequestParam("file") MultipartFile file) {

        // ---- Step 1：参数校验 ----
        if (lotId == null)                  return Result.error(400, "请选择停车场");
        if (file == null || file.isEmpty()) return Result.error(400, "请上传车辆图片");

        ParkLot lot = lotService.getById(lotId); // 查停车场信息（后面需要用停车场名称）
        if (lot == null) return Result.error(400, "停车场不存在");

        // ---- Step 2：保存图片到本地磁盘 ----
        // 图片保存后，URL 会存入停车记录，方便后续在记录列表里展示入/出场图片
        String savedImgUrl = saveImage(file);

        // ---- Step 3：获取百度 access_token ----
        String token;
        try {
            token = getAccessToken(); // 优先用缓存，缓存过期才重新申请
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "百度Token获取失败: " + e.getMessage());
        }
        if (token == null) return Result.error(500, "百度Token获取失败，请检查apiKey/secretKey配置");

        // ---- Step 4：调用百度 OCR 车牌识别接口 ----
        String rawResponse;
        try {
            // 把图片字节数组转成 Base64 字符串（百度接口要求图片以 Base64 格式上传）
            byte[] imgBytes = file.getBytes();
            String base64   = Base64.getEncoder().encodeToString(imgBytes);

            // 百度 OCR 车牌识别接口地址（把 token 拼在 URL 参数里）
            String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate?access_token=" + token;

            // 构造 HTTP 请求（表单格式，Content-Type: application/x-www-form-urlencoded）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("image", base64); // image 是百度接口要求的参数名

            // 用 RestTemplate 发 POST 请求，拿到百度的响应字符串
            rawResponse = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), String.class
            ).getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "调用百度OCR接口异常: " + e.getMessage());
        }

        // ---- Step 5：解析百度返回的 JSON ----
        try {
            JsonNode root = objectMapper.readTree(rawResponse); // 把 JSON 字符串解析成树形结构

            // 如果百度返回了 error_code，说明识别出了问题
            if (root.has("error_code")) {
                int code   = root.get("error_code").asInt();
                String msg = root.path("error_msg").asText("未知错误");

                // 错误码 282103 = "target recognize error"，表示图片里没有车牌
                // 这是正常情况（摄像头对着空旷路面时），不算错误，返回空车牌号
                // 前端摄像头模式收到空车牌号会静默继续下一帧，不弹错误提示
                if (code == 282103) {
                    Map<String, Object> empty = new HashMap<>();
                    empty.put("plateNumber", "");
                    return Result.success("未检测到车牌", empty);
                }
                return Result.error(500, "百度OCR错误 [" + code + "]: " + msg);
            }

            // 从百度返回的 JSON 中提取车牌号
            // 百度响应格式：{ "words_result": { "number": "粤B12345", "color": "blue" } }
            String plate = root.path("words_result").path("number").asText("").trim().toUpperCase();
            if (plate.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("plateNumber", "");
                return Result.success("未检测到车牌", empty);
            }

            // ---- Step 6：查询该车牌是否为当前停车场的有效包月车 ----
            // 条件：lot_id 匹配 + 车牌号匹配 + status=1（正常状态，未过期）
            QueryWrapper<ParkMonthlyCar> mq = new QueryWrapper<>();
            mq.eq("lot_id", lotId).eq("plate_number", plate).eq("status", 1);
            int carType = monthlyCarService.getOne(mq) != null ? 1 : 2; // 1=包月车，2=临时车

            // ---- Step 7：组装并返回结果 ----
            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", plate);
            data.put("carType",     carType);
            data.put("carTypeText", carType == 1 ? "包月车" : "临时车");
            data.put("lotId",       lotId);
            data.put("lotName",     lot.getLotName());
            data.put("imgUrl",      savedImgUrl); // 图片URL，前端用于入场时回显图片

            return Result.success("识别成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "解析百度响应失败: " + e.getMessage() + "，原始响应: " + rawResponse);
        }
    }

    // 接口二：车辆入场 POST /api/plate/entry


    /**
     * 【车辆入场登记接口】
     *
     * 车辆识别完成后，点击"确认入场"按钮调用此接口，
     * 在数据库里创建一条停车记录，并更新停车场已用车位数。
     *
     * 入场流程：
     *   Step 1：校验参数（停车场ID、车牌号、车辆类型不能为空）
     *   Step 2：防重复入场检查（同一车牌不能在同一停车场重复入场）
     *   Step 3：创建停车记录（status=0 表示"在场中"）
     *   Step 4：停车场已用车位数 +1
     *
     * @OperationLog：此次入场操作会被 AOP 切面自动记录到日志表
     * @param params 前端传来的 JSON 对象，包含 lotId、plateNumber、carType、entryImgUrl
     */
    @OperationLog(module = "车牌识别", action = "入场", description = "车辆入场登记")
    @PostMapping("/entry")
    public Result<?> entry(@RequestBody Map<String, Object> params) {

        // 从 Map 中取出各参数（前端传 JSON，Spring 解析成 Map<String, Object>）
        Long    lotId   = parseLong(params.get("lotId"));         // 停车场 ID
        String  plate   = parseStr(params.get("plateNumber"));    // 车牌号
        Integer carType = parseInteger(params.get("carType"));    // 车辆类型（1包月/2临时）
        String  imgUrl  = parseStr(params.get("entryImgUrl"));    // 入场图片 URL

        // ---- Step 1：参数校验 ----
        if (lotId == null)   return Result.error(400, "停车场不能为空");
        if (plate == null)   return Result.error(400, "车牌号不能为空");
        if (carType == null) return Result.error(400, "车辆类型不能为空");

        ParkLot lot = lotService.getById(lotId);
        if (lot == null) return Result.error(400, "停车场不存在");

        // ---- Step 2：防重复入场 ----
        // 查询同一停车场内是否已有该车牌"在场中（status=0）"的记录
        // 如果有，说明这辆车已经在场内，不能再次入场
        QueryWrapper<ParkRecord> eq = new QueryWrapper<>();
        eq.eq("plate_number", plate).eq("lot_id", lotId).eq("status", 0);
        if (recordService.getOne(eq) != null)
            return Result.error(400, "车辆【" + plate + "】已在本场内，请勿重复入场");

        // ---- Step 3：创建入场记录 ----
        ParkRecord record = new ParkRecord();
        record.setLotId(lotId);                 // 所属停车场
        record.setPlateNumber(plate);           // 车牌号
        record.setCarType(carType);             // 车辆类型
        record.setEntryTime(new Date());        // 入场时间 = 当前时间
        record.setEntryImgUrl(imgUrl);          // 入场图片 URL
        record.setStatus(0);                    // 状态 0 = 在场中（出场后改为 1）

        if (!recordService.save(record)) return Result.error(500, "入场登记失败");

        // ---- Step 4：停车场已用车位数 +1 ----
        // Math.min 防止超过总车位数（不能超过上限）
        int current = lot.getUsedSpaces() == null ? 0 : lot.getUsedSpaces();
        int total   = lot.getTotalSpaces() == null ? Integer.MAX_VALUE : lot.getTotalSpaces();
        lot.setUsedSpaces(Math.min(current + 1, total));
        lotService.updateById(lot);

        // 组装返回数据
        String typeText = carType == 1 ? "包月车" : "临时车";
        Map<String, Object> data = new HashMap<>();
        data.put("recordId",    record.getId());
        data.put("plateNumber", plate);
        data.put("carType",     carType);
        data.put("carTypeText", typeText);
        data.put("entryTime",   record.getEntryTime());

        return Result.success("入场成功，车辆类型：" + typeText, data);
    }


    // 接口三：车辆出场 POST /api/plate/exit


    /**
     * 【车辆出场结算接口】
     *
     * 车辆出场时调用此接口，自动计算停车时长和费用，
     * 更新停车记录状态，并为临时车生成待支付记录。
     *
     * 出场流程：
     *   Step 1：查找该车在本停车场的"在场中"记录
     *   Step 2：重新验证是否为有效包月车（出场时再查一次，防止中途续费或过期）
     *   Step 3：计算停车时长（分钟）和应缴费用（调用 calculateFee 方法）
     *   Step 4：更新停车记录（状态改为已出场，回写时长、金额、出场图片）
     *   Step 5：临时车且费用>0 时，生成待支付的缴费记录
     *   Step 6：停车场已用车位数 -1
     *
     * 为什么出场时要重新验证包月车？
     *   入场时判断是包月车，但包月车有可能在停车期间到期，
     *   出场时应该以出场时刻的状态为准，重新查一次更准确。
     *
     * @param params 前端传来的 JSON，包含 lotId、plateNumber、exitImgUrl
     */
    @OperationLog(module = "车牌识别", action = "出场", description = "车辆出场结算")
    @PostMapping("/exit")
    public Result<?> exit(@RequestBody Map<String, Object> params) {

        Long   lotId  = parseLong(params.get("lotId"));
        String plate  = parseStr(params.get("plateNumber"));
        String imgUrl = parseStr(params.get("exitImgUrl")); // 出场图片 URL

        if (lotId == null) return Result.error(400, "停车场不能为空");
        if (plate == null) return Result.error(400, "车牌号不能为空");

        ParkLot lot = lotService.getById(lotId);
        if (lot == null) return Result.error(400, "停车场不存在");

        // ---- Step 1：查找在场记录 ----
        // 条件：车牌号匹配 + 停车场匹配 + 状态为在场中(0)
        // orderByDesc("entry_time")：如果因为异常产生了多条，取最新的一条
        QueryWrapper<ParkRecord> q = new QueryWrapper<>();
        q.eq("plate_number", plate).eq("lot_id", lotId).eq("status", 0).orderByDesc("entry_time");
        ParkRecord record = recordService.getOne(q);
        if (record == null) return Result.error(400, "未找到车辆【" + plate + "】在本场的入场记录");

        // ---- Step 2：计算"今天零点"用于包月车有效期判断 ----
        // 原因：数据库存的到期日是 yyyy-MM-dd 格式（时间部分是 00:00:00）
        // 如果用 new Date()（当前时间如 15:30:00）与之比较，
        // 当天到期的包月车（到期日 00:00:00）会被误判为过期（因为 00:00:00 < 15:30:00）
        // 用今天零点比较：到期日(00:00:00) >= 今天零点(00:00:00) → 当天仍有效 ✓
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date todayStart = todayCal.getTime();

        // ---- Step 2（续）：重新验证是否为有效包月车 ----
        // 条件：停车场匹配 + 车牌匹配 + status=1(正常) + expire_date >= 今天零点（未过期）
        QueryWrapper<ParkMonthlyCar> mq = new QueryWrapper<>();
        mq.eq("lot_id", lotId)
          .eq("plate_number", plate)
          .eq("status", 1)
          .ge("expire_date", todayStart);  // ge = greater than or equal（大于等于）
        boolean isValidMonthlyCar = monthlyCarService.getOne(mq) != null;
        int actualCarType = isValidMonthlyCar ? 1 : 2; // 实际车辆类型（以出场时判断为准）

        // ---- Step 3：计算停车时长和费用 ----
        Date now         = new Date();
        // 停车分钟数 = (出场时间毫秒 - 入场时间毫秒) / 60000
        int  parkMinutes = (int) ((now.getTime() - record.getEntryTime().getTime()) / 60000);
        // 调用计费方法（包月车费用为0，临时车按规则计算）
        BigDecimal fee   = calculateFee(record.getEntryTime(), now, lot, actualCarType);

        // ---- Step 4：更新停车记录 ----
        record.setExitTime(now);             // 出场时间
        record.setExitImgUrl(imgUrl);        // 出场图片
        record.setStatus(1);                 // 状态改为 1（已出场）
        record.setParkingMinutes(parkMinutes); // 停车时长（分钟）
        record.setPayableAmount(fee);        // 应缴金额
        record.setCarType(actualCarType);    // 以出场时的实际类型为准（可能与入场时不同）

        if (!recordService.updateById(record)) return Result.error(500, "出场登记失败");

        // ---- Step 5：生成缴费记录（仅临时车且费用>0时）----
        Map<String, Object> data = new HashMap<>();
        if (actualCarType == 2 && fee.compareTo(BigDecimal.ZERO) > 0) {
            ParkPayment p = new ParkPayment();
            p.setRecordId(record.getId());   // 关联停车记录 ID
            p.setLotId(lotId);
            p.setPlateNumber(plate);
            p.setCarType(2);
            p.setAmount(fee);                // 应缴金额
            p.setPayTime(now);
            p.setPayStatus(0);               // payStatus=0 表示未支付（待用户点击确认支付）
            paymentService.save(p);
            data.put("paymentId", p.getId()); // 返回 paymentId，前端支付确认时用
        }

        // ---- Step 6：停车场已用车位数 -1 ----
        // Math.max 防止变成负数
        int current = lot.getUsedSpaces() == null ? 0 : lot.getUsedSpaces();
        lot.setUsedSpaces(Math.max(current - 1, 0));
        lotService.updateById(lot);

        String typeText = actualCarType == 1 ? "包月车" : "临时车";
        data.put("plateNumber",    plate);
        data.put("carType",        actualCarType);
        data.put("carTypeText",    typeText);
        data.put("entryTime",      record.getEntryTime());
        data.put("exitTime",       now);
        data.put("parkingMinutes", parkMinutes);
        data.put("amount",         fee);

        return Result.success(
            "出场成功，" + typeText + "，停车 " + parkMinutes + " 分钟，应缴 " + fee + " 元", data
        );
    }


    // 私有方法：停车费用计算（calculateFee）—— 答辩必讲！


    /**
     * 【停车费用计算】—— 计费核心逻辑
     *
     * 计费规则（四步判断，按顺序）：
     *   1. 包月车（carType=1）→ 直接返回 0 元，免费
     *   2. 停车总时长 ≤ 免费时长 → 返回 0 元，免费（免费停车时间内）
     *   3. 超出免费时长的部分，按"计费单元（unitMinutes）"向上取整计算单元数，
     *      再乘以"每单元单价（unitPrice）"，得出费用
     *   4. 如果当日费用超过"每日最高限额（maxFee）"，按当日封顶价收取
     *   5. 跨天停车时，每天独立计算并累加，避免停多天只收一天费用
     *
     * 【计算示例】
     *   停车场设置：单价 5元/小时（unitPrice=5），计费单元 60分钟（unitMinutes=60），
     *              免费时长 30分钟（freeMinutes=30），每日最高限额 50元（maxFee=50）
     *   车辆停车 160分钟：
     *     → 超出免费时长：160 - 30 = 130 分钟
     *     → 计费单元数（向上取整）：ceil(130 / 60) = ceil(2.17) = 3 个单元
     *     → 应缴费用：3 × 5 = 15 元
     *     → 15 元 < 50 元封顶，最终收费 15 元
     *
     *   车辆停车 3 天（每天均达封顶）：
     *     → 每天独立计算，每天最多 50 元
     *     → 最终收费：3 × 50 = 150 元
     *
     * 向上取整的意义：停了 61 分钟按 2 小时收，对停车场有利（行业惯例）。
     *
     * @param entry   入场时间
     * @param exit    出场时间
     * @param lot     停车场对象（包含所有收费参数）
     * @param carType 车辆类型（1=包月车，2=临时车）
     * @return 应缴费用（BigDecimal，保留2位小数，四舍五入）
     */
    private BigDecimal calculateFee(Date entry, Date exit, ParkLot lot, Integer carType) {
        // 包月车免费
        if (carType != null && carType == 1) return BigDecimal.ZERO;
        if (entry == null || exit == null)   return BigDecimal.ZERO;

        // 计算总停车分钟数
        long total = (exit.getTime() - entry.getTime()) / 60000;

        // 读取停车场的收费参数（null 时给合理默认值）
        int  free  = lot.getFreeMinutes()  == null ? 0  : lot.getFreeMinutes();   // 免费时长（分钟）
        int  unit  = (lot.getUnitMinutes() == null || lot.getUnitMinutes() <= 0)
                     ? 60 : lot.getUnitMinutes();                                  // 计费单元（默认60分钟）
        BigDecimal up = lot.getUnitPrice() == null ? BigDecimal.ZERO : lot.getUnitPrice(); // 单价
        BigDecimal mx = lot.getMaxFee();                                           // 每日封顶金额（可为null表示无上限）

        // 未超出免费时长，直接免费
        if (total <= free) return BigDecimal.ZERO;

        // 按日历天拆分停车时长，每天独立计费并应用当日封顶
        LocalDateTime entryDt = entry.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime exitDt = exit.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDate entryDate = entryDt.toLocalDate();
        LocalDate exitDate = exitDt.toLocalDate();

        long remainingFree = free;
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

            // 计算当日费用：超出部分按计费单元向上取整，再乘以单价
            long units = (chargeMinutes + unit - 1) / unit;
            BigDecimal dayFee = up.multiply(BigDecimal.valueOf(units));

            // 超过当日封顶限额则按封顶价收费
            if (mx != null && dayFee.compareTo(mx) > 0) dayFee = mx;

            totalFee = totalFee.add(dayFee);
        }

        // 保留2位小数，四舍五入（BigDecimal 精确计算，避免浮点误差）
        return totalFee.setScale(2, RoundingMode.HALF_UP);
    }

    // =====================================================================
    // 私有方法：保存图片
    // =====================================================================

    /**
     * 把上传的图片保存到服务器本地磁盘，返回可通过 HTTP 访问的 URL。
     *
     * 文件名格式：时间戳_随机4位数.扩展名，如 20250601153025_3847.jpg
     * 时间戳 + 随机数：防止同一秒内上传的图片文件名冲突。
     *
     * 保存路径：application.yml 里 upload.path 配置的目录（如 D:/parking-uploads/）
     * 访问 URL：http://localhost:8080/文件名（Spring Boot 把该目录映射为静态资源）
     *
     * @param file 上传的文件对象
     * @return 可访问的图片 URL，保存失败时返回 null
     */
    private String saveImage(MultipartFile file) {
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs(); // 目录不存在则创建（mkdirs 会同时创建父目录）

            // 取原始文件名的扩展名（如 .jpg、.png），保持文件格式
            String original  = file.getOriginalFilename();
            String ext       = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf(".")) : ".jpg";

            // 生成不重复的文件名：时间戳_随机4位数.扩展名
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName  = timestamp + "_" + (int)(Math.random() * 9000 + 1000) + ext;

            // 把文件字节流写入磁盘
            Files.write(Paths.get(uploadPath + fileName), file.getBytes());

            // 返回可通过 HTTP 访问的 URL
            return "http://localhost:8080/" + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            return null; // 图片保存失败不影响主流程，返回 null 即可
        }
    }

    // =====================================================================
    // 私有方法：获取百度 access_token
    // =====================================================================

    /**
     * 获取百度 API 的 access_token（带缓存）。
     *
     * 百度 OAuth2.0 认证流程（client_credentials 模式）：
     *   1. 用 apiKey（client_id）和 secretKey（client_secret）
     *      向百度 token 接口发 POST 请求
     *   2. 百度返回 access_token 和 expires_in（有效期，约2592000秒=30天）
     *   3. 拿着这个 token 去调用 OCR 识别接口
     *
     * 缓存策略：
     *   - 把 token 和 "过期时间戳" 存在内存变量里
     *   - 每次调用先判断是否过期，没过期直接返回缓存，过期了再重新申请
     *   - 提前 300 秒（5分钟）刷新，防止 token 恰好在使用时过期
     *
     * @return 有效的 access_token 字符串
     * @throws Exception 网络请求失败或百度返回异常时抛出
     */
    private String getAccessToken() throws Exception {
        // 检查缓存：token 不为空 且 当前时间 < 过期时间，直接返回缓存
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        // 缓存过期或没有缓存，重新向百度申请 token
        // grant_type=client_credentials：用应用的凭证（apiKey+secretKey）换 token（非用户授权）
        String url = "https://aip.baidubce.com/oauth/2.0/token"
                + "?grant_type=client_credentials"
                + "&client_id=" + apiKey
                + "&client_secret=" + secretKey;

        ResponseEntity<String> resp = restTemplate.postForEntity(url, null, String.class);
        JsonNode root = objectMapper.readTree(resp.getBody());

        if (!root.has("access_token"))
            throw new RuntimeException("百度未返回access_token，响应: " + resp.getBody());

        // 更新缓存
        cachedToken = root.get("access_token").asText();
        // expires_in 单位是秒，转成毫秒，提前 300 秒刷新（防止边界过期）
        tokenExpireTime = System.currentTimeMillis()
                + (root.path("expires_in").asLong(2592000) - 300) * 1000;

        return cachedToken;
    }

    // =====================================================================
    // 工具方法：安全类型转换（防止前端传来的类型不一致导致转换异常）
    // =====================================================================

    /**
     * 安全地把任意对象转为 Long，失败时返回 null（不抛异常）。
     * 前端传 JSON 时数字有时是 Integer 有时是 Long，用 toString 统一处理。
     */
    private Long parseLong(Object o) {
        try { return o == null ? null : Long.valueOf(o.toString()); }
        catch (Exception e) { return null; }
    }

    /**
     * 安全地把任意对象转为 Integer，失败时返回 null。
     */
    private Integer parseInteger(Object o) {
        try { return o == null ? null : Integer.valueOf(o.toString()); }
        catch (Exception e) { return null; }
    }

    /**
     * 安全地把任意对象转为 String，并去掉前后空格，空字符串返回 null。
     */
    private String parseStr(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
