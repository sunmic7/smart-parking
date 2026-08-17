package com.parking.smart_parking.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.parking.smart_parking.entity.ParkMonthlyCar;
import com.parking.smart_parking.service.IParkMonthlyCarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

/**
 * 【定时任务】MonthlyCarExpireTask.java —— 包月车自动到期处理
 *
 * 功能：每隔 60 秒自动扫描一次数据库，
 * 将"有效期已过"的包月车状态从"正常(1)"改为"已过期(0)"。
 *
 * 定时任务
 * 包月车有一个到期日（expire_date）。到期后应该自动变成临时车收费。
 * 但如果每次查询时才去判断是否过期，需要在很多地方写重复逻辑，容易漏掉。
 * 用定时任务统一处理，数据库里的 status 字段始终是准确的，
 * 其他地方只需要判断 status == 1 即可，逻辑清晰。
 *
 * 工作原理
 * 1. Application.java 上的 @EnableScheduling 开启定时任务功能
 * 2. 本类加 @Component，被 Spring 扫描注册为 Bean
 * 3. autoExpire() 方法上的 @Scheduled(fixedRate = 60_000) 告诉 Spring：
 *    每隔 60000 毫秒（60秒）执行一次这个方法
 *
 * 处理包月车过期其他方式
 *   可以在每次"出场结算"时实时判断包月车是否过期，
 *       本项目出场接口（PlateRecognizeController.exit）就有重新查包月车表的逻辑，
 *       双重保险，既有定时任务更新 status，又在业务关键节点实时验证。
 *
 * @Component：把这个类注册成 Spring Bean，才能被 Spring 的定时任务调度器管理
 */
@Component
public class MonthlyCarExpireTask {

    /**
     * 注入包月车业务服务，用于执行数据库更新操作。
     */
    @Autowired
    private IParkMonthlyCarService monthlyCarService;

    /**
     * 【自动过期方法】每分钟执行一次
     *
     * @Scheduled(fixedRate = 60_000)：
     *   fixedRate = 固定频率，单位毫秒。
     *   60_000 就是 60000 毫秒 = 60 秒（Java 数字可以用下划线分隔增加可读性）。
     *   表示"上一次任务开始执行后，隔 60 秒再次执行"（不管上次是否已经结束）。
     *   如果用 fixedDelay，则是"上一次任务执行完毕后，再等 60 秒"。
     *
     * 执行逻辑：
     *   找到所有 expire_date < 今天零点 且 status = 1 的包月车，
     *   把它们的 status 批量更新为 0（已过期）。
     *   一条 UPDATE SQL 搞定，不需要逐条处理，效率高。
     */
    @Scheduled(fixedRate = 60_000)
    public void autoExpire() {

        // ---- 计算"今天零点"时间 ----
        // 为什么用今天零点，而不是 new Date()（当前时间）？
        //
        // 场景：某包月车的 expire_date = 2025-06-01（数据库存的是 2025-06-01 00:00:00）
        //
        // 如果用当前时间（比如今天下午 15:30:00）去比较：
        //   expire_date(00:00:00) < 当前时间(15:30:00) → 判断为过期 ✗ 错误！
        //   因为今天 6月1日 整天都应该是有效的，不应该在下午被判成过期。
        //
        // 如果用今天零点（2025-06-01 00:00:00）去比较：
        //   expire_date(00:00:00) < 今天零点(00:00:00) → 不满足 < 条件 → 不过期 ✓ 正确！
        //   只有 expire_date = 2025-05-31 00:00:00（昨天）时，才会 < 今天零点，才判断为过期。
        //
        // 这个判断标准与出场接口（PlateRecognizeController.exit）保持一致，逻辑统一。
        Calendar cal = Calendar.getInstance();          // 获取当前时间的 Calendar 对象
        cal.set(Calendar.HOUR_OF_DAY, 0);               // 小时设为 0
        cal.set(Calendar.MINUTE, 0);                    // 分钟设为 0
        cal.set(Calendar.SECOND, 0);                    // 秒设为 0
        cal.set(Calendar.MILLISECOND, 0);               // 毫秒设为 0
        Date todayStart = cal.getTime();                // 今天 00:00:00

        // ---- 构建更新条件（UpdateWrapper）----
        // UpdateWrapper 是 MyBatis-Plus 的条件构造器（专用于 UPDATE）
        // 等效的 SQL：
        //   UPDATE park_monthly_car
        //   SET status = 0
        //   WHERE expire_date < '2025-06-01 00:00:00' AND status = 1
        UpdateWrapper<ParkMonthlyCar> wrapper = new UpdateWrapper<>();
        wrapper.lt("expire_date", todayStart)   // lt = less than，即 expire_date < 今天零点
               .eq("status", 1);               // 只更新"正常"状态的记录（避免重复更新已过期的）
        wrapper.set("status", 0);              // 把符合条件的记录的 status 改为 0（已过期）

        // ---- 执行批量更新 ----
        // update() 方法对应 SQL 的 UPDATE，会返回受影响的行数（这里不关心行数）
        monthlyCarService.update(wrapper);

        // 如果想知道本次过期了几条记录，可以用：
        // int count = monthlyCarService.update(wrapper); （实际返回 boolean，不是 int）
        // 若需要行数，可改用 sysOperationLogMapper 的 update 方法
    }
}
