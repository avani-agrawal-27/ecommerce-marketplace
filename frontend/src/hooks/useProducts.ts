import { useCallback, useEffect, useState } from "react";
import {
    getCategories,
    getProducts,
} from "../api/productApi";
import type {
    Category,
    Product,
    ProductPageResponse,
} from "../types/product";

function useProducts() {
    const [products, setProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);

    const [search, setSearch] = useState("");
    const [appliedSearch, setAppliedSearch] = useState("");

    const [categoryId, setCategoryId] = useState("");
    const [active, setActive] = useState("");
    const [sort, setSort] = useState("createdAt,desc");

    const [page, setPage] = useState(0);
    const [pageSize] = useState(10);

    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const loadCategories = useCallback(async () => {
        try {
            const data = await getCategories();
            setCategories(data);
        } catch {
            setError("Failed to load categories");
        }
    }, []);

    const loadProducts = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const response: ProductPageResponse =
                await getProducts({
                    page,
                    size: pageSize,
                    categoryId: categoryId || undefined,
                    active:
                        active === ""
                            ? undefined
                            : active === "true",
                    search: appliedSearch || undefined,
                    sort,
                });

            setProducts(response.content);
            setTotalPages(response.totalPages);
            setTotalElements(response.totalElements);
        } catch {
            setError("Failed to load products");
        } finally {
            setLoading(false);
        }
    }, [
        page,
        pageSize,
        categoryId,
        active,
        appliedSearch,
        sort,
    ]);

    useEffect(() => {
        loadCategories();
    }, [loadCategories]);

    useEffect(() => {
        loadProducts();
    }, [loadProducts]);

    const handleSearch = () => {
        setPage(0);
        setAppliedSearch(search.trim());
    };

    const handleCategoryChange = (value: string) => {
        setCategoryId(value);
        setPage(0);
    };

    const handleActiveChange = (value: string) => {
        setActive(value);
        setPage(0);
    };

    const handleSortChange = (value: string) => {
        setSort(value);
        setPage(0);
    };

    const goToPreviousPage = () => {
        setPage((current) => Math.max(current - 1, 0));
    };

    const goToNextPage = () => {
        setPage((current) =>
            Math.min(current + 1, totalPages - 1)
        );
    };

    return {
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
    };
}

export default useProducts;