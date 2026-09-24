import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { getProductById } from "../../api/productApi";
import type { Product } from "../../types/product";

function ProductDetails() {
    const { id } = useParams<{ id: string }>();

    const [product, setProduct] =
        useState<Product | null>(null);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState("");

    useEffect(() => {
        if (!id) {
            setError("Product ID is missing");
            setLoading(false);
            return;
        }

        const loadProduct = async () => {
            try {
                setLoading(true);
                setError("");

                const data =
                    await getProductById(id);

                setProduct(data);
            } catch {
                setError(
                    "Failed to load product"
                );
            } finally {
                setLoading(false);
            }
        };

        loadProduct();
    }, [id]);

    if (loading) {
        return (
            <div className="state-message">
                Loading product...
            </div>
        );
    }

    if (error) {
        return (
            <div className="state-message error">
                {error}
            </div>
        );
    }

    if (!product) {
        return (
            <div className="state-message">
                Product not found.
            </div>
        );
    }

    return (
        <section className="product-details-page">
            <Link
                className="back-link"
                to="/products"
            >
                ← Back to Products
            </Link>

            <div className="product-details">
                <div className="product-details-image">
                    <span>Product</span>
                </div>

                <div className="product-details-content">
                    <p className="product-category">
                        {product.categoryName}
                    </p>

                    <h1>
                        {product.name}
                    </h1>

                    <p className="product-details-description">
                        {product.description ||
                            "No description available."}
                    </p>

                    <div className="product-details-price">
                        ₹{product.price.toFixed(2)}
                    </div>

                    <div className="product-meta">
                        <div>
                            <strong>
                                SKU
                            </strong>
                            <span>
                                {product.sku}
                            </span>
                        </div>

                        <div>
                            <strong>
                                Status
                            </strong>
                            <span>
                                {product.active
                                    ? "Active"
                                    : "Inactive"}
                            </span>
                        </div>
                    </div>

                    <button
                        className="view-product-button"
                        disabled={!product.active}
                    >
                        Add to Cart
                    </button>
                </div>
            </div>
        </section>
    );
}

export default ProductDetails;