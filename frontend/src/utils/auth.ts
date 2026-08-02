// Token 管理
import logger from "./logger";

const TOKEN_KEY = "token";
const REFRESH_TOKEN_KEY = "refresh_token";
const TOKEN_EXPIRY_KEY = "token_expiry";

interface JwtPayload {
  exp?: number;
  userId?: string | number;
  username?: string;
  sub?: string;
  email?: string;
  roles?: string[];
  permissions?: string[];
}

interface UserInfo {
  id: string | number | null;
  username: string | null;
  roles: string[];
  permissions: string[];
}

/**
 * 获取登录状态和JWT Token
 */
export const getToken = (): string | null => {
  const token = localStorage.getItem(TOKEN_KEY);
  return token;
};

/**
 * 设置登录状态和JWT Token
 */
export const setToken = (token: string): void => {
  // 存储真实JWT Token
  localStorage.setItem(TOKEN_KEY, token);

  // 如果需要解析过期时间，可以尝试解析传入token
  try {
    const payload = parseJwt(token);
    if (payload && payload.exp) {
      localStorage.setItem(TOKEN_EXPIRY_KEY, payload.exp.toString());
    }
  } catch {
    // 解析失败时忽略，不影响token存储
  }
};

/**
 * 移除登录状
 */
export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem("access_token"); // 兼容旧版
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
};

/**
 * 获取刷新 token
 */
export const getRefreshToken = (): string | null => {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
};

/**
 * 设置刷新 token
 */
export const setRefreshToken = (token: string): void => {
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
};

/**
 * 移除刷新 token
 */
export const removeRefreshToken = (): void => {
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

/**
 * 获取 token 过期时间
 */
export const getTokenExpiry = (): number | null => {
  const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
  return expiry ? parseInt(expiry, 10) : null;
};

/**
 * 检token 是否即将过期分钟内）
 * 解析JWT的exp字段，判断是否在5分钟内过
 */
export const isTokenExpiring = (token: string): boolean => {
  try {
    const remainingTime = getTokenRemainingTime(token);
    // 如果剩余时间小于5分钟00秒），则认为即将过期
    return remainingTime > 0 && remainingTime < 300;
  } catch (error) {
    logger.error("AUTH", "检查token即将过期失败", error as Error);
    return true;
  }
};

/**
 * @deprecated 请使isTokenExpiring 代替
 */
export const isTokenAboutToExpire = isTokenExpiring;

/**
 * 清除所token
 */
export const clearAllTokens = (): void => {
  removeToken();
  removeRefreshToken();
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
};

/**
 * 检token 是否存在
 */
export const hasToken = (): boolean => {
  return !!getToken();
};

/**
 * 解析 JWT token
 */
export const parseJwt = (token: string | null | undefined): JwtPayload | null => {
  try {
    if (!token || token === "true" || token === "false") {
      return null;
    }
    const parts = token.split(".");
    if (parts.length !== 3) {
      return null;
    }
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map(function (c) {
          return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join(""),
    );

    return JSON.parse(jsonPayload) as JwtPayload;
  } catch (error) {
    return null;
  }
};

/**
 * 检token 是否过期
 */
export const isTokenExpired = (token: string): boolean => {
  try {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) {
      return true;
    }

    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp < currentTime;
  } catch (error) {
    // 使用日志记录token过期检查错
    logger.error("AUTH", "检查token过期失败", error as Error);

    return true;
  }
};

/**
 * 获取 token 剩余时间（秒
 */
export const getTokenRemainingTime = (token: string): number => {
  try {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) {
      return 0;
    }

    const currentTime = Math.floor(Date.now() / 1000);
    return Math.max(0, payload.exp - currentTime);
  } catch (error) {
    // 使用日志记录token剩余时间获取错误
    logger.error("AUTH", "获取token剩余时间失败", error as Error);
    return 0;
  }
};

/**
 * 获取 token 中的用户信息
 */
export const getTokenUserInfo = (): UserInfo | null => {
  const token = getToken();
  if (!token) {
    return null;
  }

  try {
    const payload = parseJwt(token);
    if (!payload) {
      return null;
    }

    return {
      id: payload.userId || payload.sub || null,
      username: payload.username || payload.sub || null,
      roles: payload.roles || [],
      permissions: payload.permissions || [],
    };
  } catch (error) {
    logger.error("AUTH", "解析token用户信息失败", error as Error);
    return null;
  }
};

/**
 * 检查用户是否已认证
 */
export const isAuthenticated = (): boolean => {
  const token = getToken();
  return !!token && !isTokenExpired(token);
};

/**
 * 检查用户是否有某个权限
 */
export const hasPermission = (permission: string): boolean => {
  const userInfo = getTokenUserInfo();
  if (!userInfo || !userInfo.permissions) {
    return false;
  }

  return userInfo.permissions.includes(permission);
};

/**
 * 检查用户是否有某个角色
 */
export const hasRole = (role: string): boolean => {
  const userInfo = getTokenUserInfo();
  if (!userInfo || !userInfo.roles) {
    return false;
  }

  return userInfo.roles.includes(role);
};

/**
 * 检查用户是否有任意一个权
 */
export const hasAnyPermission = (permissions: string[]): boolean => {
  const userInfo = getTokenUserInfo();
  if (!userInfo || !userInfo.permissions) {
    return false;
  }

  return permissions.some((permission) =>
    userInfo.permissions.includes(permission),
  );
};

/**
 * 检查用户是否有任意一个角
 */
export const hasAnyRole = (roles: string[]): boolean => {
  const userInfo = getTokenUserInfo();
  if (!userInfo || !userInfo.roles) {
    return false;
  }

  return roles.some((role) => userInfo.roles.includes(role));
};

/**
 * 获取用户角色列表
 */
export const getUserRoles = (): string[] => {
  const userInfo = getTokenUserInfo();
  return userInfo?.roles || [];
};

/**
 * 获取用户权限列表
 */
export const getUserPermissions = (): string[] => {
  const userInfo = getTokenUserInfo();
  return userInfo?.permissions || [];
};

/**
 * 获取用户 ID
 */
export const getUserId = (): string | number | null => {
  const userInfo = getTokenUserInfo();
  return userInfo?.id ?? null;
};

/**
 * 获取用户
 */
export const getUsername = (): string | null => {
  const userInfo = getTokenUserInfo();
  return userInfo?.username || null;
};
