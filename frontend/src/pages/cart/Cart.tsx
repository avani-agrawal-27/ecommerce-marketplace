import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
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
      setError("");

      const updatedCart = await updateCartItem(productId, { quantity });

      setCart(updatedCart);
    } catch (error) {
      console.error(error);
      setError("Unable to update cart item.");
    }
  };

  const handleRemove = async (productId: string) => {
    try {
      setError("");

      const updatedCart = await removeCartItem(productId);

      setCart(updatedCart);
    } catch (error) {
      console.error(error);
      setError("Unable to remove item.");
    }
  };

  const handleClearCart = async () => {
    try {
      setError("");

      await clearCart();
      await loadCart();
    } catch (error) {
      console.error(error);
      setError("Unable to clear cart.");
    }
  };

  if (loading) {
    return (
      <div className="cart-page">
        <div className="state-message">Loading cart...</div>
      </div>
    );
  }

  if (error && !cart) {
    return (
      <div className="cart-page">
        <div className="state-message error">{error}</div>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="cart-page">
        <div className="cart-header">
          <h1>Your Cart</h1>
        </div>

        <div className="empty-cart">
          <h2>Your cart is empty</h2>

          <p>Add some products to your cart to get started.</p>

          <Link to="/products">Continue Shopping</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <div className="cart-header">
        <div>
          <p className="eyebrow">Shopping Cart</p>

          <h1>Your Cart</h1>
        </div>

        <button
          type="button"
          className="clear-cart-button"
          onClick={handleClearCart}
        >
          Clear Cart
        </button>
      </div>

      {error && <div className="cart-error">{error}</div>}

      <div className="cart-items">
        {cart.items.map((item: CartResponse["items"][number]) => (
          <div key={item.id} className="cart-item">
            <div className="cart-item-info">
              <h2>{item.productName}</h2>

              <p>SKU: {item.sku}</p>

              <p className="cart-item-price">
                ₹{item.unitPrice.toFixed(2)} each
              </p>
            </div>

            <div className="cart-item-actions">
              <button
                type="button"
                className="cart-quantity-button"
                onClick={() =>
                  handleQuantityChange(item.productId, item.quantity - 1)
                }
                disabled={item.quantity <= 1}
                aria-label="Decrease quantity"
              >
                −
              </button>

              <span className="cart-quantity">{item.quantity}</span>

              <button
                type="button"
                className="cart-quantity-button"
                onClick={() =>
                  handleQuantityChange(item.productId, item.quantity + 1)
                }
                aria-label="Increase quantity"
              >
                +
              </button>

              <button
                type="button"
                className="cart-remove-button"
                onClick={() => handleRemove(item.productId)}
              >
                Remove
              </button>
            </div>

            <div className="cart-item-total">₹{item.subtotal.toFixed(2)}</div>
          </div>
        ))}
      </div>

      <div className="cart-summary">
        <h2>Order Summary</h2>

        <div className="cart-summary-row">
          <span>Subtotal</span>

          <span>₹{cart.totalAmount.toFixed(2)}</span>
        </div>

        <div className="cart-summary-total">
          <span>Total</span>

          <span>₹{cart.totalAmount.toFixed(2)}</span>
        </div>

        <button type="button" className="checkout-button">
          Checkout — Coming Soon
        </button>
      </div>
    </div>
  );
}

export default Cart;
