package com.parking.smart_parking.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parking.smart_parking.entity.ParkLot;
import com.parking.smart_parking.mapper.ParkLotMapper;
import com.parking.smart_parking.service.IParkLotService;
import org.springframework.stereotype.Service;

@Service
/*spring注解，业务层组件要交给 Spring 管理，之后在 Controller 里用 @Autowired 注入 IParkLotService 时，
Spring 会自动创建 ParkLotServiceImpl 的实例*/
public class ParkLotServiceImpl extends ServiceImpl<ParkLotMapper, ParkLot> implements IParkLotService {
}
/* extends ServiceImpl<ParkLotMapper, ParkLot>：继承 MyBatis-Plus 提供的通用实现，
它内部自动持有 ParkLotMapper，所有继承来的方法底层都通过这个 Mapper 操作数据库

implements IParkLotService：告诉 Spring 这个类是 IParkLotService 接口的实现，
Controller 里 @Autowired IParkLotService 时 Spring 会自动注入这个实现类

@Service：把这个类注册为 Spring Bean，@Autowired 才能找到它
 */