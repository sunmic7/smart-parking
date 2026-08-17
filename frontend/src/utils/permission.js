/**
 * 【权限工具】utils/permission.js —— 判断当前用户对指定菜单是否有编辑权限
 *
 * 本系统有两种角色：
 *   SUPER_ADMIN（超级管理员）：拥有所有菜单的完整权限，可以增删改查
 *   ADMIN（普通管理员）：权限由超级管理员在"角色管理"页面配置，
 *                       每个菜单可单独设置为"无权限"、"只读"或"可编辑"
 *
 * 权限数据怎么存的？
 * 后端把角色的权限字符串返回给前端，新版格式如：
 *   "park-lot:edit,monthly-car:read,park-lot-map:read"
 * 前端 Layout.vue 的 loadPermissions() 把它解析成 { key: mode } 对象，
 * 并存入 localStorage 的 permMap 中。
 *
 * 使用场景（各页面中）：
 *   import { canEdit } from '../utils/permission'
 *   if (!canEdit('park-lot')) { // 隐藏新增/编辑/删除按钮 }
 */

/**
 * 判断当前用户对指定菜单是否有"编辑"权限。
 *
 * 逻辑：
 *   超级管理员（SUPER_ADMIN）：始终返回 true，不受权限设置限制
 *   普通管理员：读取 localStorage 里的 permMap 字段，
 *              如果对应菜单的 mode 为 'edit' 返回 true，否则返回 false
 *
 * @param {string} key 菜单标识，如 'park-lot'、'monthly-car'
 * @returns {boolean} true=有编辑权限，false=只读或无权限
 */
export function canEdit(key) {
  // 超级管理员直接放行
  if (localStorage.getItem('role') === 'SUPER_ADMIN') return true

  // 读取并解析 per-menu 权限映射
  try {
    const permMap = JSON.parse(localStorage.getItem('permMap') || '{}')
    return permMap[key] === 'edit'
  } catch {
    return false
  }
}

/**
 * 判断当前用户是否能看到某个菜单（有 read 或 edit 权限）。
 * Layout.vue 里主要用 hasPerm() 控制菜单显示，这里也提供一个便捷方法。
 *
 * @param {string} key 菜单标识
 * @returns {boolean}
 */
export function hasPerm(key) {
  if (localStorage.getItem('role') === 'SUPER_ADMIN') return true

  try {
    const permMap = JSON.parse(localStorage.getItem('permMap') || '{}')
    return permMap[key] === 'edit' || permMap[key] === 'read'
  } catch {
    return false
  }
}
