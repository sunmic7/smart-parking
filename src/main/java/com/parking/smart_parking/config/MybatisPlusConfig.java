package com.parking.smart_parking.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 【MyBatis-Plus 配置类】MybatisPlusConfig.java —— 开启分页功能
 * MyBatis-Plus 是 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变。
 * 它内置了大量常用的 CRUD 方法（save、getById、list、removeById 等），
 * 不需要手写 SQL 就能完成大部分数据库操作。
 *
 * 注册"分页插件"。
   分页显示
 * MyBatis-Plus 提供了 Page 对象来做分页：
 *   Page<ParkRecord> page = new Page<>(pageNum, pageSize); // 第几页，每页几条
 *   recordService.page(page, queryWrapper);                // 自动加 LIMIT 分页
 *
 * 但如果不配置分页插件，MyBatis-Plus 的 page() 方法不会自动加 SQL 的 LIMIT 子句，
 * 会把所有数据全查出来，再在内存里截取——对大数据量来说性能极差。
 *
 * 配置了 PaginationInnerInterceptor 后，MyBatis-Plus 会在执行 SQL 前
 * 自动在语句末尾加上 LIMIT offset, size，真正在数据库层面分页。
 *
 * 分页查询
 *   用 MyBatis-Plus 的 Page 对象，配置了分页插件后，
 *       调用 service.page(page, wrapper) 即可，框架自动处理 LIMIT 语句，
 *       返回结果包含总记录数（total）、总页数、当前页数据等信息。
 */
@Configuration
public class MybatisPlusConfig {
    /*
     * 注册 MyBatis-Plus 拦截器（包含分页插件）。
     * @Bean：标记这个方法的返回值会被注册为 Spring Bean，
     *       Spring 启动时会调用这个方法并把返回的拦截器对象放入容器。
     *
     * MybatisPlusInterceptor 是一个拦截器容器，
     * 可以往里面加多种"内部拦截器"（插件）：
     *   - PaginationInnerInterceptor：分页插件（本项目用到）
     *   - OptimisticLockerInnerInterceptor：乐观锁插件
     *   - BlockAttackInnerInterceptor：防止全表更新/删除
     *   等等...
     *
     * @return 配置好的拦截器对象
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件，指定数据库类型为 MySQL
        // 不同数据库（MySQL、Oracle、PostgreSQL 等）的分页 SQL 语法不同，
        // 指定 DbType.MYSQL 让插件生成正确的 LIMIT 语句（MySQL 语法）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
