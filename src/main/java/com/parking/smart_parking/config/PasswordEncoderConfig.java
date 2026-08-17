package com.parking.smart_parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

/*
BCrypt 改造， mvnw compile 编译。


- pom.xml 增加 spring-security-crypto 依赖
（只引入 crypto，不引入完整 security starter，避免改变现有 JWT 认证流程）。

- 新增 PasswordEncoderConfig.java 提供 BCryptPasswordEncoder Bean。

- LoginController.java 登录验证改为 passwordEncoder.matches(...) ，
并兼容旧明文密码：检测到不是 BCrypt 哈希时按明文比对，比对成功自动更新为 BCrypt 哈希。

- SysUserController.java 在新增用户、用户改密、超管重置密码时用
passwordEncoder.encode(...) 加密后落库。

- SysUser.java 给 getPassword() 加 @JsonIgnore ，避免用户列表/详情把密码返回前端。
部署后已有明文密码的用户仍可正常登录一次，系统会自动把密码升级为 BCrypt；

 */