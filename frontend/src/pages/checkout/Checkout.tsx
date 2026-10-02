import { useEffect, useState } from "react";
import type { SubmitEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getCart } from "../../api/cartApi";
import { createOrder } from "../../api/orderApi";
import type { CartResponse } from "../../types/cart";

interface ShippingForm {
  shippingFirstName: string;
  shippingLastName: string;
  shippingAddressLine1: string;
  shippingAddressLine2: string;
  shippingCity: string;
  shippingState: string;
  shippingPostalCode: string;
  shippingCountry: string;
  shippingPhone: string;
}

const initialForm: ShippingForm = {
  shippingFirstName: "",
  shippingLastName: "",
  shippingAddressLine1: "",
  shippingAddressLine2: "",
  shippingCity: "",
  shippingState: "",
  shippingPostalCode: "",
  shippingCountry: "India",
  shippingPhone: "",
};

function Checkout() {
  const navigate = useNavigate();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [form, setForm] = useState<ShippingForm>(initialForm);
  const [loading, setLoading] = useState(true);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadCart = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await getCart();

        if (!response.items || response.items.length === 0) {
          navigate("/cart", { replace: true });
          return;
        }

        setCart(response);
      } catch (err) {
        console.error(err);
        setError("Unable to load your cart. Please try again.");
      } finally {
        setLoading(false);
      }
    };

    loadCart();
  }, [navigate]);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault();

    if (!cart || cart.items.length === 0) {
      return;
    }

    try {
      setPlacingOrder(true);
      setError("");

      const order = await createOrder({
        shippingFirstName: form.shippingFirstName.trim(),
        shippingLastName: form.shippingLastName.trim() || undefined,
        shippingAddressLine1: form.shippingAddressLine1.trim(),
        shippingAddressLine2: form.shippingAddressLine2.trim() || undefined,
        shippingCity: form.shippingCity.trim(),
        shippingState: form.shippingState.trim(),
        shippingPostalCode: form.shippingPostalCode.trim(),
        shippingCountry: form.shippingCountry.trim(),
        shippingPhone: form.shippingPhone.trim(),
      });

      navigate(`/orders/${order.id}`);
    } catch (err) {
      console.error(err);
      setError(
        "Unable to place the order. Please check your details and try again.",
      );
    } finally {
      setPlacingOrder(false);
    }
  };

  if (loading) {
    return (
      <main className="page-container">
        <p>Loading checkout...</p>
      </main>
    );
  }

  if (error && !cart) {
    return (
      <main className="page-container">
        <p className="error-message">{error}</p>
        <Link to="/cart" className="secondary-button">
          Back to Cart
        </Link>
      </main>
    );
  }

  if (!cart) {
    return null;
  }

  return (
    <main className="page-container">
      <div className="page-header">
        <div>
          <h1>Checkout</h1>
          <p>Enter your delivery information to place your order.</p>
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}

      <div className="checkout-layout">
        <form className="checkout-form" onSubmit={handleSubmit}>
          <section className="checkout-section">
            <h2>Delivery Information</h2>

            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="shippingFirstName">First Name</label>
                <input
                  id="shippingFirstName"
                  name="shippingFirstName"
                  type="text"
                  value={form.shippingFirstName}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="shippingLastName">Last Name</label>
                <input
                  id="shippingLastName"
                  name="shippingLastName"
                  type="text"
                  value={form.shippingLastName}
                  onChange={handleChange}
                />
              </div>

              <div className="form-field full-width">
                <label htmlFor="shippingAddressLine1">Address Line 1</label>
                <input
                  id="shippingAddressLine1"
                  name="shippingAddressLine1"
                  type="text"
                  value={form.shippingAddressLine1}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-field full-width">
                <label htmlFor="shippingAddressLine2">Address Line 2</label>
                <input
                  id="shippingAddressLine2"
                  name="shippingAddressLine2"
                  type="text"
                  value={form.shippingAddressLine2}
                  onChange={handleChange}
                />
              </div>

              <div className="form-field">
                <label htmlFor="shippingCity">City</label>
                <input
                  id="shippingCity"
                  name="shippingCity"
                  type="text"
                  value={form.shippingCity}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="shippingState">State</label>
                <input
                  id="shippingState"
                  name="shippingState"
                  type="text"
                  value={form.shippingState}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="shippingPostalCode">Postal Code</label>
                <input
                  id="shippingPostalCode"
                  name="shippingPostalCode"
                  type="text"
                  value={form.shippingPostalCode}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="shippingCountry">Country</label>
                <input
                  id="shippingCountry"
                  name="shippingCountry"
                  type="text"
                  value={form.shippingCountry}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-field">
                <label htmlFor="shippingPhone">Phone</label>
                <input
                  id="shippingPhone"
                  name="shippingPhone"
                  type="tel"
                  value={form.shippingPhone}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </section>

          <div className="checkout-actions">
            <Link to="/cart" className="secondary-button">
              Back to Cart
            </Link>

            <button
              type="submit"
              className="checkout-button"
              disabled={placingOrder}
            >
              {placingOrder ? "Placing Order..." : "Place Order"}
            </button>
          </div>
        </form>

        <aside className="checkout-summary">
          <h2>Order Summary</h2>

          <div className="checkout-items">
            {cart.items.map((item) => (
              <div className="checkout-item" key={item.id}>
                <div>
                  <strong>{item.productName}</strong>
                  <span>
                    Qty: {item.quantity} × ₹{item.unitPrice.toFixed(2)}
                  </span>
                </div>

                <strong>₹{item.subtotal.toFixed(2)}</strong>
              </div>
            ))}
          </div>

          <div className="summary-row">
            <span>Subtotal</span>
            <span>₹{cart.totalAmount.toFixed(2)}</span>
          </div>

          <div className="summary-row">
            <span>Shipping</span>
            <span>Calculated with order</span>
          </div>

          <div className="summary-total">
            <span>Cart Total</span>
            <strong>₹{cart.totalAmount.toFixed(2)}</strong>
          </div>
        </aside>
      </div>
    </main>
  );
}

export default Checkout;
