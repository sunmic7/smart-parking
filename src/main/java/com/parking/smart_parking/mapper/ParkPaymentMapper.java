package com.parking.smart_parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.smart_parking.entity.ParkPayment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkPaymentMapper extends BaseMapper<ParkPayment> {
}