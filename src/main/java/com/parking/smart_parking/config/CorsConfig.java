package com.parking.smart_parking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 【跨域配置类】CorsConfig.java —— 解决前后端分离的"跨域"问题
 *
 * 跨域
 * 浏览器有一个安全策略叫"同源策略"（Same-Origin Policy）：
 * 只有"协议 + 域名 + 端口"完全相同的请求，浏览器才允许直接访问。
 *
 * 本项目中：
 *   前端运行在 http://localhost:5173（Vue 开发服务器，端口 5173）
 *   后端运行在 http://localhost:8080（Spring Boot，端口 8080）
 *
 * 端口不同 → 不同源 → 浏览器会拦截前端发出的请求，报错：
 *   "Access to XMLHttpRequest at 'http://localhost:8080/...'
 *    from origin 'http://localhost:5173' has been blocked by CORS policy"
 *
 * 解决方案：后端在响应头里告诉浏览器"我允许这个来源访问"，
 * 浏览器看到这个响应头后就放行了。这就是 CORS（跨域资源共享）。
 *
 * 其他跨域方式
 *   1. Nginx 反向代理（把前后端统一代理到同一个端口，从根本上消除跨域）
 *       2. 前端配置开发代理（vite.config.js 里配置 proxy，仅开发环境有效）
 *       本项目用后端配置 CORS，最直接且生产环境同样有效。
 *
 * @Configuration：标记这是一个配置类，Spring 启动时会加载并执行其中的配置方法。
 *
 * implements WebMvcConfigurer：
 *   通过实现这个接口并重写 addCorsMappings 方法，
 *   来定制 Spring MVC 的跨域规则。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置 CORS 跨域规则。
     * Spring 启动时会自动调用这个方法，把配置的规则应用到所有请求上。
     * @param registry 跨域注册器，往里面添加规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
            // 对所有路径生效（"/**" 是通配符，匹配 /api/xxx、/login 等所有路径）
            .addMapping("/**")

            // 允许所有来源访问（"*" 通配，开发阶段方便调试）
            // 生产环境建议改成具体的前端域名，如 "https://parking.yourdomain.com"
            .allowedOriginPatterns("*")

            // 允许的 HTTP 请求方法
            // GET：查询数据；POST：新增数据；PUT：修改数据；DELETE：删除数据；OPTIONS：预检请求
            // 浏览器在发真实请求前，会先发一个 OPTIONS 预检请求，必须允许
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

            // 允许携带认证信息（如请求头中的 Authorization token）
            // 如果不设置为 true，前端带 token 的请求会被拦截
            .allowCredentials(true)

            // 预检请求（OPTIONS）的缓存时间（秒）
            // 3600 秒 = 1小时，意思是浏览器 1小时内不需要再发预检请求
            // 减少不必要的网络开销
            .maxAge(3600);
    }
}
