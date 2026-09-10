# Current-to-Target Semantic Mapping

Phase 20: Conceptual Mapping for Restaurant ERP System

## Overview

This document maps current database objects to target business semantics. It strictly separates:
- **CURRENT REALITY**: What the database actually contains
- **TARGET BUSINESS SEMANTICS**: What the business model should express
- **TARGET TECHNICAL MODEL**: How the business semantics translate to technical design

---

## CURRENT REALITY

### Database Objects Inventory

#### 1. Foods (Active Table)

| Field | Type | Description |
|-------|------|-------------|
| food_code | string | Unique identifier |
| food_name | string | Display name |
| food_price | decimal | Selling price |
| cost_price | decimal | Cost basis |
| food_category | string | Category label |
| stock | integer | Quantity on hand |
| shelf_life_days | integer | Expiration period |
| production_address | string | Origin location |
| nutrition_info | text | Nutritional data |
| storage_conditions | string | Storage requirements |
| price | decimal | Price (duplicate?) |
| weight | decimal | Physical weight |
| quality_grade | string | Quality tier |

**Problems identified:**
- Duplicate price fields (food_price, price)
- Mixing concerns: inventory (stock) with product definition
- storage_conditions as string (should be structured)
- nutrition_info as text (should be structured)
- No clear relationship to material_archives

#### 2. Food (Legacy Table)

Marked as deprecated. Should be removed from analysis.

#### 3. Product (Legacy)

Marked as LEGACY. Role unclear relative to foods/material_archives.

#### 4. Material Archives

| Field | Type | Description |
|-------|------|-------------|
| material_id | int | Primary key |
| material_code | string | Business code |
| material_name | string | Display name |
| category_id | int | FK to category |
| unit | string | Measurement unit |
| spec | string | Specification |
| reference_price | decimal | Price reference |
| supplier_id | int | FK to supplier |
| barcode | string | Scannable code |
| origin | string | Source location |
| shelf_life | int | Days until expiration |
| storage_condition | string | Storage requirements |
| department_id | int | Owning department |

**Problems identified:**
- Separate identity system from foods
- No clear relationship between material_archives and foods
- category_id references a category table not in scope
- supplier_id references a supplier table not in scope

#### 5. Inventory (Warehouse)

| Field | Type | Description |
|-------|------|-------------|
| inventory_id | int | Primary key |
| material_id | int | FK to material_archives |
| warehouse_id | int | FK to warehouse |
| current_stock | decimal | On-hand quantity |
| locked_quantity | decimal | Reserved quantity |
| unit_cost | decimal | Cost per unit |
| total_cost | decimal | current_stock * unit_cost |
| batch_no | string | Lot tracking |
| min_safe_qty | decimal | Reorder point |
| max_stock_qty | decimal | Maximum capacity |

#### 6. Store Inventory

| Field | Type | Description |
|-------|------|-------------|
| store_id | int | FK to store |
| material_id | int | FK to material_archives |
| current_stock | decimal | On-hand quantity |
| unit | string | Measurement unit |
| unit_cost | decimal | Cost per unit |
| total_cost | decimal | Value |
| safety_stock | decimal | Minimum threshold |
| max_stock | decimal | Maximum capacity |

**Problems identified:**
- Duplicate inventory tracking between warehouse and store
- Different field names for same concepts (safety_stock vs min_safe_qty)
- unit stored redundantly (already in material_archives)

#### 7. Inventory Transactions

Tracks movement of inventory. Fields not specified in detail.

#### 8. Dish Recipe

| Field | Type | Description |
|-------|------|-------------|
| dish_id | int | FK to dish (food?) |
| ingredient_id | int | FK to ingredient |
| ingredient_name | string | Denormalized name |
| quantity | decimal | Amount needed |
| unit | string | Measurement |
| estimated_cost | decimal | Predicted cost |
| actual_cost | decimal | Actual cost |
| allow_variance | boolean | Permits deviation |
| is_required | boolean | Mandatory ingredient |

