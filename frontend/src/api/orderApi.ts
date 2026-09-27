import apiClient from "./client";
import type {
  CreateOrderRequest,
  Order,
  OrderPageResponse,
  OrderStatus,
} from "../types/order";

export interface GetOrdersParams {
  status?: OrderStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: "asc" | "desc";
}

export const getOrders = async (
  params?: GetOrdersParams,
): Promise<OrderPageResponse> => {
  const response = await apiClient.get<OrderPageResponse>("/orders", {
    params: {
      page: params?.page ?? 0,
      size: params?.size ?? 10,
      sortBy: params?.sortBy ?? "createdAt",
      direction: params?.direction ?? "desc",
      ...(params?.status
        ? {
            status: params.status,
          }
        : {}),
    },
  });

  return response.data;
};

export const getOrderById = async (orderId: string): Promise<Order> => {
  const response = await apiClient.get<Order>(`/orders/${orderId}`);

  return response.data;
};

export const createOrder = async (
  request: CreateOrderRequest,
): Promise<Order> => {
  const response = await apiClient.post<Order>("/orders", request);

  return response.data;
};
