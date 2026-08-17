package com.parking.smart_parking.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.smart_parking.entity.ParkRecord;
import com.parking.smart_parking.mapper.ParkRecordMapper;
import com.parking.smart_parking.service.IParkRecordService;
import org.springframework.stereotype.Service;

@Service
public class ParkRecordServiceImpl extends ServiceImpl<ParkRecordMapper, ParkRecord> implements IParkRecordService {
}