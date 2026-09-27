import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getOrders } from "../../api/orderApi";
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

function getStatusClass(status: OrderStatus): string {
  return `order-status order-status-${status.toLowerCase()}`;
}

function OrderList() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadOrders = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await getOrders({
          page,
          size: 10,
          sortBy: "createdAt",
          direction: "desc",
        });

        setOrders(response.content);
        setTotalPages(response.totalPages);
      } catch (err) {
        console.error("Failed to load orders", err);
        setError("Unable to load your orders.");
      } finally {
        setLoading(false);
      }
    };

    loadOrders();
  }, [page]);

  if (loading) {
    return (
      <div className="page-container">
        <h1>My Orders</h1>
        <p>Loading orders...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container">
        <h1>My Orders</h1>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>My Orders</h1>
      </div>

      {orders.length === 0 ? (
        <div className="empty-state">
          <h2>No orders yet</h2>
          <p>You haven't placed any orders yet.</p>

          <Link to="/products">Continue Shopping</Link>
        </div>
      ) : (
        <>
          <div className="orders-list">
            {orders.map((order) => (
              <article key={order.id} className="order-card">
                <div className="order-card-header">
                  <div>
                    <h2>{order.orderNumber}</h2>

                    <p>Placed on {formatDate(order.createdAt)}</p>
                  </div>

                  <span className={getStatusClass(order.status)}>
                    {statusLabels[order.status]}
                  </span>
                </div>

                <div className="order-card-content">
                  <div>
                    <strong>Items</strong>

                    <p>
                      {order.items.length}{" "}
                      {order.items.length === 1 ? "item" : "items"}
                    </p>
                  </div>

                  <div>
                    <strong>Total</strong>

                    <p>{formatAmount(order.totalAmount, order.currency)}</p>
                  </div>
                </div>

                <div className="order-card-footer">
                  <Link to={`/orders/${order.id}`}>View Details</Link>
                </div>
              </article>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
                Previous
              </button>

              <span>
                Page {page + 1} of {totalPages}
              </span>

              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default OrderList;
