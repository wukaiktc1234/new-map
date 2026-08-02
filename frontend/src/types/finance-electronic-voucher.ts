/**
 * 电子凭证模块类型定义
 * 从 api/electronicVoucher.ts 迁移而来
 */

export interface VoucherQueryParams {
  voucherNo?: string;
  voucherType?: string;
  signatureStatus?: number;
  verifyStatus?: number;
  status?: number;
  startDate?: string;
  endDate?: string;
  businessType?: string;
  businessId?: number;
  page?: number;
  size?: number;
}

export interface VoucherVO {
  id: number;
  voucherType: string;
  voucherTypeName: string;
  voucherNo: string;
  totalAmount: number;
  issueDate: string;
  signatureStatus: number;
  signatureStatusName: string;
  verifyStatus: number;
  verifyStatusName: string;
  status: number;
  statusName: string;
  createdAt: string;
  createdByName: string;
}

export interface VoucherDetail {
  id: number;
  voucherType: string;
  voucherTypeName: string;
  voucherNo: string;
  sourceFilePath: string;
  sourceFileHash: string;
  sourceFileSize: number;
  sourceFileType: string;
  totalAmount: number;
  currency: string;
  issueDate: string;
  signatureStatus: number;
  signatureStatusName: string;
  verifyStatus: number;
  verifyStatusName: string;
  verifyTime: string;
  verifyMessage: string;
  status: number;
  statusName: string;
  businessType: string;
  businessId: number;
  financeVoucherId: number;
  invoice?: InvoiceVO;
  signatureLogs: SignatureLogVO[];
  createdAt: string;
  createdByName: string;
}

export interface InvoiceVO {
  id: number;
  invoiceType: string;
  invoiceTypeName: string;
  invoiceCode: string;
  invoiceNo: string;
  issueDate: string;
  buyerName: string;
  buyerTaxNo: string;
  buyerAddress: string;
  buyerBank: string;
  sellerName: string;
  sellerTaxNo: string;
  sellerAddress: string;
  sellerBank: string;
  totalAmount: number;
  taxAmount: number;
  amountWithoutTax: number;
  currency: string;
  deductibleStatus: number;
  deductibleAmount: number;
  certifyStatus: number;
  certifyTime: string;
  certifyPeriod: string;
  transferOutAmount: number;
  remark: string;
  checkCode: string;
  machineNo: string;
  payee: string;
  checker: string;
  issuer: string;
  pdfUrl: string;
  ofdUrl: string;
  xmlUrl: string;
  items: InvoiceItemVO[];
}

export interface InvoiceItemVO {
  id: number;
  itemNo: number;
  goodsCode: string;
  goodsName: string;
  specification: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  taxRate: number;
  taxAmount: number;
  discountAmount: number;
  remark: string;
}

export interface SignatureLogVO {
  id: number;
  signType: string;
  signAlgorithm: string;
  signer: string;
  signerCertNo: string;
  signTime: string;
  signResult: number;
  signResultName: string;
  verifyTime: string;
  certificateIssuer: string;
  certificateValidFrom: string;
  certificateValidTo: string;
  timestampAuthority: string;
  errorCode: string;
  errorMessage: string;
  createdAt: string;
}

export interface UploadResult {
  voucherId: number;
  voucherType: string;
  voucherTypeName: string;
  voucherNo: string;
  status: string;
  statusDescription: string;
  fileHash: string;
  duplicate: boolean;
  errorMessage: string;
}

export interface BatchUploadResult {
  batchNo: string;
  totalCount: number;
  successCount: number;
  failCount: number;
  duplicateCount: number;
  results: UploadResult[];
}

export interface SignatureVerifyResult {
  status: number;
  statusDescription: string;
  signer: string;
  signTime: string;
  certificateIssuer: string;
  certificateValidFrom: string;
  certificateValidTo: string;
  timestampAuthority: string;
  errorMessage: string;
}

export interface InvoiceVerifyResult {
  status: number;
  statusDescription: string;
  invoiceCode: string;
  invoiceNo: string;
  issueDate: string;
  buyerName: string;
  buyerTaxNo: string;
  sellerName: string;
  sellerTaxNo: string;
  totalAmount: number;
  taxAmount: number;
  invoiceStatus: number;
  invoiceStatusDescription: string;
  verifyTime: string;
  verifyCount: number;
  errorMessage: string;
  verifySource: string;
  taxPlatformVerified: boolean;
}

