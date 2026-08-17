package com.parking.smart_parking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("park_record")
public class ParkRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lotId;
    private String plateNumber;
    private Integer carType; // 1包月车 2临时车

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date entryTime;

    private String entryImgUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date exitTime;

    private String exitImgUrl;

    /** 停车时长（分钟），出场时自动计算并回写 */
    private Integer parkingMinutes;

    /** 应收金额（元），出场时自动计算并回写 */
    private BigDecimal payableAmount;

    private Integer status; // 0场内 1已出场

    @TableField(exist = false)
    private String lotName;  // 非数据库字段，查询时动态填充

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    // ---------- 手动 getter/setter（保留原有风格，@Data 已生成，此处显式保留供参考） ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public Integer getCarType() { return carType; }
    public void setCarType(Integer carType) { this.carType = carType; }

    public Date getEntryTime() { return entryTime; }
    public void setEntryTime(Date entryTime) { this.entryTime = entryTime; }

    public String getEntryImgUrl() { return entryImgUrl; }
    public void setEntryImgUrl(String entryImgUrl) { this.entryImgUrl = entryImgUrl; }

    public Date getExitTime() { return exitTime; }
    public void setExitTime(Date exitTime) { this.exitTime = exitTime; }

    public String getExitImgUrl() { return exitImgUrl; }
    public void setExitImgUrl(String exitImgUrl) { this.exitImgUrl = exitImgUrl; }

    public Integer getParkingMinutes() { return parkingMinutes; }
    public void setParkingMinutes(Integer parkingMinutes) { this.parkingMinutes = parkingMinutes; }

    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getLotName() { return lotName; }
    public void setLotName(String lotName) { this.lotName = lotName; }
}