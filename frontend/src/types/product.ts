export interface Product {
    id: string;
    categoryId: string;
    categoryName: string;
    name: string;
    slug: string;
    description: string | null;
    sku: string;
    price: number;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface ProductPageResponse {
    content: Product[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

export interface Category {
    id: string;
    name: string;
    slug: string;
    description: string | null;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}