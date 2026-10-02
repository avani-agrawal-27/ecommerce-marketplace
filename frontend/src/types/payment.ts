export interface CreatePaymentRequest {
  orderId: string;
}

export interface PaymentResponse {
  id: string;
  orderId: string;
}
