package com.parking.smart_parking.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.smart_parking.entity.ParkMonthlyCar;
import com.parking.smart_parking.mapper.ParkMonthlyCarMapper;
import com.parking.smart_parking.service.IParkMonthlyCarService;
import org.springframework.stereotype.Service;

@Service
public class ParkMonthlyCarServiceImpl extends ServiceImpl<ParkMonthlyCarMapper, ParkMonthlyCar> implements IParkMonthlyCarService {
}