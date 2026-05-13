# Inventory Contract

The Inventory Service owns live inventory counts. Backend samples consume this
contract when creating products, reading products, and creating orders.

Generated backend samples may use an in-memory Inventory stub for local tests,
but the implementation should keep the dependency boundary clear.

| Operation | Request | Response | Purpose |
|-----------|---------|----------|---------|
| addInventory(productId, inventory) | `{productid: int, inventory: int}` | `{message: string}` | Register initial stock for a newly created product |
| getInventory(productId) | `{productid: int}` | `{productid: int, inventory: int}` | Fetch live stock for product read responses |
| reduceInventory(productId, inventory) | `{productid: int, inventory: int}` | `{message: string}` | Decrement stock when an order is placed |

## Rules

- Product persistence stores product identity and descriptive fields.
- Inventory count comes from the Inventory Service for read responses.
- Creating a product calls `addInventory`.
- Creating an order calls `reduceInventory` with the order count.
- Deleting an order does not restore inventory.
