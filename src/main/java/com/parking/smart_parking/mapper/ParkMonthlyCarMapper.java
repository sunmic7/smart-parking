package com.parking.smart_parking.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
// 假设你之前建了 ParkMonthlyCar 实体类，如果没有，先建一个对应 park_monthly_car 表的空实体
import com.parking.smart_parking.entity.ParkMonthlyCar;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ParkMonthlyCarMapper extends BaseMapper<ParkMonthlyCar> {}
