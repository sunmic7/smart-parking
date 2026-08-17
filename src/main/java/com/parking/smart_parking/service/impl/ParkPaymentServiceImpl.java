package com.parking.smart_parking.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.smart_parking.entity.ParkPayment;
import com.parking.smart_parking.mapper.ParkPaymentMapper;
import com.parking.smart_parking.service.IParkPaymentService;
import org.springframework.stereotype.Service;

@Service
public class ParkPaymentServiceImpl extends ServiceImpl<ParkPaymentMapper, ParkPayment> implements IParkPaymentService {
}