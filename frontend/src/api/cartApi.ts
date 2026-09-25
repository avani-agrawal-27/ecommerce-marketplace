import apiClient from "./client";
import type {
  AddCartItemRequest,
  CartResponse,
  UpdateCartItemRequest,
} from "../types/cart";

export const getCart = async (): Promise<CartResponse> => {
  const response = await apiClient.get<CartResponse>("/cart");
  return response.data;
};

export const addToCart = async (
  request: AddCartItemRequest,
): Promise<CartResponse> => {
  const response = await apiClient.post<CartResponse>("/cart/items", request);

  return response.data;
};

export const updateCartItem = async (
  productId: string,
  request: UpdateCartItemRequest,
): Promise<CartResponse> => {
  const response = await apiClient.patch<CartResponse>(
    `/cart/items/${productId}`,
    request,
  );

  return response.data;
};

export const removeCartItem = async (
  productId: string,
): Promise<CartResponse> => {
  const response = await apiClient.delete<CartResponse>(
    `/cart/items/${productId}`,
  );

  return response.data;
};

export const clearCart = async (): Promise<void> => {
  await apiClient.delete("/cart");
};
