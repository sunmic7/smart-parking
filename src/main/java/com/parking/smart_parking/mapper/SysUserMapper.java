package com.parking.smart_parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.smart_parking.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // BaseMapper 已经包含了单表查询的全部方法
}