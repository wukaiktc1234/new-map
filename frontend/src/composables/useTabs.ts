import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import logger from '@/utils/logger';

interface Tab {
  title: string;
  path: string;
  icon: string;
  closable: boolean;
  isGroup?: boolean;
  groupPrefix?: string;
  groupCount?: number;
}

interface TabGroups {
  [key: string]: Tab[];
}

const STORAGE_KEYS = {
  tabList: 'appTabList',
  activeTab: 'appActiveTab',
  collapsedGroups: 'appCollapsedGroups',
} as const;

const MAX_TABS = 15;
const TAB_WARNING_THRESHOLD = 10;

/** 将 catch 块中的 unknown 类型 error 转换为 Error | undefined */
function toError(error: unknown): Error | undefined {
  return error instanceof Error ? error : error ? new Error(String(error)) : undefined;
}

/**
 * 标签页管理 Composable
 * 提供标签页的添加、删除、切换、缓存、分组折叠等能力
 * @param cleanupComponentInstances - 可选的组件实例清理回调（关闭 Tab 时调用）
 */
export function useTabs(cleanupComponentInstances?: (path: string) => void) {
  const router = useRouter();

  const tabList = ref<Tab[]>([]);
  const activeTab = ref('/home');
  const collapsedGroups = ref<Record<string, boolean>>({});
  const showContextMenu = ref(false);
  const contextMenuStyle = ref<Record<string, string>>({});
  const clickedTabPath = ref('');

  // ==================== localStorage 持久化 ====================

  function saveTabState(): void {
    try {
      localStorage.setItem(STORAGE_KEYS.tabList, JSON.stringify(tabList.value));
      localStorage.setItem(STORAGE_KEYS.activeTab, activeTab.value);
      localStorage.setItem(
        STORAGE_KEYS.collapsedGroups,
        JSON.stringify(collapsedGroups.value),
      );
    } catch (error) {
      logger.error('useTabs', '保存标签页状态失败', toError(error));
    }
  }

  // ==================== 初始化 ====================

  function createHomeTab(): Tab {
    return {
      title: '首页',
      path: '/home',
      icon: 'home-filled',
      closable: false,
    };
  }

  function isValidTab(tab: unknown): tab is Tab {
    return (
      !!tab &&
      typeof tab === 'object' &&
      typeof (tab as Tab).path === 'string' &&
      typeof (tab as Tab).title === 'string' &&
      !!(tab as Tab).path &&
      !!(tab as Tab).title
    );
  }

  function dedupeTabs(tabs: Tab[]): Tab[] {
    const seenPaths = new Set<string>();
    const unique: Tab[] = [];
    tabs.forEach((tab) => {
      if (!seenPaths.has(tab.path)) {
        seenPaths.add(tab.path);
        unique.push(tab);
      }
    });
    return unique;
  }

  function normalizeTab(tab: Tab): Tab {
    return {
      title: tab.title,
      path: tab.path,
      icon: tab.icon || 'document',
      closable: tab.closable !== false && tab.path !== '/home',
      isGroup: tab.isGroup || false,
      groupPrefix: tab.groupPrefix,
      groupCount: tab.groupCount,
    };
  }

  function ensureHomeTab(tabs: Tab[]): Tab[] {
    const homeTab = tabs.find((tab) => tab.path === '/home');
    if (!homeTab) {
      return [createHomeTab(), ...tabs];
    }
    homeTab.closable = false;
    return tabs;
  }

  function initTabList(): Tab[] {
    try {
      const savedCollapsed = localStorage.getItem(STORAGE_KEYS.collapsedGroups);
      if (savedCollapsed) {
        collapsedGroups.value = JSON.parse(savedCollapsed);
      }

      const savedTabList = localStorage.getItem(STORAGE_KEYS.tabList);
      if (!savedTabList) {
        return [createHomeTab()];
      }

      const parsedTabs = JSON.parse(savedTabList);
      if (!Array.isArray(parsedTabs)) {
        return [createHomeTab()];
      }

      const validTabs = parsedTabs.filter(isValidTab);
      const uniqueTabs = dedupeTabs(validTabs);

      if (uniqueTabs.length > MAX_TABS) {
        uniqueTabs.splice(MAX_TABS);
      }

      const finalTabs = ensureHomeTab(uniqueTabs.map(normalizeTab));
      return finalTabs;
    } catch (error) {
      logger.error('useTabs', '初始化标签页列表失败', toError(error));
      localStorage.removeItem(STORAGE_KEYS.tabList);
      localStorage.removeItem(STORAGE_KEYS.collapsedGroups);
      return [createHomeTab()];
    }
  }

  function initActiveTab(tabListFromInit: Tab[]): string {
    try {
      const savedActiveTab = localStorage.getItem(STORAGE_KEYS.activeTab);
      if (
        savedActiveTab &&
        typeof savedActiveTab === 'string' &&
        tabListFromInit.some((tab) => tab.path === savedActiveTab)
      ) {
        return savedActiveTab;
      }
    } catch (error) {
      logger.error('useTabs', '初始化活动标签页失败', toError(error));
    }
    return '/home';
  }

  // 初始化标签页
  tabList.value = initTabList();
  activeTab.value = initActiveTab(tabList.value);

  // ==================== 分组折叠 ====================

  function getTabGroupPrefix(path: string): string {
    const parts = path.split('/').filter(Boolean);
    return parts.length > 1 ? `/${parts[0]}` : path;
  }

  function mergeSimilarTabs(): void {
    const allOriginalTabs = [...tabList.value];
    const nonHomeTabs = tabList.value.filter(
      (tab) => tab.path !== '/home' && !tab.isGroup,
    );

    const tabGroups: TabGroups = {};
    nonHomeTabs.forEach((tab) => {
      const prefix = getTabGroupPrefix(tab.path);
      if (!tabGroups[prefix]) {
        tabGroups[prefix] = [];
      }
      tabGroups[prefix].push(tab);
    });

    const newTabList: Tab[] = [allOriginalTabs.find((tab) => tab.path === '/home')!];
    const preservedPaths = new Set<string>(['/home']);

    Object.entries(tabGroups).forEach(([prefix, group]) => {
      if (group.length > 1) {
        const groupTab: Tab = {
          title: `${group[0].title.split(' ')[0]}组`,
          path: `${prefix}-group`,
          icon: group[0].icon,
          closable: true,
          isGroup: true,
          groupPrefix: prefix,
          groupCount: group.length,
        };
        newTabList.push(groupTab);
        preservedPaths.add(groupTab.path);

        if (!collapsedGroups.value[prefix]) {
          const groupTabsWithPrefix = group.map((tab) => ({
            ...tab,
            groupPrefix: prefix,
          }));
          newTabList.push(...groupTabsWithPrefix);
          groupTabsWithPrefix.forEach((tab) => preservedPaths.add(tab.path));
        } else {
          group.forEach((tab) => preservedPaths.add(tab.path));
        }
      } else {
        newTabList.push(...group);
        group.forEach((tab) => preservedPaths.add(tab.path));
      }
    });

    // 保留未分组的其他标签页（如组标签页）
    allOriginalTabs.forEach((tab) => {
      if (!preservedPaths.has(tab.path) && !newTabList.some((t) => t.path === tab.path)) {
        newTabList.push(tab);
      }
    });

    // 确保 activeTab 始终指向有效标签页
    const isActiveTabValid = newTabList.some((tab) => tab.path === activeTab.value);
    if (!isActiveTabValid) {
      const validTab = newTabList.find((tab) => tab.closable !== false) || newTabList[0];
      if (validTab) {
        activeTab.value = validTab.path;
      }
    }

    tabList.value = newTabList;
    saveTabState();
  }

  function toggleTabGroup(prefix: string): void {
    collapsedGroups.value[prefix] = !collapsedGroups.value[prefix];
    mergeSimilarTabs();
    saveTabState();
  }

  // ==================== 标签页操作 ====================

  function addTab(route: { path?: string; meta?: Record<string, unknown> } | null): void {
    if (!route || !route.path || !route.meta?.title) return;

    const existingTab = tabList.value.find((tab) => tab.path === route.path);
    if (!existingTab) {
      const title = (route.meta?.title as string) || '未知页面';
      const icon = (route.meta?.icon as string) || 'document';

      tabList.value.push({
        title,
        path: route.path,
        icon,
        closable: route.path !== '/home',
      });

      if (tabList.value.length >= TAB_WARNING_THRESHOLD) {
        ElMessage.warning(
          `当前标签页数量已达 ${tabList.value.length} 个，接近上限 ${MAX_TABS} 个，建议关闭不常用标签页`,
        );
      }

      if (tabList.value.length > 8) {
        const beforeMergeCount = tabList.value.length;
        mergeSimilarTabs();
        const afterMergeCount = tabList.value.length;
        if (afterMergeCount < beforeMergeCount) {
          ElMessage.info(`已自动合并相似标签页，当前标签页数量 ${afterMergeCount}`);
        }
      }

      if (tabList.value.length >= MAX_TABS) {
        ElMessage.error(`当前标签页数量已达上限 ${MAX_TABS} 个，无法再打开新标签页`);
      }
    }

    activeTab.value = route.path;
    saveTabState();
  }

  function handleTabClick(tab: { props?: { name?: string | number } }): void {
    if (!tab || !tab.props) return;

    const tabPath = String(tab.props.name ?? '');
    if (!tabPath) return;

    const tabInfo = tabList.value.find((t) => t.path === tabPath);
    if (!tabInfo) return;

    // 折叠组标签页：切换折叠状态
    if (tabInfo.isGroup && tabInfo.groupPrefix) {
      toggleTabGroup(tabInfo.groupPrefix);
      return;
    }

    const currentPath = router.currentRoute.value.path;
    if (currentPath === tabPath) return;

    router.push(tabPath).catch((error) => {
      logger.error('useTabs', '路由跳转失败', toError(error));
      activeTab.value = currentPath;
    });
  }

  function handleTabRemove(targetPath: string): void {
    if (!targetPath || typeof targetPath !== 'string') return;

    const tabToRemove = tabList.value.find((tab) => tab.path === targetPath);
    if (!tabToRemove) return;

    const isActiveTab = activeTab.value === targetPath;
    const currentPath = router.currentRoute.value.path;

    try {
      if (tabToRemove.isGroup && tabToRemove.groupPrefix) {
        const groupPrefix = tabToRemove.groupPrefix;
        tabList.value = tabList.value.filter(
          (tab) =>
            tab.path !== targetPath &&
            !(tab.groupPrefix === groupPrefix && !tab.isGroup),
        );
      } else {
        const index = tabList.value.findIndex((tab) => tab.path === targetPath);
        if (index !== -1) {
          tabList.value.splice(index, 1);
        }
      }

      if (isActiveTab) {
        const currentRouteTab = tabList.value.find((tab) => tab.path === currentPath);
        const newActiveTab = currentRouteTab || tabList.value[0];
        if (newActiveTab) {
          activeTab.value = newActiveTab.path;
        } else {
          activeTab.value = '/home';
          if (currentPath !== '/home') {
            router.push('/home').catch(() => {});
          }
        }
      }

      if (cleanupComponentInstances) {
        cleanupComponentInstances(targetPath);
      }

      saveTabState();
    } catch (error) {
      logger.error('useTabs', '关闭标签页失败', toError(error));
    }
  }

  // ==================== 右键菜单 ====================

  function closeContextMenu(): void {
    showContextMenu.value = false;
    document.removeEventListener('click', closeContextMenu);
  }

  function handleTabContextMenu(event: MouseEvent): void {
    event.preventDefault();

    const tabElement = (event.target as HTMLElement)?.closest('.el-tabs__item');
    if (!tabElement) return;

    const tabName = tabElement.getAttribute('aria-controls');
    if (!tabName) return;

    clickedTabPath.value = tabName;
    showContextMenu.value = true;
    contextMenuStyle.value = {
      left: `${event.clientX}px`,
      top: `${event.clientY}px`,
      position: 'fixed',
      zIndex: '10000',
    };

    setTimeout(() => {
      document.addEventListener('click', closeContextMenu);
    }, 0);
  }

  function closeCurrentTab(): void {
    if (clickedTabPath.value && clickedTabPath.value !== '/home') {
      handleTabRemove(clickedTabPath.value);
    }
    showContextMenu.value = false;
  }

  function closeOtherTabs(): void {
    if (!clickedTabPath.value) return;

    const tabsToKeep = tabList.value.filter(
      (tab) => tab.path === clickedTabPath.value || tab.path === '/home',
    );

    if (cleanupComponentInstances) {
      tabList.value.forEach((tab) => {
        if (tab.path !== clickedTabPath.value && tab.path !== '/home') {
          cleanupComponentInstances(tab.path);
        }
      });
    }

    tabList.value = tabsToKeep;
    saveTabState();
    showContextMenu.value = false;
  }

  function closeAllTabs(): void {
    if (cleanupComponentInstances) {
      tabList.value.forEach((tab) => {
        if (tab.path !== '/home') {
          cleanupComponentInstances(tab.path);
        }
      });
    }

    tabList.value = [createHomeTab()];
    activeTab.value = '/home';
    saveTabState();
    showContextMenu.value = false;
  }

  // ==================== 路由监听 ====================

  watch(
    () => router.currentRoute.value,
    (newRoute) => {
      if (newRoute && newRoute.path) {
        addTab(newRoute);
      }
    },
    { immediate: true },
  );

  return {
    tabList,
    activeTab,
    collapsedGroups,
    showContextMenu,
    contextMenuStyle,
    addTab,
    handleTabClick,
    handleTabRemove,
    toggleTabGroup,
    handleTabContextMenu,
    closeContextMenu,
    closeCurrentTab,
    closeOtherTabs,
    closeAllTabs,
    saveTabState,
  };
}