#### 9. Dish Recipe New

Same structure as dish_recipe but uses materialId instead of ingredientId.

**Problems identified:**
- Parallel recipe tables suggest incomplete migration
- ingredient_id vs material_id confusion
- Denormalized ingredient_name
- No link to foods table for the dish itself

#### 10. Order Items

| Field | Type | Description |
|-------|------|-------------|
| order_id | int | FK to order |
| food_id | int | FK to foods |
| food_name | string | Denormalized name |
| quantity | int | Amount ordered |
| unit_price | decimal | Price at sale |
| total_price | decimal | Computed total |

#### 11. Purchase Order Items

| Field | Type | Description |
|-------|------|-------------|
| product_id | int | FK to product (legacy?) |
| material_id | int | FK to material_archives |
| quantity | decimal | Amount ordered |
| unit_price | decimal | Agreed price |

**Problems identified:**
- References both product_id and material_id
- Suggests products and materials are used interchangeably

#### 12. Purchase Stock-In Items

Tracks received goods against purchase orders.

#### 13. Inventory Unit

Lookup table for measurement units.

---

## TARGET BUSINESS SEMANTICS

### 1. Canonical Identity

**Current State:**
- foods.food_code, material_archives.material_code
- No unified identity across the system
- Same physical item may have different codes in different contexts

**Target Semantics:**
Every business entity must have a single, globally unique identity that persists across all contexts (ordering, inventory, recipes, reporting).

**Business Rule:**
A tomato used in a salad and sold as a side dish must be the same entity in the system, even if referenced differently in different operations.

### 2. Type / Classification

**Current State:**
- foods.food_category (string)
- material_archives.category_id (FK)
- No unified type hierarchy
- Food vs Material distinction is unclear

**Target Semantics:**
Entities belong to explicit type hierarchies that determine behavior, valid operations, and required attributes.

**Business Rule:**
A "dish" (prepared item) and a "raw material" (ingredient) are fundamentally different types with different lifecycles, even if both appear in inventory.

### 3. Business Capability

**Current State:**
- No explicit capability mapping
- Tables organized by operational function (orders, inventory, purchases)
- No visibility into which business processes each entity supports

**Target Semantics:**
Each entity's role in business capabilities must be explicit. An entity may participate in multiple capabilities but its primary purpose must be clear.

**Business Rule:**
The same physical tomato participates in "Procurement" (when purchased), "Inventory Management" (when stored), "Menu Engineering" (when in a recipe), and "Sales" (when sold as part of a dish). These are distinct capabilities.

### 4. Role

**Current State:**
- Implicit roles through table placement
- material_archives serves as both "product to buy" and "ingredient to use"
- foods serves as both "menu item to sell" and "inventory item to track"

**Target Semantics:**
Entities play explicit roles in different contexts. The same canonical entity may have different roles in different business processes.

**Business Rule:**
Tomato (canonical identity) plays role of:
- **Purchasable Material** in procurement context
- **Inventory Item** in warehouse context
- **Recipe Ingredient** in menu context
- **Sellable Unit** in retail context (if sold separately)

### 5. Profile

**Current State:**
- Attributes scattered across tables
- foods has storage_conditions, nutrition_info
- material_archives has spec, origin, barcode
- No clear attribute ownership

**Target Semantics:**
Each entity type has a defined profile schema specifying which attributes are required, optional, or conditional based on type and role.

**Business Rule:**
A prepared dish has nutrition_info and allergen_warnings. A raw material has supplier_info and batch_tracking. These are different profiles for different types.

### 6. Relationship

**Current State:**
- dish_recipe links dish to ingredients
- purchase_order_items links orders to materials
- No explicit relationship types
- Relationship semantics inferred from context

**Target Semantics:**
Relationships are first-class entities with their own types, attributes, and lifecycle. A recipe is not just a link but a defined relationship with version, effective dates, and status.

**Business Rule:**
A recipe (relationship) between Dish A and Ingredient B has:
- Quantity ratio (1:0.5 kg)
- Effective date (from 2024-01-01)
- Status (active, deprecated)
- Variance tolerance (±10%)

