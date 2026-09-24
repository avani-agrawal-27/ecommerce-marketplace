import ProductCard from "../../components/products/ProductCard";
import ProductFilters from "../../components/products/ProductFilters";
import ProductPagination from "../../components/products/ProductPagination";
import useProducts from "../../hooks/useProducts";

function ProductList() {
    const {
        products,
        categories,

        search,
        setSearch,

        categoryId,
        active,
        sort,

        page,
        totalPages,
        totalElements,

        loading,
        error,

        handleSearch,
        handleCategoryChange,
        handleActiveChange,
        handleSortChange,

        goToPreviousPage,
        goToNextPage,
    } = useProducts();

    return (
        <section className="catalog-page">
            <div className="catalog-header">
                <div>
                    <p className="eyebrow">
                        Marketplace
                    </p>

                    <h1>
                        Product Catalog
                    </h1>

                    <p className="catalog-description">
                        Browse our products and find
                        what you're looking for.
                    </p>
                </div>

                <div className="product-count">
                    {totalElements} products
                </div>
            </div>

            <div className="catalog-layout">
                <ProductFilters
                    search={search}
                    categories={categories}
                    categoryId={categoryId}
                    active={active}
                    sort={sort}
                    onSearchChange={setSearch}
                    onSearch={handleSearch}
                    onCategoryChange={
                        handleCategoryChange
                    }
                    onActiveChange={
                        handleActiveChange
                    }
                    onSortChange={handleSortChange}
                />

                <div className="catalog-results">
                    {loading && (
                        <div className="state-message">
                            Loading products...
                        </div>
                    )}

                    {error && (
                        <div className="state-message error">
                            {error}
                        </div>
                    )}

                    {!loading &&
                        !error &&
                        products.length === 0 && (
                            <div className="state-message">
                                No products found.
                            </div>
                        )}

                    {!loading &&
                        !error &&
                        products.length > 0 && (
                            <>
                                <div className="product-grid">
                                    {products.map(
                                        (product) => (
                                            <ProductCard
                                                key={
                                                    product.id
                                                }
                                                product={
                                                    product
                                                }
                                            />
                                        )
                                    )}
                                </div>

                                <ProductPagination
                                    page={page}
                                    totalPages={
                                        totalPages
                                    }
                                    onPrevious={
                                        goToPreviousPage
                                    }
                                    onNext={
                                        goToNextPage
                                    }
                                />
                            </>
                        )}
                </div>
            </div>
        </section>
    );
}

export default ProductList;