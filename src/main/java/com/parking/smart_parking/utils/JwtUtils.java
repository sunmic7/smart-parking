package com.parking.smart_parking.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

/**
 * =====================================================================
 * 【JWT 工具类】JwtUtils.java —— 登录认证的核心工具
 * =====================================================================
 *
 * JWT（JSON Web Token）是本系统实现"登录认证"的方式。
 *
 * 【通俗理解】
 * 把 JWT 想象成一张"电影票"：
 *   - 用户登录成功 → 影院（服务器）给你一张盖了章的票（token）
 *   - 你每次进场（每次请求）都出示这张票
 *   - 工作人员（服务器）验一下票是不是真的、有没有过期
 *   - 票上还写了你的姓名（用户名）和座位号（用户ID），不需要再查花名册（数据库）
 *
 * 【JWT 的结构】
 * token 是一个用英文点号分隔的三段字符串：xxxxx.yyyyy.zzzzz
 *   第一段 Header（头部）：声明算法类型，如 {"typ":"JWT","alg":"HS256"}
 *   第二段 Payload（载荷）：存放数据，如用户ID、用户名、过期时间（Base64编码，不加密）
 *   第三段 Signature（签名）：用密钥对前两段内容签名，防止内容被篡改
 *
 * 【为什么比 Session 好？】
 * Session 需要服务器存状态（每个登录用户都要在内存里保存一条记录），
 * 多台服务器时还需要共享 Session，比较麻烦。
 * JWT 是无状态的，服务器不存任何东西，token 由客户端（前端 localStorage）保管，
 * 天然支持分布式部署。
 *
 * JWT 和 Session 的区别
 *   Session 是服务器存状态，JWT 是客户端存状态（无状态认证）。
 *       JWT 不依赖服务器内存，适合前后端分离和分布式系统。
 *       缺点是 token 一旦签发无法主动失效（除非让它过期）。
 */
public class JwtUtils {

    /**
     * Token 有效期：24小时（单位：毫秒）。
     *
     * 计算方式：24（小时）× 60（分钟）× 60（秒）× 1000（毫秒）
     * 超过这个时间后，token 自动失效，用户需要重新登录。
     */
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000;

    /**
     * 签名密钥（对称加密密钥）。
     *
     * 这个密钥用来对 token 的第三段（Signature）进行签名和验证。
     * 只要密钥不泄露，别人就无法伪造合法的 token。
     *
     * 注意：生产环境中这个密钥不应该写死在代码里，
     * 应该放在 application.yml 或环境变量中，防止代码泄露导致密钥暴露。
     */
    private static final String SECRET_KEY = "ParkingSystemSecretKeyForGraduationProject";

    /**
     * 【生成 Token】
     *
     * 用户登录验证通过后调用此方法，生成一个包含用户信息的 token。
     * 生成的 token 会返回给前端，前端存入 localStorage。
     *
     * Token 里存了什么：
     *   - subject（主题）：用户名，标识这个 token 是谁的
     *   - userId（自定义字段）：用户ID，业务接口需要用到时可以从 token 解析出来
     *   - issuedAt：签发时间
     *   - expiration：过期时间（当前时间 + 24小时）
     *
     * 链式调用说明（builder 模式）：
     *   Jwts.builder() 创建一个构建器，
     *   每个 .setXxx() 或 .claim() 都是在往 token 里加内容，
     *   最后 .compact() 把三段内容拼成字符串返回。
     *
     * @param userId   用户的数据库主键 ID
     * @param username 用户名（登录账号）
     * @return 生成的 JWT 字符串，类似：eyJhbGciOiJIUzI1NiJ9.xxx.xxx
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();                                         // 当前时间（签发时间）
        Date expiration = new Date(now.getTime() + EXPIRE_TIME);       // 过期时间 = 现在 + 24小时

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")          // 声明 token 类型为 JWT
                .setSubject(username)                   // Payload 中的 subject 字段，存用户名
                .claim("userId", userId)                // 自定义字段，存用户 ID，供后续业务使用
                .setIssuedAt(now)                       // 签发时间
                .setExpiration(expiration)              // 过期时间，解析时自动校验
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)  // 用 HS256 算法 + 密钥签名
                .compact();                             // 生成最终的 token 字符串
    }

    /**
     * 【解析 Token】
     *
     * 每次前端请求时，从请求头 Authorization 取出 token，调用此方法解析。
     * 解析成功返回 Claims 对象（可从中取出 userId、username 等信息），
     * 解析失败（token 无效、已过期、被篡改）返回 null。
     *
     * 验证过程（jjwt 库自动完成）：
     *   1. 用密钥重新计算签名，和 token 第三段对比，判断是否被篡改
     *   2. 取出 expiration 字段，和当前时间对比，判断是否过期
     *   以上任一验证失败，都会抛出异常，catch 后返回 null。
     *
     * 调用方拿到 null 时，应该返回 401 错误，前端跳转到登录页。
     *
     * 取出用户ID的示例：
     *   Claims claims = JwtUtils.parseToken(token);
     *   if (claims == null) { // token 无效，拒绝访问 }
     *   Long userId = claims.get("userId", Long.class); // 取出用户ID
     *
     * @param token 前端请求头中携带的 JWT 字符串
     * @return Claims 对象（含 userId、username 等），token 无效时返回 null
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)   // 设置验签密钥（必须与生成时一致）
                    .parseClaimsJws(token)       // 解析并验证 token（签名校验 + 过期校验）
                    .getBody();                  // 返回 Payload 部分（Claims 对象）
        } catch (Exception e) {
            // 解析失败的三种情况：
            // 1. SignatureException：签名不匹配，token 被篡改
            // 2. ExpiredJwtException：token 已过期（超过24小时）
            // 3. MalformedJwtException：token 格式不对（不是合法的 JWT）
            return null; // 统一返回 null，调用方据此判断"未登录或登录已过期"
        }
    }
}
