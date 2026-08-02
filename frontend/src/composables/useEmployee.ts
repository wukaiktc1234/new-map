import { ref, type Ref } from 'vue';
import type { Employee } from '../types/hr';
import employeeApi from '../api/employee';

/**
 * 员工管理Composable
 */
export function useEmployee(): {
  employees: Ref<Employee[]>;
  loading: Ref<boolean>;
  currentEmployee: Ref<Employee | null>;
  error: Ref<string | null>;
  fetchEmployees: () => Promise<void>;
  fetchEmployeeById: (id: string) => Promise<Employee | null>;
  fetchEmployeesByDepartmentId: (departmentId: string) => Promise<Employee[]>;
  fetchEmployeesByPositionId: (positionId: string) => Promise<Employee[]>;
  createEmployee: (employee: Omit<Employee, 'id'>) => Promise<Employee | null>;
updateEmployee: (id: string, employee: Partial<Employee>) => Promise<Employee |
null>;
  deleteEmployee: (id: string) => Promise<boolean>;
  batchUpdateDepartment: (employeeIds: string[], departmentId: string) => Promise<boolean>;
  batchUpdatePosition: (employeeIds: string[], positionId: string) => Promise<boolean>;
  getEmployeeCountByDepartmentId: (departmentId: string) => Promise<number>;
  getEmployeeCountByPositionId: (positionId: string) => Promise<number>;
  searchEmployees: (query: string) => Employee[];
  clearError: () => void;
} {
  const employees = ref<Employee[]>([]);
  const loading = ref(false);
  const currentEmployee = ref<Employee | null>(null);
  const error = ref<string | null>(null);

  /**
   * 获取员工列表
   */
  const fetchEmployees = async (): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await employeeApi.getEmployees();
      employees.value = response || [];
    } catch (err) {
      error.value = '获取员工列表失败';

      loading.value = false;
    }
  };

  /**
   * 根据ID获取员工详情
   */
  const fetchEmployeeById = async (id: string): Promise<Employee | null> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await employeeApi.getEmployeeById(id);
      currentEmployee.value = response;
      return response;
    } catch (err) {
      error.value = '获取员工详情失败';
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 根据部门ID获取员工列表
   */
  const fetchEmployeesByDepartmentId = async (departmentId: string): Promise<Employee[]> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await employeeApi.getEmployeesByDepartmentId(departmentId);
      employees.value = response || [];
      return response || [];
    } catch (err) {
      error.value = '获取部门员工列表失败';
      return [];
    } finally {
      loading.value = false;
    }
  };

  /**
   * 根据职位ID获取员工列表
   */
  const fetchEmployeesByPositionId = async (positionId: string): Promise<Employee[]> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await employeeApi.getEmployeesByPosition(positionId);
      employees.value = response || [];
      return response || [];
    } catch (err) {
      error.value = '获取职位位员工列表失败';
      return [];
    } finally {
      loading.value = false;
    }
  };

  /**
   * 创建员工
   */
  const createEmployee = async (employee: Omit<Employee, 'id'>): Promise<Employee | null> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await employeeApi.createEmployee(employee);
      const newEmployee = response;
      employees.value.push(newEmployee);
      return newEmployee;
    } catch (err) {
      error.value = '创建员工失败';
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 更新员工
   */
const updateEmployee = async (id: string, employee: Partial<Employee>):
Promise<Employee | null> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await employeeApi.updateEmployee(id, employee);
      const updatedEmployee = response;
      const index = employees.value.findIndex(e => e.id === id);
      if (index !== -1) {
        employees.value[index] = updatedEmployee;
      }
      if (currentEmployee.value?.id === id) {
        currentEmployee.value = updatedEmployee;
      }
      return updatedEmployee;
    } catch (err) {
      error.value = '更新员工失败';
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 删除员工
   */
  const deleteEmployee = async (id: string): Promise<boolean> => {
    loading.value = true;
    error.value = null;
    try {
      await employeeApi.deleteEmployee(id);
      employees.value = employees.value.filter(e => e.id !== id);
      if (currentEmployee.value?.id === id) {
        currentEmployee.value = null;
      }
      return true;
    } catch (err) {
      error.value = '删除员工失败';
      return false;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 批量更新员工部门
   */
const batchUpdateDepartment = async (employeeIds: string[], departmentId: string):
Promise<boolean> => {
    loading.value = true;
    error.value = null;
    try {
      await employeeApi.batchUpdateDepartment(employeeIds, departmentId);
      // 更新本地员工数据
      employees.value.forEach(emp => {
        if (emp.id && employeeIds.includes(emp.id)) {
          emp.departmentId = departmentId;
        }
      });
      return true;
    } catch (err) {
      error.value = '批量更新员工部门失败';
      return false;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 批量更新员工职位
   */
const batchUpdatePosition = async (employeeIds: string[], positionId: string):
Promise<boolean> => {
    loading.value = true;
    error.value = null;
    try {
      await employeeApi.batchUpdatePosition(employeeIds, positionId);
      // 更新本地员工数据
      employees.value.forEach(emp => {
        if (emp.id && employeeIds.includes(emp.id)) {
          emp.positionId = positionId;
        }
      });
      return true;
    } catch (err) {
      error.value = '批量更新员工职位失败';
      return false;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 获取部门员工数量
   */
  const getEmployeeCountByDepartmentId = async (departmentId: string): Promise<number> => {
    try {
      const response = await employeeApi.getEmployeeCountByDepartmentId(departmentId);
      return response || 0;
    } catch (err) {
      return 0;
    }
  };

  /**
   * 获取职位位员工数量
   */
  const getEmployeeCountByPositionId = async (positionId: string): Promise<number> => {
    try {
      const response = await employeeApi.getEmployeeCountByPosition(positionId);
      return response || 0;
    } catch (err) {
      return 0;
    }
  };

  /**
   * 搜索员工
   */
  const searchEmployees = (query: string): Employee[] => {
    if (!query) {
      return employees.value;
    }
    const lowerQuery = query.toLowerCase();
    return employees.value.filter(emp =>
      emp.employeeName?.toLowerCase().includes(lowerQuery) ||
      emp.employeeCode?.toLowerCase().includes(lowerQuery) ||
      emp.departmentName?.toLowerCase().includes(lowerQuery) ||
      emp.positionName?.toLowerCase().includes(lowerQuery)
    );
  };

  /**
   * 清空错误信息
   */
  const clearError = (): void => {
    error.value = null;
  };

  return {
    employees,
    loading,
    currentEmployee,
    error,

    // 方法
    fetchEmployees,
    fetchEmployeeById,
    fetchEmployeesByDepartmentId,
    fetchEmployeesByPositionId,
    createEmployee,
    updateEmployee,
    deleteEmployee,
    batchUpdateDepartment,
    batchUpdatePosition,
    getEmployeeCountByDepartmentId,
    getEmployeeCountByPositionId,
    searchEmployees,
    clearError
  };
}
