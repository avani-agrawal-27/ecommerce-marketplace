function Header() {
    return (
        <header className="app-header">
            <div className="header-container">
                <a className="brand" href="/products">
                    <span className="brand-mark">E</span>
                    <span>E-Commerce Marketplace</span>
                </a>

                <nav className="header-nav">
                    <a href="/products">Products</a>
                    <a href="#">Categories</a>
                    <a href="#">Orders</a>
                    <button className="cart-button">
                        🛒 Cart
                    </button>
                </nav>
            </div>
        </header>
    );
}

export default Header;