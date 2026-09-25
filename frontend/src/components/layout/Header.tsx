import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";

const Header = () => {
    const { user, isAuthenticated, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    return (
      <header className="app-header">
        <div className="header-container">
          <Link to="/products" className="brand">
            <span className="brand-mark">M</span>
            Marketplace
          </Link>

          <nav className="header-nav">
            <Link to="/products">Products</Link>

            <a href="#">Categories</a>

            <a href="#">Orders</a>

            {isAuthenticated ? (
              <>
                <span className="user-greeting">Hello, {user?.firstName}</span>

                <button
                  type="button"
                  className="logout-button"
                  onClick={handleLogout}
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login">Login</Link>

                <Link to="/register">Register</Link>
              </>
            )}

            <Link to="/cart" className="cart-link">
              Cart
            </Link>
          </nav>
        </div>
      </header>
    );
};

export default Header;