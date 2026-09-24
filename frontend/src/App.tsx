import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from "react-router-dom";

import Layout from "./components/layout/Layout";
import ProductList from "./pages/products/ProductList";
import ProductDetails from "./pages/products/ProductDetails";

function App() {
    return (
        <BrowserRouter>
            <Layout>
                <Routes>
                    <Route
                        path="/"
                        element={
                            <Navigate
                                to="/products"
                                replace
                            />
                        }
                    />

                    <Route
                        path="/products"
                        element={<ProductList />}
                    />
                    <Route
                        path="/products/:id"
                        element={<ProductDetails />}
                    />
                </Routes>
            </Layout>
        </BrowserRouter>
    );
}

export default App;