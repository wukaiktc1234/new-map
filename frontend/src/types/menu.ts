// 统一菜单数据格式定义
export interface MenuMeta {
  title: string;
  icon?: string;
  hidden?: boolean;
  roles?: string[];
  keepAlive?: boolean;
  menuType?: number;
  category?: string;
  requireAuth?: boolean;
  permissions?: string[];
}

export interface MenuItem {
  id: string | number;
  name: string;
  path: string;
  component?: string;
  redirect?: string;
  meta: MenuMeta;
  children?: MenuItem[];
  permission?: string;
  sort?: number;
  parentId?: string | number;
  menuType?: number;
  status?: string;
}

export interface MenuResponse {
  menus: MenuItem[];
}

// 后端菜单数据接口
export interface BackendMenuItem {
  id: string | number;
  name: string;
  path?: string;
  component?: string;
  redirect?: string;
  meta?: {
    title?: string;
    icon?: string;
    hidden?: boolean;
    keepAlive?: boolean;
    menuType?: number;
    roles?: string[];
    permissions?: string[];
    category?: string;
  };
  icon?: string;
  hidden?: boolean;
  keepAlive?: boolean;
  menuType?: number;
  children?: BackendMenuItem[];
  permission?: string;
  sort?: number;
  parentId?: string | number;
  status?: string;
}

// 从后端菜单数据转换为前端菜单数据的函
export const convertBackendMenuToFrontend = (backendMenus: BackendMenuItem[]): MenuItem[] => {

  return backendMenus.map(menu => {
    return {
      id: menu.id,
      name: menu.name,
      path: menu.path || '',
      component: menu.component,
      redirect: menu.redirect,
      meta: {
        title: menu.meta?.title || menu.name,
        icon: menu.meta?.icon || menu.icon || '',
        hidden: menu.meta?.hidden || menu.hidden || false,
        keepAlive: menu.meta?.keepAlive || menu.keepAlive || false,
        menuType: menu.meta?.menuType || menu.menuType || 0,
        roles: menu.meta?.roles || [],
        requireAuth: true,
        permissions: menu.meta?.permissions || [],
        category: menu.meta?.category,
      },
      children: menu.children ? convertBackendMenuToFrontend(menu.children) : undefined,
      permission: menu.permission,
      sort: menu.sort || 0,
      parentId: menu.parentId,
      status: menu.status || 'active'
    };
  });
};
