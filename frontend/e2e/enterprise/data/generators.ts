// Enterprise simulation test data generators
const TS = Date.now();

export const MODE = {
  STANDARD_CHAIN: 'standard_chain',
  LARGE_CHAIN: 'large_chain',
  SINGLE_STORE: 'single_store',
  CUSTOM: 'custom',
};

export const genStore = (mode, idx) => ({
  storeCode: 'e2e-' + mode + '-' + idx + '-' + TS,
  storeName: 'Store-' + mode + '-' + idx,
  storeType: idx < 3 ? 'direct' : idx < 6 ? 'franchise' : 'cooperation',
  address: 'Addr-' + idx,
  phone: '1380000' + String(idx).padStart(4,'0'),
  areaSize: 100 + idx * 50,
  status: 'running',
});

export const genEmployee = (storeId, idx) => ({
  name: 'Emp-' + idx,
  employeeNo: 'EMP' + String(idx).padStart(4,'0') + '-' + TS,
  phone: '1390000' + String(idx).padStart(4,'0'),
  department: idx < 4 ? 'Kitchen' : idx < 7 ? 'Front' : 'Management',
  position: idx === 1 ? 'Manager' : 'Staff',
  storeId,
  status: 'active',
});

export const genProduct = (catId, idx) => ({
  name: 'Dish-' + idx,
  code: 'DISH-' + String(idx).padStart(3,'0') + '-' + TS,
  categoryId: catId,
  price: 10 + idx * 5,
  cost: 3 + idx * 2,
  unit: 'serving',
});

export const genSupplier = (idx) => ({
  name: 'Supplier-' + idx,
  code: 'SUP-' + String(idx).padStart(3,'0') + '-' + TS,
  contact: 'Contact-' + idx,
  phone: '1370000' + String(idx).padStart(4,'0'),
  category: idx < 3 ? 'food' : 'packaging',
});
