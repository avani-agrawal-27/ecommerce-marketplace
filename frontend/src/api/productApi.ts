import apiClient from "./client";
import type {
    Category,
    ProductPageResponse,
    Product
} from "../types/product";

export interface ProductQueryParams {
    page?: number;
    size?: number;
    categoryId?: string;
    active?: boolean;
    search?: string;
    sort?: string;
}
export const getProductById = async (
    id: string
): Promise<Product> => {
    const response = await apiClient.get<Product>(
        `/products/${id}`
    );

    return response.data;
};

export const getProducts = async (
    params: ProductQueryParams = {}
): Promise<ProductPageResponse> => {
    const response = await apiClient.get<ProductPageResponse>(
        "/products",
        { params }
    );

    return response.data;
};

export const getCategories = async (): Promise<Category[]> => {
    const response = await apiClient.get<Category[]>("/categories");

    return response.data;
};