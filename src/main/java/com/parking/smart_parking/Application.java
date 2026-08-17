package com.parking.smart_parking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.parking.smart_parking.mapper")
@EnableScheduling
/**
 * 【启动类】Application.java —— 整个后端项目的总开关
 * 这是 Spring Boot 项目的入口类，运行 main 方法即可启动整个后端服务。
 * 启动后，后端会监听 8080 端口，等待前端的 HTTP 请求。
 *
 *  @SpringBootApplication  启动 Spring Boot
 *    这是一个"组合注解"，相当于同时开启了三件事：
 *    1. @SpringBootConfiguration：标记这是 Spring Boot 的配置类
 *    2. @EnableAutoConfiguration：自动配置（比如检测到 MySQL 依赖就自动配置数据源）
 *    3. @ComponentScan：自动扫描当前包及子包下所有带 @Component、@Service、
 *       @Controller、@Repository 等注解的类，注册为 Spring Bean（交给 Spring 管理）
 *
 *  @MapperScan("com.parking.smart_parking.mapper")   告诉程序去哪里找数据库操作类
 *    告诉 MyBatis-Plus：去这个包下面找所有的 Mapper 接口，自动为它们生成
 *    实现类并注册到 Spring 容器里。
 *    如果没有这个注解，@Autowired 注入 Mapper 时会报"找不到 Bean"的错误。
 *
 *  @EnableScheduling    开启Spring定时任务功能
 *    只有加了这个注解，MonthlyCarExpireTask 里的 @Scheduled 定时任务才会生效。
 *    本项目用它来实现"每分钟自动检查包月车是否到期"的功能。
 */
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
/**
 * 程序入口方法
 * SpringApplication.run() 会做以下事情：
 *  1. 创建 Spring 容器（ApplicationContext）
 *  2. 扫描并注册所有 Bean（Controller、Service、Mapper 等）
 *  3. 启动内嵌的 Tomcat 服务器，监听 application.yml 中配置的 8080 端口
 *  4. 执行所有自动配置（数据库连接、MyBatis-Plus、跨域配置等）
 *
 * @param args 命令行参数（一般不传，忽略即可）
 */