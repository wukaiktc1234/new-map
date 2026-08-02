/**
 * 财务工作流模块类型定义
 * 从 api/financeWorkflow.ts 迁移而来
 */

export interface FinanceWorkflowState {
  workflowId: string;
  workflowType: 'VOUCHER_APPROVAL' | 'EXPENSE_REIMBURSE' | 'INVOICE_PROCESS' | 'REPORT_GENERATE';
  currentState: 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
  currentNode: string;
  history: WorkflowHistoryItem[];
  createTime: string;
  updateTime: string;
}

export interface WorkflowHistoryItem {
  nodeId: string;
  nodeName: string;
  operatorId: string;
  operatorName: string;
  action: 'SUBMIT' | 'APPROVE' | 'REJECT' | 'RETURN' | 'CANCEL';
  comment?: string;
  operateTime: string;
}

export interface WorkflowNode {
  nodeId: string;
  nodeName: string;
  nodeType: 'START' | 'APPROVAL' | 'CC' | 'CONDITION' | 'END';
  assigneeType: 'USER' | 'ROLE' | 'DEPT';
  assigneeIds: string[];
  condition: WorkflowCondition;
}

export interface WorkflowCondition {
  field: string;
  operator: 'EQ' | 'NE' | 'GT' | 'LT' | 'GTE' | 'LTE' | 'CONTAINS';
  value: string | number;
  trueNodeId: string;
  falseNodeId: string;
}

export interface WorkflowDefinition {
  definitionId: string;
  workflowType: string;
  workflowName: string;
  nodes: WorkflowNode[];
  version: number;
  status: 'ACTIVE' | 'INACTIVE';
}

export interface StartWorkflowParams {
  workflowType: string;
  businessId: string;
  businessData: Record<string, unknown>;
  submitterId: string;
  submitterName: string;
  comment?: string;
}

export interface WorkflowActionParams {
  workflowId: string;
  action: 'APPROVE' | 'REJECT' | 'RETURN' | 'CANCEL';
  operatorId: string;
  operatorName: string;
  comment?: string;
}

export interface FinanceWorkflowResult {
  success: boolean;
  message: string;
  data: FinanceWorkflowState;
}
