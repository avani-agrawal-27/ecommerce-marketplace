import type { ReactNode } from "react";
import Header from "./Header";

interface LayoutProps {
    children: ReactNode;
}

function Layout({ children }: LayoutProps) {
    return (
        <div className="app">
            <Header />

            <main className="main-container">
                {children}
            </main>

            <footer className="app-footer">
                <p>
                    E-Commerce Marketplace
                </p>
            </footer>
        </div>
    );
}

export default Layout;