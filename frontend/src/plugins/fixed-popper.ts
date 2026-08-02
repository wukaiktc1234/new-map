/**
 * 全局 Popper 定位策略修复插件
 *
 * 问题：Element Plus 默认使用 Popper.js 的 strategy: 'absolute'，
 * 当页面存在自定义滚动容器时，下拉菜单在滚动过程中会与触发元素分离错位。
 *
 * 方案：通过 Vue 插件全局包装 ElSelect/ElDatePicker/ElTimePicker/ElCascader
 * 组件，根据 teleported 属性智能选择定位策略：
 * - teleported=true（默认）：popper 渲染到 body → 使用 strategy: 'fixed'
 * - teleported=false：popper 留在滚动容器内 → 使用 strategy: 'absolute'
 *
 * 这样 dialog 内部的组件（teleported=false）能正确跟随滚动，
 * 而 dialog 外部的组件（teleported=true）也能保持稳定。
 */
import type { App, DefineComponent } from 'vue'
import { defineComponent, h } from 'vue'

/** fixed 策略配置（用于 teleported 到 body 的场景） */
const FIXED_POPPER_OPTIONS = {
  strategy: 'fixed' as const,
  modifiers: [
    { name: 'flip', options: { fallbackPlacements: ['bottom-start', 'top-start'] as const } },
    { name: 'preventOverflow', options: { boundary: 'viewport' as const } },
  ],
}

/** absolute 策略配置（用于 teleported=false 留在滚动容器内的场景） */
const ABSOLUTE_POPPER_OPTIONS = {
  strategy: 'absolute' as const,
  modifiers: [
    { name: 'flip', options: { fallbackPlacements: ['bottom-start', 'top-start'] as const } },
    { name: 'preventOverflow', options: { boundary: 'clippingParents' as const } },
  ],
}

/**
 * 包装 Element Plus 组件，智能注入 popperOptions
 * - teleported=false 时使用 strategy: 'absolute'（跟随滚动容器）
 * - teleported=true（默认）时使用 strategy: 'fixed'（相对 viewport 稳定）
 * 如果使用时显式传入 popperOptions，会与默认值合并（显式传入优先）
 */
function withFixedPopper(WrappedComponent: DefineComponent<Record<string, unknown>>, componentName: string) {
  return defineComponent({
    name: `Fixed${componentName}`,
    inheritAttrs: false,
    setup(_props, { attrs, slots }) {
      // 根据 teleported 属性选择定位策略
      const isTeleported = attrs.teleported !== false && attrs['teleported'] !== false
      const baseOptions = isTeleported ? FIXED_POPPER_OPTIONS : ABSOLUTE_POPPER_OPTIONS

      return () =>
        h(WrappedComponent, {
          ...attrs,
          popperOptions: {
            ...baseOptions,
            ...(attrs.popperOptions as Record<string, unknown> || {}),
          },
        }, slots)
    },
  })
}

/**
 * 安装插件：全局替换 ElSelect/ElDatePicker/ElTimePicker/ElCascader
 * 使其默认使用 strategy: 'fixed' 的 Popper 定位
 */
export default {
  install(app: App) {
    const componentsToWrap = [
      'ElSelect',
      'ElSelectV2',
      'ElDatePicker',
      'ElTimePicker',
      'ElCascader',
    ]

    for (const name of componentsToWrap) {
      const original = app.component(name)
      if (original) {
        app.component(name, withFixedPopper(original as DefineComponent<Record<string, unknown>>, name))
      }
    }
  },
}