export interface BatchVerifyResult {
  totalCount: number;
  successCount: number;
  failCount: number;
}

// 入账相关类型定义
export interface VoucherAccountingRequest {
  accountingPeriod: string;
  postingDate: string;
  voucherWord?: string;
  attachmentCount?: number;
  customSummary?: string;
  departmentId?: number;
  projectId?: number;
  handlerId?: number;
  autoReview?: boolean;
  entries?: AccountingEntryRequest[];
}

export interface AccountingEntryRequest {
  accountCode: string;
  accountName?: string;
  debitAmount?: number;
  creditAmount?: number;
  summary?: string;
  auxiliaryDeptId?: number;
  auxiliaryProjectId?: number;
  auxiliaryCustomerId?: number;
  auxiliarySupplierId?: number;
  auxiliaryEmployeeId?: number;
}

export interface VoucherAccountingResult {
  success: boolean;
  electronicVoucherId: number;
  electronicVoucherNo: string;
  financeVoucherId?: number;
  financeVoucherNo?: string;
  voucherWord?: string;
  voucherNumber?: string;
  accountingPeriod: string;
  postingDate: string;
  totalAmount: number;
  totalDebit: number;
  totalCredit: number;
  entries: AccountingEntryResult[];
  errorMessage?: string;
  accountingTime: string;
  preview: boolean;
}

export interface AccountingEntryResult {
  entryNo: number;
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
  summary: string;
  auxiliaryInfo?: string;
}

export interface AccountingEntryTemplate {
  debitAccount: string;
  debitAccountName: string;
  creditAccount: string;
  creditAccountName: string;
  summary: string;
  amountSource: string;
}

// 归档相关类型定义
export interface VoucherArchiveQueryParams {
  archiveNo?: string;
  archiveName?: string;
  fiscalYear?: string;
  archiveType?: number;
  status?: number;
  archiveDateFrom?: string;
  archiveDateTo?: string;
  archivistId?: number;
  page?: number;
  size?: number;
}

export interface VoucherArchive {
  id: number;
  archiveNo: string;
  archiveName: string;
  fiscalYear: string;
  periodFrom: string;
  periodTo: string;
  archiveDate: string;
  archiveType: number;
  status: number;
  voucherCount: number;
  fileSize: number;
  filePath?: string;
  xbrlPath?: string;
  archivistId?: number;
  archivistName?: string;
  reviewerId?: number;
  reviewerName?: string;
  reviewTime?: string;
  remark?: string;
  createdAt: string;
  items?: VoucherArchiveItem[];
}

export interface VoucherArchiveItem {
  id: number;
  voucherId: number;
  voucherNo: string;
  voucherType: string;
  voucherTypeName: string;
  amount: number;
  issueDate: string;
  sourceFilePath?: string;
  signatureValid?: boolean;
  verifyValid?: boolean;
  financeVoucherId?: number;
}

export interface VoucherArchiveInfo {
  archiveName: string;
  fiscalYear: string;
  periodFrom: string;
  periodTo: string;
  archiveType: number;
  remark?: string;
}

export interface ArchiveVerifyResult {
  valid: boolean;
  message: string;
  totalCount: number;
  validCount: number;
  invalidCount: number;
}

/** 手动输入表单数据 */
export interface ManualInputFormData {
  invoiceType: string;
  issueDate: string;
  invoiceCode: string;
  invoiceNo: string;
  checkCode: string;
  totalAmount: number;
  taxAmount: number;
  buyer: {
    name: string;
    taxNo: string;
    addressPhone: string;
    bankAccount: string;
  };
  seller: {
    name: string;
    taxNo: string;
    addressPhone: string;
    bankAccount: string;
  };
  manualInputReason: string;
}

/** 报销请求数据 */
export interface ReimbursementRequestData {
  reimbursementType: string;
  items: Array<{
    voucherId: number;
    amount: number;
    expenseType: string;
    expenseDescription: string;
  }>;
  remark: string;
}
