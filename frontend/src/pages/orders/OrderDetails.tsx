import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getOrderById } from "../../api/orderApi";
import { createPayment } from "../../api/paymentApi";
import type { Order, OrderStatus } from "../../types/order";

const statusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "Pending Payment",
  CONFIRMED: "Confirmed",
  PROCESSING: "Processing",
  SHIPPED: "Shipped",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

function formatDate(value: string): string {
  return new Date(value).toLocaleString();
}

function formatAmount(amount: number, currency: string): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(amount);
}

function OrderDetails() {
  const { orderId } = useParams<{ orderId: string }>();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState("");
  const [paymentMessage, setPaymentMessage] = useState("");

  useEffect(() => {
    const loadOrder = async () => {
      if (!orderId) {
        setError("Order ID is missing.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");

        const response = await getOrderById(orderId);

        setOrder(response);
      } catch (err) {
        console.error("Failed to load order", err);

        setError("Unable to load this order.");
      } finally {
        setLoading(false);
      }
    };

    loadOrder();
  }, [orderId]);
  const handlePayment = async () => {
    if (!orderId || !order) {
      return;
    }

    try {
      setPaying(true);
      setError("");
      setPaymentMessage("");

      await createPayment({
        orderId,
      });

      // Reload the order from the backend so that
      // the UI reflects the actual server-side status.
      const updatedOrder = await getOrderById(orderId);

      setOrder(updatedOrder);

      setPaymentMessage("Payment successful. Your order is confirmed.");
    } catch (err) {
      console.error("Payment failed", err);

      setError("Payment could not be completed. Please try again.");
    } finally {
      setPaying(false);
    }
  };
  if (loading) {
    return (
      <div className="page-container">
        <p>Loading order...</p>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="page-container">
        <h1>Order Details</h1>

        <p>{error || "Order not found."}</p>

        <Link to="/orders">Back to Orders</Link>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <Link to="/orders">← Back to Orders</Link>

          <h1>Order {order.orderNumber}</h1>

          <p>Placed on {formatDate(order.createdAt)}</p>
        </div>

        <span
          className={`order-status order-status-${order.status.toLowerCase()}`}
        >
          {statusLabels[order.status]}
        </span>
      </div>
      {paymentMessage && (
        <div className="success-message">{paymentMessage}</div>
      )}

      {error && <div className="error-message">{error}</div>}

      {order.status === "PENDING_PAYMENT" && (
        <section className="payment-action">
          <div>
            <h2>Payment Required</h2>
            <p>Complete payment to confirm this order.</p>
          </div>

          <button
            type="button"
            className="checkout-button"
            onClick={handlePayment}
            disabled={paying}
          >
            {paying ? "Processing Payment..." : "Pay Now"}
          </button>
        </section>
      )}
      <section className="order-section">
        <h2>Items</h2>

        <div className="order-items">
          {order.items.map((item) => (
            <div key={item.id} className="order-item">
              <div>
                <h3>{item.productName}</h3>

                <p>SKU: {item.sku}</p>

                <p>Quantity: {item.quantity}</p>
              </div>

              <div>
                <p>
                  {formatAmount(item.unitPrice, order.currency)} ×{" "}
                  {item.quantity}
                </p>

                <strong>{formatAmount(item.lineTotal, order.currency)}</strong>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="order-section">
        <h2>Shipping Address</h2>

        <div className="shipping-address">
          <p>
            <strong>
              {order.shippingFirstName} {order.shippingLastName || ""}
            </strong>
          </p>

          <p>{order.shippingAddressLine1}</p>

          {order.shippingAddressLine2 && <p>{order.shippingAddressLine2}</p>}

          <p>
            {order.shippingCity}, {order.shippingState}{" "}
            {order.shippingPostalCode}
          </p>

          <p>{order.shippingCountry}</p>

          <p>Phone: {order.shippingPhone}</p>
        </div>
      </section>

      <section className="order-section order-summary">
        <h2>Order Summary</h2>

        <div className="summary-row">
          <span>Subtotal</span>
          <span>{formatAmount(order.subtotal, order.currency)}</span>
        </div>

        <div className="summary-row">
          <span>Shipping</span>
          <span>
            {order.shippingFee === 0
              ? "Free"
              : formatAmount(order.shippingFee, order.currency)}
          </span>
        </div>

        <div className="summary-row summary-total">
          <strong>Total</strong>

          <strong>{formatAmount(order.totalAmount, order.currency)}</strong>
        </div>
      </section>
    </div>
  );
}

export default OrderDetails;
