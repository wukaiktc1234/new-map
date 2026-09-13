# Owner Business Rules — Inventory / Tray / Fulfillment Checkpoint 001

**Status:** BUSINESS-DEFINITION CHECKPOINT — B6/B9/C CONFIRMED
**Purpose:** Preserve the Owner decisions for inventory identity, store location, tray fulfillment, and inventory-consumption semantics before engineering validation. This record is intentionally not an implementation specification and does not authorize schema/migration work.

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

### B6 — Inventory-consumption timing
Inventory consumption is triggered when the order formally transitions:

`待制作 → 待出餐`

The business reason is that the system cannot reliably establish or guarantee the exact kitchen-production moment represented by "start production" or "finish production". Waiting until the serving scan (`待出餐 → 已出餐`) is too late because an order may need to be cancelled while it is still being produced.

Therefore the `待制作 → 待出餐` transition is the authoritative business event for inventory reservation/consumption handling.

### B9 — Consumption granularity
The basic consumption unit is the tray as a whole. A tray represents one order and inventory is handled once for the tray/order rather than requiring a separate inventory event for every dish at serving time.

A single order may exceed one tray's physical capacity. In that case the same order may be carried by multiple trays (second, third, etc.). The implementation must therefore model the possibility of multiple trays belonging to one order without incorrectly treating each additional tray as a separate commercial order.

The exact engineering mechanism for multi-tray orders is an implementation question; it must preserve the business rule that the order is the commercial unit while trays are fulfillment/physical-carrying units.

### B10 — TraceCode is traceability, not the inventory driver
TraceCode is used for order/food traceability and must not directly drive inventory deduction.

The tray workflow may use scanning events to advance fulfillment state, but TraceCode itself is not an inventory identity and must not be interpreted as a Material/Product locator.

### B11 — Tray scanners have fixed, single-purpose roles
There are two fixed scanner locations/functions:

1. **Binding scanner:** scanning an idle tray binds it to the first eligible paid order in the `待制作` queue. It is not an order-serving scanner.
2. **Serving scanner:** scanning the tray at the fixed serving position changes the associated order from `待出餐` to `已出餐`. It is not allowed to perform the binding action.

The physical scanner role is part of the business boundary; a generic scan endpoint must not infer either role merely from the current order state.

### C — Meaning of "预扣减"
"预扣减" means that the required inventory is locked/reserved when the B6 event occurs, and the reservation is represented on the books as a pending inventory commitment that can be reversed if the business transaction does not complete.

For cancellation or other non-completion paths, the reserved quantity must be released/reversed rather than remaining as a permanent consumption.

**Accounting terminology clarification:** the Owner's "预付账款/计提" analogy describes the desired business effect of recognizing a pending commitment before final completion. It should not be interpreted by engineering as a literal accounting entry to the `预付账款` financial account unless a separate accounting rule explicitly requires that treatment. Inventory reservation/commitment and financial accounting are separate concerns.

### External-order fulfillment rule
Takeaway/delivery does not require the tray flow. Its inventory-consumption semantics should remain consistent with the same business consumption rule once the corresponding order reaches its defined production/fulfillment event.

## 2. Business consequences now fixed

The current MVP therefore has the following authoritative flow:

`Food → Recipe → Material → Selling Store's Material Inventory`

and for directly sold retail products:

`Product → Selling Store's Product Inventory`

For dine-in tray fulfillment:

`Paid Order / 待制作 → Tray Binding → 待出餐 → Inventory Reservation at 待制作→待出餐 → Serving Scan → 已出餐`

The serving scan is a fulfillment-state event, not the inventory identity or inventory-consumption definition.

An order may use multiple physical trays. Multiple trays must not create multiple commercial orders or cause duplicate inventory consumption merely because the order requires more carrying capacity.

## 3. Engineering boundary already established

The following are engineering questions, not additional Owner business questions:

- whether the existing `inventory` and `store_inventory` tables are duplicate representations or have distinct valid responsibilities;
- how `warehouse_id` and `store_id` currently map to the confirmed Store-as-inventory-location definition;
- which existing paths are ACTIVE / DEAD / CONDITIONAL;
- how reservation, reversal, idempotency, and multi-tray fulfillment should be represented in the existing model without prematurely authorizing schema changes.

No schema merge, migration, or broad governance round is authorized by this record.

## 4. Next step

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

and provide file/line evidence and the minimal call chain.

The scan must specifically test the confirmed business rules:
- Material/Product identity separation;
- Store as inventory location;
- Food → Recipe → Material consumption;
- `待制作 → 待出餐` as the inventory reservation trigger;
- tray-level/order-level consumption with multi-tray orders;
- TraceCode as traceability only;
- SelfPurchase object-type routing;
- cancellation/reversal implications of reservation;
- inventory ↔ store_inventory semantic convergence.

Then select the first construction targets. No further business-definition governance phase is required unless the engineering evidence exposes a genuine contradiction with these Owner decisions.

## 5. Governance principle

This checkpoint exists to preserve decisions and prevent context loss, not to create another governance phase. Business meaning is now sufficiently defined for the narrow engineering classification and subsequent construction planning.
