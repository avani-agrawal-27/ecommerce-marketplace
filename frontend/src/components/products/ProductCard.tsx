import type { Product } from "../../types/product";
import { Link } from "react-router-dom";

interface ProductCardProps {
  product: Product;
}

function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="product-card">
      <div className="product-image-placeholder">
        <span>Product</span>
      </div>

      <div className="product-card-body">
        <div className="product-category">{product.categoryName}</div>

        <h3 className="product-name">{product.name}</h3>

        <p className="product-description">
          {product.description || "No description available."}
        </p>

        <div className="product-card-footer">
          <div>
            <div className="product-price">₹{product.price.toFixed(2)}</div>

            <div className="product-sku">SKU: {product.sku}</div>
          </div>

          <span
            className={
              product.active ? "status-badge active" : "status-badge inactive"
            }
          >
            {product.active ? "Active" : "Inactive"}
          </span>
        </div>

        <Link className="view-product-button" to={`/products/${product.id}`}>
          View Product
        </Link>
      </div>
    </article>
  );
}

export default ProductCard;
