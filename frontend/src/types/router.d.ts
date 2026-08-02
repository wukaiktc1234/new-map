import 'vue-router';
import type { UserRole } from '@/stores/permission';

declare module 'vue-router' {
  interface RouteMeta {
    /** 路由标题
 */
    title?: string;
    /** 路由图标
 */
    icon?: string;
    /** 权限标识列表（满足其一即可）
 */
    permissions?: string[];
    /** 可访问角色列表（满足其一即可）
 */
    roles?: UserRole[];
    /** 菜单类型: 0-目录, 1-菜单 2-详情
 */
    menuType?: 0 | 1 | 2;
    /** 菜单分类
 */
    category?: string;
    /** 是否隐藏菜单
 */
    hidden?: boolean;
    /** 是否公开页面（无需登录
 */
    public?: boolean;
    /** 是否固定在标签栏
 */
    affix?: boolean;
    /** 是否缓存页面
 */
    keepAlive?: boolean;
    /** 外链地址
 */
    link?: string;
    /** 权限域标识（用于路由级权限守卫，如 'product'/'finance'/'hr' 等）
     * 路由守卫通过此字段查询域矩阵，判断当前角色是否有权访问
     */
    domain?: string;
  }
}
