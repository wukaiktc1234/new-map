/**
 * HR模块状态管理
 * 供 useHealthCertificate.ts 通过 @/stores/hr 导入
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { HealthCertificate } from '@/types/healthCertificate'

export const useHrStore = defineStore('hr', () => {
  const healthCertificates = ref<HealthCertificate[]>([])

  /** 设置健康证列表 */
  function setHealthCertificates(list: HealthCertificate[]) {
    healthCertificates.value = list
  }

  /** 添加健康证 */
  function addHealthCertificate(cert: HealthCertificate) {
    healthCertificates.value.push(cert)
  }

  /** 更新健康证 */
  function updateHealthCertificate(id: string, data: Partial<HealthCertificate>) {
    const index = healthCertificates.value.findIndex(c => c.id === id)
    if (index !== -1) {
      Object.assign(healthCertificates.value[index], data)
    }
  }

  /** 删除健康证 */
  function removeHealthCertificate(id: string) {
    healthCertificates.value = healthCertificates.value.filter(c => c.id !== id)
  }

  return {
    healthCertificates,
    setHealthCertificates,
    addHealthCertificate,
    updateHealthCertificate,
    removeHealthCertificate,
  }
})

// 重新导出类型以兼容 useHealthCertificate.ts 的导入
export type { HealthCertificate } from '@/types/healthCertificate'
