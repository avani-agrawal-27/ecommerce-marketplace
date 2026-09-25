export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName: string;
    lastName?: string;
}

export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string | null;
    roles: string[];
}

export interface AuthResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: User;
}