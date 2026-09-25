import apiClient from "./client";
import type {
    LoginRequest,
    RegisterRequest,
    AuthResponse,
    User
} from "../types/auth";

export const register = async (
    request: RegisterRequest
): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>(
        "/auth/register",
        request
    );

    return response.data;
};

export const login = async (
    request: LoginRequest
): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>(
        "/auth/login",
        request
    );

    return response.data;
};

export const getCurrentUser = async (): Promise<User> => {
    const response = await apiClient.get<User>("/users/me");

    return response.data;
};