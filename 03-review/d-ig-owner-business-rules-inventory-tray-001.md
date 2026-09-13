# Owner Business Rules — Inventory / Tray / Fulfillment Checkpoint 001

**Status:** BUSINESS-DEFINITION CHECKPOINT — B6/B9/C unresolved
**Purpose:** Preserve the current Owner decisions and the three remaining explicit business questions before engineering validation. This record is intentionally not an implementation specification and does not authorize schema/migration work.

## 1. Confirmed business definitions

### B1 — Business object separation
Material, Product, and Food are distinct business objects. Numeric ID equality or generic field names must not be used as implicit cross-domain identity.

### B2 — Inventory object typing
Material is managed through Material Inventory; Product is managed through Product Inventory.

Inventory is not a global undifferentiated balance. The intended grain is effectively:

`Business Object × Store`

### B3 — Store is the current inventory location
For the current product scope:

`Store = Warehouse = Inventory Location`

The kitchen is an operating area inside the store, not an independent inventory location. Do not introduce a separate Kitchen Inventory for the current MVP.

### B4 — Sales consume only the selling store's inventory
All sales-related inventory consumption must occur against the inventory belonging to the store where the sale is fulfilled.

The consumed inventory object depends on what is being sold:
- Food/Dish sale → Recipe → Material → that store's Material Inventory
- Product sale → that store's Product Inventory

Cross-store inventory deduction is not allowed.

### B5 — Food sales use recipes
Food sales do not directly interpret a Food ID as an inventory identity. Food consumption is derived through its Recipe and results in Material consumption.

### B10 — TraceCode is traceability, not the inventory driver
TraceCode is used for order/food traceability and must not directly drive inventory deduction.

### B11 — Tray scanners have fixed, single-purpose roles
There are two fixed scanner locations/functions:

1. **Binding scanner:** scanning an idle tray binds it to the first eligible paid order in the `待制作` queue. It is not an order-serving scanner.
2. **Serving scanner:** scanning the tray at the fixed serving position changes the associated order from `待出餐` to `已出餐`. It is not allowed to perform the binding action.

The physical scanner role is part of the business boundary; a generic scan endpoint must not infer either role merely from the current order state.

### External-order fulfillment rule
Takeaway/delivery does not require the tray flow. Its inventory-consumption semantics should remain consistent with the same business consumption rule once B6/C are finalized.

## 2. Explicitly unresolved Owner decisions

### B6 — Exact inventory-consumption timing
The Owner previously described inventory consumption as a "预扣减" when the order changes from `待制作` to `待出餐`. The exact business event still needs to be fixed because the existing wording can refer to different moments:

- kitchen starts production;
- kitchen finishes production / places the completed food on the tray;
- order state transition `待制作 → 待出餐`;
- serving scan `待出餐 → 已出餐`.

**Current status:** NOT DECIDED.

### B9 — Consumption granularity for multi-item orders
The current conversation establishes that a tray is bound to an order and the kitchen works on the order, but it has not explicitly established whether inventory consumption is:

- one consumption for the whole order;
- one consumption per order item/food;
- one consumption per tray.

**Current status:** NOT DECIDED.

### C — Meaning of "预扣减"
The term "预扣减" is ambiguous and must not be interpreted by engineering until Owner confirms whether it means:

- a provisional reservation/deduction that may be reversed before final confirmation; or
- the final material consumption, merely occurring earlier than serving.

**Current status:** NOT DECIDED.

## 3. Engineering boundary already established

The following are engineering questions, not additional Owner business questions:

- whether the existing `inventory` and `store_inventory` tables are duplicate representations or have distinct valid responsibilities;
- how `warehouse_id` and `store_id` currently map to the confirmed Store-as-inventory-location definition;
- which existing paths are ACTIVE / DEAD / CONDITIONAL.

No schema merge, migration, or broad governance round is authorized by this record.

## 4. Next step after B6/B9/C are answered

Run a narrow read-only engineering classification against only these five paths:

1. Food → Recipe → Material → Inventory
2. SalesOrder / POS → Inventory
3. TraceCode → Inventory
4. SelfPurchase → Inventory
5. inventory ↔ store_inventory

For each path classify only:
- ACTIVE
- DEAD
- CONDITIONAL

and provide file/line evidence and the minimal call chain. Then select the first construction targets.

## 5. Governance principle

This checkpoint exists to prevent context loss, not to create another governance phase. Once B6/B9/C are answered, do not reopen the business-definition discussion unless new evidence exposes a genuine contradiction.
