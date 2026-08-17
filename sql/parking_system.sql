/*
 Navicat MySQL Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80036
 Source Host           : localhost:3306
 Source Schema         : parking_system

 Target Server Type    : MySQL
 Target Server Version : 80036
 File Encoding         : 65001

 Date: 03/07/2026 16:07:33
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for park_lot
-- ----------------------------
DROP TABLE IF EXISTS `park_lot`;
CREATE TABLE `park_lot`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '停车场ID',
  `lot_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '停车场名称',
  `total_spaces` int(0) NOT NULL COMMENT '车位总数',
  `used_spaces` int(0) NULL DEFAULT 0 COMMENT '已用临时车位(动态更新)',
  `monthly_fee` decimal(10, 2) NOT NULL COMMENT '包月费用(元/月)',
  `free_minutes` int(0) NOT NULL COMMENT '免费时长(分钟)',
  `unit_minutes` int(0) NOT NULL COMMENT '计费单位时长(分钟)',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '计费单价(元/单位时长)',
  `max_fee` decimal(10, 2) NOT NULL COMMENT '每日最大金额(元)',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度（GCJ-02）',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度（GCJ-02）',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `discounts` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '续费优惠规则JSON，如[\\"months\\":3,\\"discount\\":100}]',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '停车场信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of park_lot
-- ----------------------------
INSERT INTO `park_lot` VALUES (1, '黄山学院南区停车场', 500, 0, 300.00, 30, 60, 5.00, 60.00, '[{\"months\":6,\"discount\":80}]', '2026-03-22 16:12:44', '2026-03-22 16:12:44');
INSERT INTO `park_lot` VALUES (2, '第一人民医院', 100, 1, 220.00, 30, 60, 5.00, 50.00, '[{\"months\":3,\"discount\":50}]', '2026-03-27 18:30:02', '2026-03-27 18:30:02');
INSERT INTO `park_lot` VALUES (3, '镜湖小区', 400, 0, 200.00, 60, 60, 5.00, 50.00, NULL, '2026-04-04 16:56:20', '2026-04-04 16:56:20');
INSERT INTO `park_lot` VALUES (4, '西湖春天', 1000, 2, 250.00, 100, 60, 6.00, 70.00, NULL, '2026-04-04 16:57:43', '2026-04-04 16:57:43');
INSERT INTO `park_lot` VALUES (5, '微风游乐场', 300, 1, 0.00, 30, 60, 5.00, 50.00, NULL, '2026-04-04 16:59:31', '2026-04-04 16:59:31');
INSERT INTO `park_lot` VALUES (7, '第二人民医院', 100, 0, 300.00, 30, 60, 5.00, 50.00, '[{\"months\":5,\"discount\":50}]', '2026-05-03 13:21:01', '2026-05-03 13:21:01');

-- ----------------------------
-- Table structure for park_monthly_car
-- ----------------------------
DROP TABLE IF EXISTS `park_monthly_car`;
CREATE TABLE `park_monthly_car`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '包月记录ID',
  `lot_id` bigint(0) NOT NULL COMMENT '所属停车场ID',
  `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
  `owner_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车主姓名',
  `gender` tinyint(0) NULL DEFAULT 1 COMMENT '性别 (1男 2女 0未知)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号码',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '状态 (1正常 0欠费/过期)',
  `start_date` date NOT NULL COMMENT '生效日期',
  `expire_date` date NOT NULL COMMENT '有效期至',
  `space_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '已购车位编号(选填)',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plate_lot`(`plate_number`, `lot_id`) USING BTREE COMMENT '同一车场车牌唯一'
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '包月车辆信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of park_monthly_car
-- ----------------------------
INSERT INTO `park_monthly_car` VALUES (1, 2, '川G00M00', '王五', 1, '19956764545', 1, '2026-03-06', '2026-08-02', 'A-14', '2026-03-29 15:26:10', '2026-05-03 16:17:09');
INSERT INTO `park_monthly_car` VALUES (3, 3, '京AD06088', '李四', 1, '17776314587', 0, '2026-05-01', '2026-05-02', 'A-102', '2026-04-03 16:42:20', '2026-05-09 20:16:27');
INSERT INTO `park_monthly_car` VALUES (4, 1, '皖SKL789', '张三', 1, '18836459821', 0, '2026-04-04', '2026-05-30', 'B-12', '2026-04-04 15:14:29', '2026-07-03 15:28:15');
INSERT INTO `park_monthly_car` VALUES (5, 4, '皖SBN969', '王飞', 1, '17778564391', 0, '2026-05-02', '2026-06-02', 'A-77', '2026-05-02 14:55:19', '2026-07-03 15:28:15');
INSERT INTO `park_monthly_car` VALUES (7, 7, '皖S78456', '小王', 1, '17845612345', 1, '2026-05-03', '2027-03-03', 'A-78', '2026-05-03 13:32:53', '2026-05-03 13:32:53');

-- ----------------------------
-- Table structure for park_payment
-- ----------------------------
DROP TABLE IF EXISTS `park_payment`;
CREATE TABLE `park_payment`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `record_id` bigint(0) NULL DEFAULT NULL COMMENT '关联停车记录ID(包月续费可为空)',
  `lot_id` bigint(0) NOT NULL COMMENT '停车场ID',
  `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
  `car_type` tinyint(0) NOT NULL COMMENT '费用类型 (1包月续费 2临停缴费)',
  `amount` decimal(10, 2) NOT NULL COMMENT '支付金额(元)',
  `pay_method` tinyint(0) NULL DEFAULT 1 COMMENT '支付方式(1现金 2微信 3支付宝)',
  `pay_time` datetime(0) NOT NULL COMMENT '支付时间',
  `pay_status` tinyint(0) NULL DEFAULT 1 COMMENT '支付状态 (0未支付 1已支付)',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '缴费流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of park_payment
-- ----------------------------
INSERT INTO `park_payment` VALUES (6, 14, 1, '沪KR9888', 2, 0.00, 1, '2026-05-01 15:03:32', 1, '2026-05-01 15:03:31');
INSERT INTO `park_payment` VALUES (7, 15, 3, '京AD06088', 2, 0.00, 1, '2026-05-01 15:06:55', 1, '2026-05-01 15:06:54');
INSERT INTO `park_payment` VALUES (8, 16, 5, '京AD06088', 2, 0.00, 1, '2026-05-01 15:16:33', 1, '2026-05-01 15:16:32');
INSERT INTO `park_payment` VALUES (9, 17, 4, '京AD06088', 2, 24.00, 1, '2026-05-01 15:29:48', 1, '2026-05-01 15:29:48');
INSERT INTO `park_payment` VALUES (10, 19, 4, '京AD06088', 2, 24.00, 1, '2026-05-01 15:41:29', 1, '2026-05-01 15:41:29');
INSERT INTO `park_payment` VALUES (11, 22, 1, '京AD06088', 2, 25.00, 1, '2026-05-01 16:00:09', 1, '2026-05-01 15:59:57');
INSERT INTO `park_payment` VALUES (12, 23, 5, '沪KR9888', 2, 25.00, 1, '2026-05-01 16:01:49', 1, '2026-05-01 16:01:30');
INSERT INTO `park_payment` VALUES (13, 25, 3, '京AD06088', 2, 20.00, 1, '2026-05-03 14:49:51', 1, '2026-05-01 16:09:04');
INSERT INTO `park_payment` VALUES (14, NULL, 6, '浙AIU774', 1, 100.00, 1, '2026-05-02 15:11:38', 1, '2026-05-02 15:11:37');
INSERT INTO `park_payment` VALUES (15, NULL, 2, '京AD06088', 1, 610.00, 1, '2026-05-02 15:12:18', 1, '2026-05-02 15:12:17');
INSERT INTO `park_payment` VALUES (16, NULL, 6, '浙AIU774', 1, 500.00, 1, '2026-05-02 15:16:05', 1, '2026-05-02 15:16:05');
INSERT INTO `park_payment` VALUES (17, NULL, 7, '皖S78456', 1, 2900.00, 1, '2026-05-03 13:33:29', 1, '2026-05-03 13:33:28');
INSERT INTO `park_payment` VALUES (18, 28, 7, '沪KR9888', 2, 25.00, 1, '2026-05-03 13:51:12', 1, '2026-05-03 13:50:58');
INSERT INTO `park_payment` VALUES (19, 33, 2, '京AD06088', 2, 25.00, 1, '2026-05-03 16:20:43', 1, '2026-05-03 16:20:38');
INSERT INTO `park_payment` VALUES (20, 34, 3, '京AD06088', 2, 25.00, 1, '2026-05-03 16:22:18', 0, '2026-05-03 16:22:17');
INSERT INTO `park_payment` VALUES (21, NULL, 3, '京AD06088', 1, 200.00, 1, '2026-05-09 16:46:09', 1, '2026-05-09 16:46:08');
INSERT INTO `park_payment` VALUES (22, NULL, 2, 'A', 1, 220.00, 1, '2026-05-09 20:18:47', 1, '2026-05-09 20:18:46');

-- ----------------------------
-- Table structure for park_record
-- ----------------------------
DROP TABLE IF EXISTS `park_record`;
CREATE TABLE `park_record`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `lot_id` bigint(0) NOT NULL COMMENT '停车场ID',
  `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '车牌号',
  `car_type` tinyint(0) NOT NULL COMMENT '车辆类型 (1包月车 2临时车)',
  `entry_time` datetime(0) NOT NULL COMMENT '入场时间',
  `entry_img_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '入场照片路径/URL',
  `exit_time` datetime(0) NULL DEFAULT NULL COMMENT '出场时间',
  `exit_img_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出场照片路径/URL',
  `parking_minutes` int(0) NULL DEFAULT NULL COMMENT '停车时长(分钟)',
  `payable_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '应收金额',
  `status` tinyint(0) NULL DEFAULT 0 COMMENT '停车状态 (0场内 1已出场)',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plate_number`(`plate_number`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '车辆进出记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of park_record
-- ----------------------------
INSERT INTO `park_record` VALUES (21, 1, '京AD06088', 2, '2026-05-01 15:58:13', 'http://localhost:8080/20260501155812_7433.jpg', '2026-05-01 15:58:29', 'http://localhost:8080/20260501155812_7433.jpg', 0, 0.00, 1, '2026-05-01 15:58:13', '2026-05-01 15:58:13');
INSERT INTO `park_record` VALUES (23, 5, '沪KR9888', 2, '2026-05-01 11:00:55', 'http://localhost:8080/20260501160054_1538.jpg', '2026-05-01 16:01:30', 'http://localhost:8080/20260501160129_2856.jpg', 300, 25.00, 1, '2026-05-01 16:00:55', '2026-05-01 16:00:55');
INSERT INTO `park_record` VALUES (24, 3, '京AD06088', 2, '2026-05-01 16:06:32', 'http://localhost:8080/20260501160631_2214.jpg', '2026-05-01 16:06:39', 'http://localhost:8080/20260501160631_2214.jpg', 0, 0.00, 1, '2026-05-01 16:06:32', '2026-05-01 16:06:32');
INSERT INTO `park_record` VALUES (25, 3, '京AD06088', 2, '2026-05-01 11:08:36', 'http://localhost:8080/20260501160835_2240.jpg', '2026-05-01 16:09:05', 'http://localhost:8080/20260501160904_8521.jpg', 300, 20.00, 1, '2026-05-01 16:08:35', '2026-05-01 16:08:35');
INSERT INTO `park_record` VALUES (28, 7, '沪KR9888', 2, '2026-05-03 08:50:29', 'http://localhost:8080/20260503135028_3987.jpg', '2026-05-03 13:50:58', 'http://localhost:8080/20260503135057_2679.jpg', 300, 25.00, 1, '2026-05-03 13:50:29', '2026-05-03 13:50:29');
INSERT INTO `park_record` VALUES (31, 2, '川G00M00', 1, '2026-05-03 08:59:01', 'http://localhost:8080/20260503135900_3098.jpg', '2026-05-03 13:59:38', 'http://localhost:8080/20260503135937_5079.jpg', 300, 0.00, 1, '2026-05-03 13:59:01', '2026-05-03 13:59:01');
INSERT INTO `park_record` VALUES (33, 2, '京AD06088', 2, '2026-05-03 11:20:03', 'http://localhost:8080/20260503162001_9369.jpg', '2026-05-03 16:20:38', '', 300, 25.00, 1, '2026-05-03 16:20:02', '2026-05-03 16:20:02');
INSERT INTO `park_record` VALUES (34, 3, '京AD06088', 2, '2026-05-03 11:21:05', 'http://localhost:8080/20260503162104_7575.jpg', '2026-05-03 16:22:18', 'http://localhost:8080/20260503162217_2086.jpg', 301, 25.00, 1, '2026-05-03 16:21:05', '2026-05-03 16:21:05');
INSERT INTO `park_record` VALUES (39, 2, '川G00M00', 1, '2026-05-10 10:02:20', 'http://localhost:8080/20260510100219_9670.jpg', NULL, NULL, NULL, NULL, 0, '2026-05-10 10:02:19', '2026-05-10 10:02:19');

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作账号',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '功能模块（如：停车场管理）',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '操作类型（如：新增、编辑、删除）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作描述（如：新增停车场[黄山大学]）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法全路径',
  `request_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求URL',
  `request_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '操作结果 1成功 0失败',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `cost_time` int(0) NULL DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_username`(`username`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 185 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------
INSERT INTO `sys_operation_log` VALUES (1, 'admin', '王武', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 551, '2026-04-21 15:04:00');
INSERT INTO `sys_operation_log` VALUES (2, 'admin', '王武', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-04-21 15:04:36');
INSERT INTO `sys_operation_log` VALUES (3, 'admin', '王武', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 533, '2026-04-25 17:19:29');
INSERT INTO `sys_operation_log` VALUES (4, 'admin', '王武', '用户管理', '编辑', '编辑管理员信息', 'com.parking.smart_parking.controller.SysUserController.update', '/api/user/update', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-04-25 17:20:32');
INSERT INTO `sys_operation_log` VALUES (5, 'admin', '王武', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 3, '2026-04-25 17:20:52');
INSERT INTO `sys_operation_log` VALUES (6, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 528, '2026-04-28 12:08:29');
INSERT INTO `sys_operation_log` VALUES (7, 'admin', '武家乐', '车牌识别', '入场', '车辆入场登记', 'com.parking.smart_parking.controller.PlateRecognizeController.entry', '/api/plate/entry', '0:0:0:0:0:0:0:1', 1, NULL, 28, '2026-04-28 12:22:22');
INSERT INTO `sys_operation_log` VALUES (8, 'admin', '武家乐', '车牌识别', '出场', '车辆出场结算', 'com.parking.smart_parking.controller.PlateRecognizeController.exit', '/api/plate/exit', '0:0:0:0:0:0:0:1', 1, NULL, 20, '2026-04-28 12:22:56');
INSERT INTO `sys_operation_log` VALUES (9, 'admin', '武家乐', '车牌识别', '入场', '车辆入场登记', 'com.parking.smart_parking.controller.PlateRecognizeController.entry', '/api/plate/entry', '0:0:0:0:0:0:0:1', 1, NULL, 29, '2026-04-28 13:00:09');
INSERT INTO `sys_operation_log` VALUES (10, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-04-28 13:00:52');
INSERT INTO `sys_operation_log` VALUES (11, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-04-28 13:01:12');
INSERT INTO `sys_operation_log` VALUES (12, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/11', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-04-28 13:04:55');
INSERT INTO `sys_operation_log` VALUES (13, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/9', '0:0:0:0:0:0:0:1', 1, NULL, 5, '2026-04-28 13:04:57');
INSERT INTO `sys_operation_log` VALUES (14, 'admin', '武家乐', '车牌识别', '入场', '车辆入场登记', 'com.parking.smart_parking.controller.PlateRecognizeController.entry', '/api/plate/entry', '0:0:0:0:0:0:0:1', 1, NULL, 28, '2026-04-28 13:05:20');
INSERT INTO `sys_operation_log` VALUES (15, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-04-28 13:05:39');
INSERT INTO `sys_operation_log` VALUES (16, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 19, '2026-04-28 13:06:02');
INSERT INTO `sys_operation_log` VALUES (17, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 550, '2026-04-30 18:55:10');
INSERT INTO `sys_operation_log` VALUES (18, 'admin', '武家乐', '车牌识别', '入场', '车辆入场登记', 'com.parking.smart_parking.controller.PlateRecognizeController.entry', '/api/plate/entry', '0:0:0:0:0:0:0:1', 1, NULL, 28, '2026-04-30 19:36:52');
INSERT INTO `sys_operation_log` VALUES (19, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-04-30 19:37:11');
INSERT INTO `sys_operation_log` VALUES (20, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 19, '2026-04-30 19:37:16');
INSERT INTO `sys_operation_log` VALUES (21, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 85, '2026-05-01 14:57:25');
INSERT INTO `sys_operation_log` VALUES (22, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/13', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 14:57:45');
INSERT INTO `sys_operation_log` VALUES (23, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 25, '2026-05-01 15:02:24');
INSERT INTO `sys_operation_log` VALUES (24, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 21, '2026-05-01 15:03:32');
INSERT INTO `sys_operation_log` VALUES (25, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 15:05:22');
INSERT INTO `sys_operation_log` VALUES (26, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 18, '2026-05-01 15:06:55');
INSERT INTO `sys_operation_log` VALUES (27, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-01 15:16:18');
INSERT INTO `sys_operation_log` VALUES (28, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-01 15:16:33');
INSERT INTO `sys_operation_log` VALUES (29, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-01 15:21:31');
INSERT INTO `sys_operation_log` VALUES (30, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 107, '2026-05-01 15:28:40');
INSERT INTO `sys_operation_log` VALUES (31, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-01 15:28:53');
INSERT INTO `sys_operation_log` VALUES (32, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-01 15:29:08');
INSERT INTO `sys_operation_log` VALUES (33, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 15:29:37');
INSERT INTO `sys_operation_log` VALUES (34, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-05-01 15:29:48');
INSERT INTO `sys_operation_log` VALUES (35, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/16', '127.0.0.1', 1, NULL, 13, '2026-05-01 15:38:45');
INSERT INTO `sys_operation_log` VALUES (36, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '127.0.0.1', 1, NULL, 7, '2026-05-01 15:39:10');
INSERT INTO `sys_operation_log` VALUES (37, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '127.0.0.1', 1, NULL, 12, '2026-05-01 15:39:51');
INSERT INTO `sys_operation_log` VALUES (38, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '127.0.0.1', 1, NULL, 16, '2026-05-01 15:40:01');
INSERT INTO `sys_operation_log` VALUES (39, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '127.0.0.1', 1, NULL, 9, '2026-05-01 15:40:55');
INSERT INTO `sys_operation_log` VALUES (40, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '127.0.0.1', 1, NULL, 7, '2026-05-01 15:41:10');
INSERT INTO `sys_operation_log` VALUES (41, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '127.0.0.1', 1, NULL, 19, '2026-05-01 15:41:29');
INSERT INTO `sys_operation_log` VALUES (42, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 15:48:14');
INSERT INTO `sys_operation_log` VALUES (43, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-01 15:48:24');
INSERT INTO `sys_operation_log` VALUES (44, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 20, '2026-05-01 15:58:13');
INSERT INTO `sys_operation_log` VALUES (45, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-01 15:58:29');
INSERT INTO `sys_operation_log` VALUES (46, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/8', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 15:58:43');
INSERT INTO `sys_operation_log` VALUES (47, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/12', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-01 15:58:44');
INSERT INTO `sys_operation_log` VALUES (48, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 15:59:23');
INSERT INTO `sys_operation_log` VALUES (49, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-01 15:59:36');
INSERT INTO `sys_operation_log` VALUES (50, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 18, '2026-05-01 15:59:58');
INSERT INTO `sys_operation_log` VALUES (51, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/11', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-01 16:00:09');
INSERT INTO `sys_operation_log` VALUES (52, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/10', '0:0:0:0:0:0:0:1', 1, NULL, 5, '2026-05-01 16:00:37');
INSERT INTO `sys_operation_log` VALUES (53, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/17', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 16:00:38');
INSERT INTO `sys_operation_log` VALUES (54, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/18', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-01 16:00:40');
INSERT INTO `sys_operation_log` VALUES (55, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/19', '0:0:0:0:0:0:0:1', 1, NULL, 5, '2026-05-01 16:00:42');
INSERT INTO `sys_operation_log` VALUES (56, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/22', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-01 16:00:44');
INSERT INTO `sys_operation_log` VALUES (57, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/14', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-01 16:00:45');
INSERT INTO `sys_operation_log` VALUES (58, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/15', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-01 16:00:46');
INSERT INTO `sys_operation_log` VALUES (59, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/20', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-01 16:00:48');
INSERT INTO `sys_operation_log` VALUES (60, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-01 16:00:55');
INSERT INTO `sys_operation_log` VALUES (61, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 16:01:04');
INSERT INTO `sys_operation_log` VALUES (62, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-01 16:01:30');
INSERT INTO `sys_operation_log` VALUES (63, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/12', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-01 16:01:49');
INSERT INTO `sys_operation_log` VALUES (64, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-01 16:06:07');
INSERT INTO `sys_operation_log` VALUES (65, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-01 16:06:19');
INSERT INTO `sys_operation_log` VALUES (66, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-01 16:06:32');
INSERT INTO `sys_operation_log` VALUES (67, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 16:06:39');
INSERT INTO `sys_operation_log` VALUES (68, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-01 16:08:36');
INSERT INTO `sys_operation_log` VALUES (69, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 16:08:49');
INSERT INTO `sys_operation_log` VALUES (70, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 19, '2026-05-01 16:09:05');
INSERT INTO `sys_operation_log` VALUES (71, 'admin', '武家乐', '用户管理', '编辑', '超管重置用户密码', 'com.parking.smart_parking.controller.SysUserController.resetPassword', '/api/user/resetPassword', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-01 16:10:51');
INSERT INTO `sys_operation_log` VALUES (72, 'admin', '武家乐', '角色管理', '新增', '新增角色', 'com.parking.smart_parking.controller.SysRoleController.add', '/api/role/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-01 16:12:54');
INSERT INTO `sys_operation_log` VALUES (73, 'admin', '武家乐', '角色管理', '编辑', '编辑角色权限', 'com.parking.smart_parking.controller.SysRoleController.update', '/api/role/update', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-01 16:13:09');
INSERT INTO `sys_operation_log` VALUES (74, 'admin', '武家乐', '用户管理', '新增', '新增管理员', 'com.parking.smart_parking.controller.SysUserController.add', '/api/user/add', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-01 16:13:56');
INSERT INTO `sys_operation_log` VALUES (75, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-05-01 16:14:17');
INSERT INTO `sys_operation_log` VALUES (76, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 1, '2026-05-01 16:15:37');
INSERT INTO `sys_operation_log` VALUES (77, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-01 16:15:52');
INSERT INTO `sys_operation_log` VALUES (78, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 76, '2026-05-02 14:17:27');
INSERT INTO `sys_operation_log` VALUES (79, 'admin', '武家乐', '角色管理', '删除', '删除角色', 'com.parking.smart_parking.controller.SysRoleController.delete', '/api/role/delete/4', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-02 14:22:00');
INSERT INTO `sys_operation_log` VALUES (80, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-05-02 14:30:14');
INSERT INTO `sys_operation_log` VALUES (81, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-05-02 14:41:46');
INSERT INTO `sys_operation_log` VALUES (82, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-02 14:55:19');
INSERT INTO `sys_operation_log` VALUES (83, 'admin', '武家乐', '停车场管理', '新增', '新增停车场', 'com.parking.smart_parking.controller.ParkLotController.add', '/park-lot/add', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-02 15:02:14');
INSERT INTO `sys_operation_log` VALUES (84, 'admin', '武家乐', '停车场管理', '编辑', '编辑停车场信息', 'com.parking.smart_parking.controller.ParkLotController.update', '/park-lot/update', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-02 15:02:26');
INSERT INTO `sys_operation_log` VALUES (85, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 24, '2026-05-02 15:09:39');
INSERT INTO `sys_operation_log` VALUES (86, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-05-02 15:10:45');
INSERT INTO `sys_operation_log` VALUES (87, 'admin', '武家乐', '车辆管理', '续费', '包月车续费', 'com.parking.smart_parking.controller.ParkMonthlyCarController.renew', '/monthly-car/renew', '0:0:0:0:0:0:0:1', 1, NULL, 20, '2026-05-02 15:11:38');
INSERT INTO `sys_operation_log` VALUES (88, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/14', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-02 15:11:38');
INSERT INTO `sys_operation_log` VALUES (89, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-05-02 15:12:04');
INSERT INTO `sys_operation_log` VALUES (90, 'admin', '武家乐', '车辆管理', '续费', '包月车续费', 'com.parking.smart_parking.controller.ParkMonthlyCarController.renew', '/monthly-car/renew', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-02 15:12:18');
INSERT INTO `sys_operation_log` VALUES (91, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/15', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-02 15:12:18');
INSERT INTO `sys_operation_log` VALUES (92, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 21, '2026-05-02 15:13:08');
INSERT INTO `sys_operation_log` VALUES (93, 'admin', '武家乐', '车辆管理', '续费', '包月车续费', 'com.parking.smart_parking.controller.ParkMonthlyCarController.renew', '/monthly-car/renew', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-05-02 15:16:05');
INSERT INTO `sys_operation_log` VALUES (94, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/16', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-02 15:16:05');
INSERT INTO `sys_operation_log` VALUES (95, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 85, '2026-05-03 13:10:51');
INSERT INTO `sys_operation_log` VALUES (96, 'admin', '武家乐', '停车场管理', '删除', '删除停车场', 'com.parking.smart_parking.controller.ParkLotController.delete', '/park-lot/delete/6', '0:0:0:0:0:0:0:1', 1, NULL, 11, '2026-05-03 13:11:36');
INSERT INTO `sys_operation_log` VALUES (97, 'admin', '武家乐', '车辆管理', '删除', '删除包月车记录', 'com.parking.smart_parking.controller.ParkMonthlyCarController.delete', '/monthly-car/delete/6', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 13:11:49');
INSERT INTO `sys_operation_log` VALUES (98, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 3, '2026-05-03 13:14:42');
INSERT INTO `sys_operation_log` VALUES (99, 'admin', '武家乐', '停车场管理', '新增', '新增停车场', 'com.parking.smart_parking.controller.ParkLotController.add', '/park-lot/add', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-03 13:21:01');
INSERT INTO `sys_operation_log` VALUES (100, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 11, '2026-05-03 13:32:53');
INSERT INTO `sys_operation_log` VALUES (101, 'admin', '武家乐', '车辆管理', '续费', '包月车续费', 'com.parking.smart_parking.controller.ParkMonthlyCarController.renew', '/monthly-car/renew', '0:0:0:0:0:0:0:1', 1, NULL, 21, '2026-05-03 13:33:29');
INSERT INTO `sys_operation_log` VALUES (102, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/17', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 13:33:29');
INSERT INTO `sys_operation_log` VALUES (103, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 3, '2026-05-03 13:43:34');
INSERT INTO `sys_operation_log` VALUES (104, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 13:44:47');
INSERT INTO `sys_operation_log` VALUES (105, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/26', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 13:45:04');
INSERT INTO `sys_operation_log` VALUES (106, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 13:46:14');
INSERT INTO `sys_operation_log` VALUES (107, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-05-03 13:46:45');
INSERT INTO `sys_operation_log` VALUES (108, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-03 13:50:29');
INSERT INTO `sys_operation_log` VALUES (109, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 13:50:46');
INSERT INTO `sys_operation_log` VALUES (110, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 19, '2026-05-03 13:50:58');
INSERT INTO `sys_operation_log` VALUES (111, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/18', '0:0:0:0:0:0:0:1', 1, NULL, 11, '2026-05-03 13:51:12');
INSERT INTO `sys_operation_log` VALUES (112, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-03 13:52:49');
INSERT INTO `sys_operation_log` VALUES (113, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 13:55:35');
INSERT INTO `sys_operation_log` VALUES (114, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/29', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-03 13:55:47');
INSERT INTO `sys_operation_log` VALUES (115, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 13:56:38');
INSERT INTO `sys_operation_log` VALUES (116, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 13:57:40');
INSERT INTO `sys_operation_log` VALUES (117, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/30', '0:0:0:0:0:0:0:1', 1, NULL, 5, '2026-05-03 13:58:13');
INSERT INTO `sys_operation_log` VALUES (118, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-03 13:59:01');
INSERT INTO `sys_operation_log` VALUES (119, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 13:59:19');
INSERT INTO `sys_operation_log` VALUES (120, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-03 13:59:38');
INSERT INTO `sys_operation_log` VALUES (121, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 1, '2026-05-03 14:43:23');
INSERT INTO `sys_operation_log` VALUES (122, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-03 14:46:00');
INSERT INTO `sys_operation_log` VALUES (123, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-03 14:46:23');
INSERT INTO `sys_operation_log` VALUES (124, 'admin', '武家乐', '用户管理', '编辑', '超管重置用户密码', 'com.parking.smart_parking.controller.SysUserController.resetPassword', '/api/user/resetPassword', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 14:46:38');
INSERT INTO `sys_operation_log` VALUES (125, 'admin', '武家乐', '用户管理', '编辑', '超管重置用户密码', 'com.parking.smart_parking.controller.SysUserController.resetPassword', '/api/user/resetPassword', '0:0:0:0:0:0:0:1', 1, NULL, 3, '2026-05-03 14:48:03');
INSERT INTO `sys_operation_log` VALUES (126, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 1, '2026-05-03 14:48:18');
INSERT INTO `sys_operation_log` VALUES (127, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-03 14:48:47');
INSERT INTO `sys_operation_log` VALUES (128, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-03 14:49:22');
INSERT INTO `sys_operation_log` VALUES (129, 'wuye', '小枫物业', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 14:49:44');
INSERT INTO `sys_operation_log` VALUES (130, 'wuye', '小枫物业', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/13', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-03 14:49:51');
INSERT INTO `sys_operation_log` VALUES (131, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 2, '2026-05-03 14:50:02');
INSERT INTO `sys_operation_log` VALUES (132, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 76, '2026-05-03 15:40:16');
INSERT INTO `sys_operation_log` VALUES (133, 'admin', '武家乐', '缴费记录', '删除', '删除缴费记录', 'com.parking.smart_parking.controller.ParkPaymentController.delete', '/api/payment/delete/1', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 15:52:19');
INSERT INTO `sys_operation_log` VALUES (134, 'admin', '武家乐', '缴费记录', '删除', '删除缴费记录', 'com.parking.smart_parking.controller.ParkPaymentController.delete', '/api/payment/delete/2', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 15:52:20');
INSERT INTO `sys_operation_log` VALUES (135, 'admin', '武家乐', '缴费记录', '删除', '删除缴费记录', 'com.parking.smart_parking.controller.ParkPaymentController.delete', '/api/payment/delete/3', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 15:52:22');
INSERT INTO `sys_operation_log` VALUES (136, 'admin', '武家乐', '缴费记录', '删除', '删除缴费记录', 'com.parking.smart_parking.controller.ParkPaymentController.delete', '/api/payment/delete/4', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 15:52:25');
INSERT INTO `sys_operation_log` VALUES (137, 'admin', '武家乐', '缴费记录', '删除', '删除缴费记录', 'com.parking.smart_parking.controller.ParkPaymentController.delete', '/api/payment/delete/5', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 15:52:27');
INSERT INTO `sys_operation_log` VALUES (138, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 21, '2026-05-03 16:00:34');
INSERT INTO `sys_operation_log` VALUES (139, 'admin', '武家乐', '用户管理', '删除', '删除管理员', 'com.parking.smart_parking.controller.SysUserController.delete', '/api/user/delete/4', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 16:02:38');
INSERT INTO `sys_operation_log` VALUES (140, 'admin', '武家乐', '角色管理', '删除', '删除角色', 'com.parking.smart_parking.controller.SysRoleController.delete', '/api/role/delete/3', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 16:02:47');
INSERT INTO `sys_operation_log` VALUES (141, 'admin', '武家乐', '角色管理', '新增', '新增角色', 'com.parking.smart_parking.controller.SysRoleController.add', '/api/role/add', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-03 16:03:15');
INSERT INTO `sys_operation_log` VALUES (142, 'admin', '武家乐', '用户管理', '新增', '新增管理员', 'com.parking.smart_parking.controller.SysUserController.add', '/api/user/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 16:04:03');
INSERT INTO `sys_operation_log` VALUES (143, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-03 16:14:09');
INSERT INTO `sys_operation_log` VALUES (144, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 16:17:10');
INSERT INTO `sys_operation_log` VALUES (145, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-03 16:20:03');
INSERT INTO `sys_operation_log` VALUES (146, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 11, '2026-05-03 16:20:32');
INSERT INTO `sys_operation_log` VALUES (147, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 19, '2026-05-03 16:20:38');
INSERT INTO `sys_operation_log` VALUES (148, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/19', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-03 16:20:43');
INSERT INTO `sys_operation_log` VALUES (149, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 16:21:05');
INSERT INTO `sys_operation_log` VALUES (150, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-03 16:21:44');
INSERT INTO `sys_operation_log` VALUES (151, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 17, '2026-05-03 16:22:18');
INSERT INTO `sys_operation_log` VALUES (152, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 16:23:32');
INSERT INTO `sys_operation_log` VALUES (153, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-03 16:23:48');
INSERT INTO `sys_operation_log` VALUES (154, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/32', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 16:23:54');
INSERT INTO `sys_operation_log` VALUES (155, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 16:24:09');
INSERT INTO `sys_operation_log` VALUES (156, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 18, '2026-05-03 16:28:33');
INSERT INTO `sys_operation_log` VALUES (157, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-05-03 16:31:07');
INSERT INTO `sys_operation_log` VALUES (158, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/36', '0:0:0:0:0:0:0:1', 1, NULL, 8, '2026-05-03 16:31:09');
INSERT INTO `sys_operation_log` VALUES (159, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-03 16:31:32');
INSERT INTO `sys_operation_log` VALUES (160, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/37', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-03 16:31:44');
INSERT INTO `sys_operation_log` VALUES (161, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-03 16:31:56');
INSERT INTO `sys_operation_log` VALUES (162, 'admin', '武家乐', '停车记录', '修改入场时间', '调整车辆入场时间（演示用）', 'com.parking.smart_parking.controller.ParkRecordController.updateEntryTime', '/api/record/update-entry-time', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-05-03 16:32:08');
INSERT INTO `sys_operation_log` VALUES (163, 'admin', '武家乐', '停车记录', '出场', '手动出场登记', 'com.parking.smart_parking.controller.ParkRecordController.exit', '/api/record/exit', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-03 16:32:19');
INSERT INTO `sys_operation_log` VALUES (164, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/27', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-03 16:33:04');
INSERT INTO `sys_operation_log` VALUES (165, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 82, '2026-05-04 14:14:31');
INSERT INTO `sys_operation_log` VALUES (166, 'admin', '武家乐', '角色管理', '编辑', '编辑角色权限', 'com.parking.smart_parking.controller.SysRoleController.update', '/api/role/update', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-04 14:17:36');
INSERT INTO `sys_operation_log` VALUES (167, 'admin', '武家乐', '角色管理', '编辑', '编辑角色权限', 'com.parking.smart_parking.controller.SysRoleController.update', '/api/role/update', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-04 14:17:41');
INSERT INTO `sys_operation_log` VALUES (168, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 75, '2026-05-06 16:46:29');
INSERT INTO `sys_operation_log` VALUES (169, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 109, '2026-05-09 16:41:19');
INSERT INTO `sys_operation_log` VALUES (170, 'admin', '武家乐', '车辆管理', '续费', '包月车续费', 'com.parking.smart_parking.controller.ParkMonthlyCarController.renew', '/monthly-car/renew', '0:0:0:0:0:0:0:1', 1, NULL, 30, '2026-05-09 16:46:09');
INSERT INTO `sys_operation_log` VALUES (171, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/21', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-09 16:46:09');
INSERT INTO `sys_operation_log` VALUES (172, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 45, '2026-05-09 20:14:28');
INSERT INTO `sys_operation_log` VALUES (173, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 22, '2026-05-09 20:15:10');
INSERT INTO `sys_operation_log` VALUES (174, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-05-09 20:16:27');
INSERT INTO `sys_operation_log` VALUES (175, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/35', '0:0:0:0:0:0:0:1', 1, NULL, 6, '2026-05-09 20:16:41');
INSERT INTO `sys_operation_log` VALUES (176, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 13, '2026-05-09 20:18:22');
INSERT INTO `sys_operation_log` VALUES (177, 'admin', '武家乐', '车辆管理', '续费', '包月车续费', 'com.parking.smart_parking.controller.ParkMonthlyCarController.renew', '/monthly-car/renew', '0:0:0:0:0:0:0:1', 1, NULL, 16, '2026-05-09 20:18:47');
INSERT INTO `sys_operation_log` VALUES (178, 'admin', '武家乐', '缴费记录', '支付', '确认支付', 'com.parking.smart_parking.controller.ParkPaymentController.pay', '/api/payment/pay/22', '0:0:0:0:0:0:0:1', 1, NULL, 10, '2026-05-09 20:18:47');
INSERT INTO `sys_operation_log` VALUES (179, 'admin', '武家乐', '车辆管理', '删除', '删除包月车记录', 'com.parking.smart_parking.controller.ParkMonthlyCarController.delete', '/monthly-car/delete/8', '0:0:0:0:0:0:0:1', 1, NULL, 14, '2026-05-09 20:18:59');
INSERT INTO `sys_operation_log` VALUES (180, 'admin', '武家乐', '车辆管理', '新增', '新增/编辑包月车', 'com.parking.smart_parking.controller.ParkMonthlyCarController.save', '/monthly-car/save', '0:0:0:0:0:0:0:1', 1, NULL, 9, '2026-05-09 20:19:35');
INSERT INTO `sys_operation_log` VALUES (181, 'admin', '武家乐', '车辆管理', '删除', '删除包月车记录', 'com.parking.smart_parking.controller.ParkMonthlyCarController.delete', '/monthly-car/delete/9', '0:0:0:0:0:0:0:1', 1, NULL, 15, '2026-05-09 20:19:48');
INSERT INTO `sys_operation_log` VALUES (182, 'admin', '武家乐', '停车记录', '删除', '删除停车记录', 'com.parking.smart_parking.controller.ParkRecordController.delete', '/api/record/delete/38', '0:0:0:0:0:0:0:1', 1, NULL, 7, '2026-05-09 20:26:13');
INSERT INTO `sys_operation_log` VALUES (183, 'admin', '武家乐', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 192, '2026-05-10 09:02:38');
INSERT INTO `sys_operation_log` VALUES (184, '', '', '系统登录', '登录', '用户登录系统', 'com.parking.smart_parking.controller.LoginController.login', '/api/auth/login', '0:0:0:0:0:0:0:1', 1, NULL, 12, '2026-05-10 09:59:35');
INSERT INTO `sys_operation_log` VALUES (185, 'admin', '武家乐', '停车记录', '入场', '手动新增入场记录', 'com.parking.smart_parking.controller.ParkRecordController.add', '/api/record/add', '0:0:0:0:0:0:0:1', 1, NULL, 23, '2026-05-10 10:02:20');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称(如:超级管理员,普通管理员)',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符(如: SUPER_ADMIN, ADMIN)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色说明',
  `permissions` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单权限，逗号分隔，如 park-lot,monthly-car',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'SUPER_ADMIN', NULL, NULL, '2026-03-20 15:11:28', '2026-03-20 15:11:28');
INSERT INTO `sys_role` VALUES (2, '普通管理员', 'ADMIN', '', 'read|park-lot,monthly-car,plate-recognize,payment,record', '2026-03-20 15:11:28', '2026-03-20 15:11:28');
INSERT INTO `sys_role` VALUES (4, '用户', 'YONGHU', '用户使用', 'read|park-lot,monthly-car', '2026-05-01 16:12:53', '2026-05-01 16:12:53');
INSERT INTO `sys_role` VALUES (5, '物业', 'WUYE', '物业专用', 'edit|park-lot,plate-recognize,payment,record,monthly-car', '2026-05-03 16:03:14', '2026-05-03 16:03:14');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码(建议MD5或BCrypt加密)',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `role_id` bigint(0) NOT NULL COMMENT '关联角色ID',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '账号状态 (1正常 0停用)',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NORMAL_ADMIN',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '123456', '武家乐', '13647878989', 1, 1, '2026-03-20 15:11:28', '2026-03-26 17:14:03', 'SUPER_ADMIN', NULL);
INSERT INTO `sys_user` VALUES (2, '111', '123456', '小李', '18796469812', 2, 1, '2026-03-29 15:28:04', '2026-03-29 15:28:04', 'ADMIN', NULL);
INSERT INTO `sys_user` VALUES (3, '459147', '123456', '张三', '15546728155', 2, 1, '2026-04-17 14:10:13', '2026-04-17 14:10:13', 'ADMIN', NULL);
INSERT INTO `sys_user` VALUES (5, 'yonghu', '123456', '张伟', '18945667812', 4, 1, '2026-05-01 16:13:55', '2026-05-01 16:13:55', 'YONGHU', NULL);
INSERT INTO `sys_user` VALUES (6, 'wuye', '123456', '小枫物业', '19978456512', 5, 1, '2026-05-03 16:04:03', '2026-05-03 16:04:03', 'WUYE', NULL);

SET FOREIGN_KEY_CHECKS = 1;
