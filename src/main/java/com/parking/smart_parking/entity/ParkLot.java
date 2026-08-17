package com.parking.smart_parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

@TableName("park_lot") /*告诉 MyBatis-Plus 这个类对应哪张数据库表；*/
public class ParkLot {

    @TableId(type = IdType.AUTO)  /*表示主键 id 自增，不用手动填。*/
    private Long id;

    private String lotName;
    private Integer totalSpaces;
    private Integer usedSpaces;
    private BigDecimal monthlyFee;
    private Integer freeMinutes;
    private Integer unitMinutes;
    private BigDecimal unitPrice;
    private BigDecimal maxFee;

    /** 经度（GCJ-02 坐标系） */
    private BigDecimal longitude;

    /** 纬度（GCJ-02 坐标系） */
    private BigDecimal latitude;

    /** 详细地址 */
    private String address;

    /**
     * 续费优惠规则，JSON 字符串，最多 3 条，格式：
     * [{"months":3,"discount":100},{"months":6,"discount":300}]
     * months：续费月数；discount：优惠金额（元）
     */
    private String discounts;

    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLotName() { return lotName; }
    public void setLotName(String lotName) { this.lotName = lotName; }

    public Integer getTotalSpaces() { return totalSpaces; }
    public void setTotalSpaces(Integer totalSpaces) { this.totalSpaces = totalSpaces; }

    public Integer getUsedSpaces() { return usedSpaces; }
    public void setUsedSpaces(Integer usedSpaces) { this.usedSpaces = usedSpaces; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public Integer getFreeMinutes() { return freeMinutes; }
    public void setFreeMinutes(Integer freeMinutes) { this.freeMinutes = freeMinutes; }

    public Integer getUnitMinutes() { return unitMinutes; }
    public void setUnitMinutes(Integer unitMinutes) { this.unitMinutes = unitMinutes; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getMaxFee() { return maxFee; }
    public void setMaxFee(BigDecimal maxFee) { this.maxFee = maxFee; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDiscounts() { return discounts; }
    public void setDiscounts(String discounts) { this.discounts = discounts; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}