package com.parking.smart_parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.smart_parking.entity.ParkLot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkLotMapper extends BaseMapper<ParkLot> {
}


/*mapper只有接口，不用写 SQL。继承了 MyBatis-Plus 的 BaseMapper
"项目采用了标准的 MVC 三层架构，
controller 负责接收请求和返回响应，service 负责业务逻辑，mapper 负责数据访问，便于维护和扩展。"

前端 （通过axios发送HTTP请求,返回Json格式)
   ↓
Controller（接收请求，调 service，返回 Result 对象）
   ↓
Service/impl（写业务逻辑，处理计算，（MyBatis-Plus 提供，自动注入了 mapper））
   ↓
Mapper（操作数据库，返回数据）
   ↓
Service/impl（封装处理结果）
   ↓
Controller（封装成HTTP响应）
   ↓
  前端

出场结算中
PlateRecognizeController.exit() 调用 recordService.getOne()
recordService 实际是 ParkRecordServiceImpl 的实例
ParkRecordServiceImpl 内部调用 ParkRecordMapper.selectOne()
Mapper 去 MySQL 查数据，返回 ParkRecord 对象

Axios调用controller
 */