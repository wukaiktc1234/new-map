import * as XLSX from 'xlsx';

export interface ExcelHeader {
  key: string;
  title: string;
  width?: number;
}

export interface ExcelRowData {
  [key: string]: unknown;
}

export interface OrderExportData {
  orderNo: string;
  orderType: string;
  status: string;
  amount: number;
  createTime: string;
  customerName?: string;
  phone?: string;
  tableNo?: string;
  deliveryAddress?: string;
  remarks?: string;
}

export class ExcelExporter {
  static exportToExcel(
    data: Record<string, unknown>[],
    headers: ExcelHeader[],
    filename: string = 'export.xlsx'
  ) {
    if (!data || data.length === 0) {
      throw new Error('没有数据可以导出');
    }

    const exportData = data.map(item => {
      const row: ExcelRowData = {};
      headers.forEach(header => {
        row[header.title] = item[header.key];
      });
      return row;
    });

    // 创建工作簿和工作
const workbook = XLSX.utils.book_new();
    const worksheet = XLSX.utils.json_to_sheet(exportData);

    // 设置列宽
const colWidths = headers.map(header => ({
      wch: header.width || 15
    }));
    worksheet['!cols'] = colWidths;

    // 添加工作表到工作

    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');

    // 生成Excel文件并下

    XLSX.writeFile(workbook, filename);
  }

  /**
   * 导出订单数据为Excel文件
   * @param orders 订单数据数组
   * @param filename 文件
 */
static exportOrders(orders: OrderExportData[], filename: string = `订单数据_${new Date().toISOString().slice(0, 10)}.xlsx`):
void {
    const headers = [
{ key: 'orderNo',
title: '订单',
width: 20 },
{ key: 'orderType',
title: '订单类型',
width: 10 },
{ key: 'status',
title: '订单状',
width: 10 },
{ key: 'amount',
title: '金额',
width: 10 },
{ key: 'createTime',
title: '创建时间',
width: 20 },
{ key: 'customerName',
title: '顾客姓名',
width: 15 },
{ key: 'phone',
title: '手机',
width: 15 },
{ key: 'tableNo',
title: '桌号',
width: 10 },
{ key: 'deliveryAddress',
title: '配送地址',
width: 30 },
{ key: 'remarks',
title: '备注',
width: 20 }
    ];

    // 转换订单类型和状态为中文
const transformedOrders = orders.map(order => {
      return {
        ...order,
        orderType: this.getOrderTypeText(order.orderType),
        status: this.getOrderStatusText(order.status)
      };
    });

    this.exportToExcel(transformedOrders, headers, filename);
  }

  private static getOrderTypeText(type: string): string {
    const typeMap: Record<string, string> = {
      'dine_in': '堂食',
      'takeaway': '外卖',
      'self_pickup': '自提'
};
    return typeMap[type] || type;
  }

  private static getOrderStatusText(status: string): string {
    const statusMap: Record<string, string> = {
      'pending': '待处理',
      'processing': '制作',
      'completed': '已完成',
      'rejected': '已拒绝',
      'new': '待接',
      'accepted': '已接',
      'delivering': '配送中'
};
    return statusMap[status] || status;
  }
}
