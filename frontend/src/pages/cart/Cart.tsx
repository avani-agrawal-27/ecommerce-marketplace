import { useEffect, useState } from "react";
import type { CartResponse } from "../../types/cart";

import {
  clearCart,
  getCart,
  removeCartItem,
  updateCartItem,
} from "../../api/cartApi";

function Cart() {
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadCart = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getCart();

      setCart(data);
    } catch (error) {
      console.error(error);
      setError("Unable to load cart.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCart();
  }, []);

  const handleQuantityChange = async (productId: string, quantity: number) => {
    if (quantity < 1) {
      return;
    }

    try {
      const updatedCart = await updateCartItem(productId, { quantity });

      setCart(updatedCart);
    } catch (error) {
      console.error(error);
      setError("Unable to update cart item.");
    }
  };

  const handleRemove = async (productId: string) => {
    try {
      const updatedCart = await removeCartItem(productId);

      setCart(updatedCart);
    } catch (error) {
      console.error(error);
      setError("Unable to remove item.");
    }
  };

  const handleClearCart = async () => {
    try {
      await clearCart();

      await loadCart();
    } catch (error) {
      console.error(error);
      setError("Unable to clear cart.");
    }
  };

  if (loading) {
    return <p>Loading cart...</p>;
  }

  if (error && !cart) {
    return <p>{error}</p>;
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="cart-page">
        <h1>Your Cart</h1>
        <p>Your cart is empty.</p>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <div className="cart-header">
        <h1>Your Cart</h1>

        <button onClick={handleClearCart}>Clear Cart</button>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="cart-items">
        {cart.items.map((item: CartResponse["items"][number]) => (
          <div key={item.id} className="cart-item">
            <div>
              <h3>{item.productName}</h3>
              <p>SKU: {item.sku}</p>
              <p>₹{item.unitPrice.toFixed(2)}</p>
            </div>

            <div className="cart-item-actions">
              <button
                onClick={() =>
                  handleQuantityChange(item.productId, item.quantity - 1)
                }
                disabled={item.quantity <= 1}
              >
                -
              </button>

              <span>{item.quantity}</span>

              <button
                onClick={() =>
                  handleQuantityChange(item.productId, item.quantity + 1)
                }
              >
                +
              </button>

              <button onClick={() => handleRemove(item.productId)}>
                Remove
              </button>
            </div>

            <div>₹{item.subtotal.toFixed(2)}</div>
          </div>
        ))}
      </div>

      <div className="cart-summary">
        <h2>Total: ₹{cart.totalAmount.toFixed(2)}</h2>

        <button>Proceed to Checkout</button>
      </div>
    </div>
  );
}

export default Cart;