### 7. Context

**Current State:**
- warehouse_id and store_id provide location context
- No explicit context model
- Same entity appears in multiple tables without context markers

**Target Semantics:**
All data exists within explicit contexts. The same entity in different contexts may have different valid states, attributes, and behaviors.

**Business Rule:**
Inventory of tomatoes at Warehouse A is contextually different from inventory at Store B. They may have different costs, different safety stocks, and different turnover rates.

### 8. Scope

**Current State:**
- department_id in material_archives
- store_id in store_inventory
- No clear organizational scope model

**Target Semantics:**
Entities exist within defined organizational scopes that determine visibility, access control, and data ownership.

**Business Rule:**
A recipe developed for Store Chain A may not be visible or valid for Store Chain B, even if they share the same parent organization.

### 9. State

**Current State:**
- No explicit state fields
- Legacy tables marked as deprecated in comments
- Lifecycle state inferred from table placement

**Target Semantics:**
Every entity has an explicit lifecycle state that determines valid operations and transitions.

**Business Rule:**
A material_archives entry progresses through:
- Draft → Active → Discontinued
- Each state has different valid operations (can't order a discontinued item)

---

## MAPPING TABLES

### Mapping: Canonical Identity

| Current Object | Current Identifier | Target Role | Identity Strategy |
|----------------|-------------------|-------------|-------------------|
| foods | food_code | Dish / Sellable Item | Merge with material_archives under unified ID |
| material_archives | material_code | Material / Resource | Primary canonical identity |
| product (legacy) | (unknown) | Deprecated | Map to material_archives or remove |
| dish_recipe | dish_id + ingredient_id | Recipe Relationship | Composite identity as relationship |

**Validation Case:**
A "Tomato Salad" (foods) uses "Tomato" (material_archives). Currently these have separate IDs. In target model, "Tomato" has one canonical ID used in inventory, procurement, and recipes.

### Mapping: Type / Classification

| Current Object | Current Type Field | Target Type | Type Hierarchy |
|----------------|-------------------|-------------|----------------|
| foods | food_category (string) | DishType | Dish → Prepared Food / Beverage / Side |
| material_archives | category_id (FK) | MaterialType | Material → Raw / Packaging / Supply |
| inventory | (implicit by table) | InventoryType | Inventory → Warehouse / Store / Transit |

**Validation Case:**
"Tomato" is MaterialType:Raw. "Tomato Salad" is DishType:Prepared. These are different types requiring different attributes and behaviors.

### Mapping: Business Capability

| Current Object | Current Capability | Target Capability | Capability Role |
|----------------|-------------------|-------------------|----------------|
| order_items | Sales | Sales | Transaction Line Item |
| purchase_order_items | Procurement | Procurement | Order Line Item |
| inventory + store_inventory | Stock Management | Inventory Management | Stock Position |
| dish_recipe + dish_recipe_new | Menu Engineering | Recipe Management | Ingredient Specification |
| inventory_transactions | Stock Movement | Inventory Management | Movement Record |

**Validation Case:**
When a customer orders "Tomato Salad", the system:
1. Creates order_items (Sales capability)
2. Decrements store_inventory (Inventory Management capability)
3. Logs transaction (Audit capability)
4. May trigger reorder (Procurement capability)

### Mapping: Role

| Current Object | Current Implicit Role | Target Roles | Role Context |
|----------------|----------------------|--------------|--------------|
| material_archives | "Product to order" | Purchasable, Storable, Consumable | Depends on usage context |
| foods | "Product to sell" | Sellable, Orderable, Trackable | Sales and service context |
| inventory | "Stock to manage" | Storable, Valuable, Reportable | Warehouse context |
| store_inventory | "Stock at store" | Storable, Consumable, Valuable | Store context |

**Validation Case:**
Tomato plays:
- In purchase_order_items: Role = Purchasable
- In inventory: Role = Storable
- In dish_recipe: Role = Consumable
- In store_inventory: Role = Storable (store location)

### Mapping: Profile

| Current Object | Current Attributes | Target Profile | Required vs Optional |
|----------------|-------------------|----------------|---------------------|
| foods | food_name, food_price, cost_price, ... | DishProfile | Required: name, price, category. Optional: nutrition, allergens |
| material_archives | material_name, unit, spec, ... | MaterialProfile | Required: name, unit, category. Optional: barcode, origin |
| inventory | current_stock, unit_cost, ... | InventoryPositionProfile | Required: quantity, cost. Optional: batch, safety levels |

**Validation Case:**
A DishProfile must have: name, price, category. Should have: nutrition_info, allergen_warnings. May have: preparation_time, serving_size.

A MaterialProfile must have: name, unit, category. Should have: supplier, shelf_life. May have: barcode, origin, storage_conditions.

### Mapping: Relationship

| Current Object | Current Structure | Target Relationship | Relationship Type |
|----------------|------------------|---------------------|-------------------|
| dish_recipe | dish_id → ingredient_id | RecipeIngredient | Composition: Dish HAS-INGREDIENT Material |
| purchase_order_items | order_id → material_id | PurchaseLineItem | Transaction: Order CONTAINS Material |
| order_items | order_id → food_id | SaleLineItem | Transaction: Order CONTAINS Dish |

**Validation Case:**
RecipeIngredient relationship between "Tomato Salad" and "Tomato":
- Quantity: 200g
- Unit: grams
- IsRequired: true
- AllowVariance: 10%
- EffectiveFrom: 2024-01-01
- Status: active

### Mapping: Context

| Current Object | Current Context | Target Context | Context Type |
|----------------|----------------|----------------|--------------|
| inventory | warehouse_id | WarehouseContext | Location-based |
| store_inventory | store_id | StoreContext | Location-based |
| material_archives | department_id | DepartmentContext | Organizational |
| purchase_order_items | (implicit from order) | ProcurementContext | Process-based |

**Validation Case:**
Tomato inventory exists in:
- WarehouseContext: 500 kg at $1.20/kg (bulk storage)
- StoreContext: 10 kg at $1.50/kg (retail ready)

### Mapping: Scope

| Current Object | Current Scope | Target Scope | Scope Level |
|----------------|--------------|--------------|-------------|
| material_archives | department_id | DepartmentScope | Organizational unit |
| store_inventory | store_id | StoreScope | Business location |
| foods | (none visible) | MenuScope | Menu/version |

**Validation Case:**
"Tomato Salad" exists in:
- MenuScope: Spring Menu 2024
- StoreScope: Downtown Location
- DepartmentScope: Kitchen Department

### Mapping: State

| Current Object | Current State (Inferred) | Target State | Valid Transitions |
|----------------|-------------------------|--------------|-------------------|
| material_archives | active (default) | Draft → Active → Discontinued | Based on lifecycle |
| foods | active (default) | Draft → Active → Seasonal → Discontinued | Menu-driven |
| dish_recipe | active (default) | Draft → Active → Deprecated → Archived | Version-driven |
| inventory | has stock | Normal → Low → OutOfStock → Overstock | Quantity-driven |

**Validation Case:**
Material "Tomato" states:
- Draft: Being evaluated for menu use
- Active: Available for ordering and recipes
- Discontinued: No longer sourced (supplier issue)

---

## VALIDATION MATRIX

### Business Case: New Dish "Grilled Chicken Salad"

| Step | Current System | Target System | Gap Identified |
|------|---------------|---------------|----------------|
| 1. Define dish | Insert into foods | Create DishProfile with Canonical Identity | No unified identity model |
| 2. Add ingredients | Insert into dish_recipe | Create RecipeIngredient relationships | Recipe is link, not relationship |
| 3. Source materials | Insert into material_archives | Create MaterialProfile if new | Duplicate identity systems |
| 4. Set pricing | Update food_price | Set price in DishProfile | Duplicate price fields |
| 5. Track inventory | Insert into inventory/store_inventory | Create InventoryPosition in context | No explicit context model |
| 6. Sell dish | Insert into order_items | Create SaleLineItem in Sales capability | Capability not explicit |
| 7. Reorder ingredients | Create purchase_order_items | Trigger Procurement capability | Capabilities not mapped |

### Business Case: Seasonal Menu Change

| Current Problem | Target Solution |
|-----------------|-----------------|
| No version control for recipes | Recipe relationship has version and effective dates |
| No seasonal marking | DishProfile has seasonalScope attribute |
| No deprecation workflow | State machine governs dish lifecycle |
| No scope control | MenuScope limits visibility |

### Business Case: Supplier Change for Material

| Current Problem | Target Solution |
|-----------------|-----------------|
| Material identity tied to one supplier | Canonical identity independent of supplier |
| No relationship tracking | MaterialSupplier relationship with effective dates |
| No quality tracking | QualityProfile as optional profile |
| No cost history | CostHistory as relationship attribute |

---

## CORRECTNESS PROOF

### Why Current Model Is Incomplete

1. **Identity Fragmentation**: Same physical item (tomato) appears as foods entry, material_archives entry, and implicitly in inventory. No canonical identity.

2. **Type Confusion**: "Food" vs "Material" distinction is artificial. A tomato is both food (edible) and material (ingredient). Current model forces a choice.

3. **Missing Context**: Same item in warehouse vs store has different meanings. Current model stores them separately without explicit context model.

4. **Implicit Roles**: material_archives is used as "what to order", "what to store", "what to cook with" without explicit role assignment.

5. **No Relationship Semantics**: dish_recipe is a simple junction table. A real recipe has version, status, effective dates, variance tolerances.

6. **State Inference**: Legacy tables marked as deprecated in comments rather than having proper state management.

### Why Target Model Is Correct

1. **Single Identity**: One canonical ID per entity, used across all contexts.

2. **Explicit Types**: Type determines valid attributes and behaviors.

3. **Contextual Existence**: All data exists in explicit contexts with clear ownership.

4. **Role-Based Usage**: Same entity plays different roles in different capabilities.

5. **Relationship as Entity**: Relationships have their own lifecycle and attributes.

6. **State Machine**: Explicit states with defined transitions and valid operations.

### Where Current Model Deviates

| Deviation | Impact | Remediation |
|-----------|--------|-------------|
| Duplicate price fields | Data inconsistency risk | Consolidate to single price in profile |
| Parallel recipe tables | Migration debt | Complete migration or remove old table |
| Implicit categories | Classification inconsistency | Implement explicit type hierarchy |
| No supplier relationship | Cannot track supplier changes | Create MaterialSupplier relationship |
| No allergen tracking | Regulatory compliance risk | Add AllergenProfile to DishProfile |

---

## RECOMMENDATIONS

### Immediate Actions

1. **Establish Canonical Identity**: Create unified identity model before any migration
2. **Define Type Hierarchy**: Formalize Dish vs Material distinction with clear rules
3. **Map Capabilities**: Document which tables support which business capabilities
4. **Audit Parallel Tables**: Resolve dish_recipe vs dish_recipe_new conflict

### Migration Strategy

1. **Phase 1**: Canonical Identity + Type System (foundation)
2. **Phase 2**: Profile + Relationship models (structure)
3. **Phase 3**: Context + Scope models (organization)
4. **Phase 4**: State machine (lifecycle)
5. **Phase 5**: Capability mapping (integration)

### Validation Criteria

Each phase must pass:
- **Identity Test**: Can all references to same physical item be resolved?
- **Type Test**: Are all attributes of each entity type valid?
- **Context Test**: Can same entity exist in multiple contexts without conflict?
- **State Test**: Are all state transitions valid and auditable?
- **Capability Test**: Does the model support all required business operations?

---

*Document Version: 1.0*
*Created: Phase 20 Conceptual Mapping*
*Status: Analysis Complete - Ready for Review*
