package com.parking.smart_parking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parking.smart_parking.entity.ParkLot;

public interface IParkLotService extends IService<ParkLot> {
}
/* service定义能做什么（功能菜单），impl定义怎么做
本项目的业务逻辑全部写在 Controller 里，Service 层只起到"传递"作用，所以接口和实现类里面没有任何自定义方法，全靠继承来的方法工作。
例save(entity)    INSERT
saveOrUpdate(entity)   有id→UPDATE，无id→INSERT
removeById(id)    DELETE WHERE id=?*/