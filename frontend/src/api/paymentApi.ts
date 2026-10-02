import apiClient from "./client";
import type { CreatePaymentRequest, PaymentResponse } from "../types/payment";

export const createPayment = async (
  request: CreatePaymentRequest,
): Promise<PaymentResponse> => {
  const response = await apiClient.post<PaymentResponse>("/payments", request);

  return response.data;
};
