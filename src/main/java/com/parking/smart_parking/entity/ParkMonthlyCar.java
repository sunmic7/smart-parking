package com.parking.smart_parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

@TableName("park_monthly_car")
public class ParkMonthlyCar {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lotId;
    private String plateNumber;
    private String ownerName;
    private Integer gender;
    private String phone;
    private Integer status;

    // 【修复】pattern 改为 yyyy-MM-dd，与前端 el-date-picker value-format="YYYY-MM-DD" 完全对应
    // 原来是 yyyy-MM-dd HH:mm:ss，导致前端只传日期时 Jackson 反序列化失败，Spring 直接返回 400
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date expireDate;

    private String spaceNumber;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String lotName;

    // 以下两个废弃字段保留以防其他地方引用，不再使用
    @TableField(exist = false)
    private String purchasedSpace;

    @TableField(exist = false)
    private Date expireTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date expireDate) { this.expireDate = expireDate; }

    public String getSpaceNumber() { return spaceNumber; }
    public void setSpaceNumber(String spaceNumber) { this.spaceNumber = spaceNumber; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getLotName() { return lotName; }
    public void setLotName(String lotName) { this.lotName = lotName; }

    public String getPurchasedSpace() { return purchasedSpace; }
    public void setPurchasedSpace(String purchasedSpace) { this.purchasedSpace = purchasedSpace; }

    public Date getExpireTime() { return expireTime; }
    public void setExpireTime(Date expireTime) { this.expireTime = expireTime; }
}