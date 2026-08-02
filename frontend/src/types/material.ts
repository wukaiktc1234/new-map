export interface MaterialTemplate {
id?: number
templateCode?: string
materialName?: string
category?: string
defaultShelfLife?: number
storageCondition?: string
weightUnit?: string
barcode?: string
barcodeUnique?: number
supplierId?: string
supplierName?: string
description?: string
deductionMode?: string
isTrackingRequired?: number
version?: number
parentId?: number
status?: string
createTime?: string
updateTime: string
}

export interface MaterialTemplateQuery {
page?: number
size?: number
category?: string
name?: string
}

export interface MaterialTemplateFormData {
id?: number
materialName?: string
barcode?: string
category?: string
defaultShelfLife?: number
storageCondition?: string
weightUnit?: string
supplierId?: string
description?: string
status?: string
}
