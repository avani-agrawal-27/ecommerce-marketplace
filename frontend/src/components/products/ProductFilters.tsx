import type {
    ChangeEvent,
    KeyboardEvent,
} from "react";

import type { Category } from "../../types/product";

interface ProductFiltersProps {
    search: string;
    categories: Category[];
    categoryId: string;
    active: string;
    sort: string;

    onSearchChange: (value: string) => void;
    onSearch: () => void;
    onCategoryChange: (value: string) => void;
    onActiveChange: (value: string) => void;
    onSortChange: (value: string) => void;
}

function ProductFilters({
    search,
    categories,
    categoryId,
    active,
    sort,
    onSearchChange,
    onSearch,
    onCategoryChange,
    onActiveChange,
    onSortChange,
}: ProductFiltersProps) {
    const handleSearchKeyDown = (
        event: KeyboardEvent<HTMLInputElement>
    ) => {
        if (event.key === "Enter") {
            onSearch();
        }
    };

    const handleCategoryChange = (
        event: ChangeEvent<HTMLSelectElement>
    ) => {
        onCategoryChange(event.target.value);
    };

    const handleActiveChange = (
        event: ChangeEvent<HTMLSelectElement>
    ) => {
        onActiveChange(event.target.value);
    };

    const handleSortChange = (
        event: ChangeEvent<HTMLSelectElement>
    ) => {
        onSortChange(event.target.value);
    };

    return (
        <aside className="filters-panel">
            <div className="filter-section">
                <label className="filter-label">
                    Search
                </label>

                <div className="search-wrapper">
                    <input
                        className="search-input"
                        type="text"
                        placeholder="Search products..."
                        value={search}
                        onChange={(event) =>
                            onSearchChange(
                                event.target.value
                            )
                        }
                        onKeyDown={handleSearchKeyDown}
                    />

                    <button
                        className="search-button"
                        onClick={onSearch}
                    >
                        Search
                    </button>
                </div>
            </div>

            <div className="filter-section">
                <label
                    className="filter-label"
                    htmlFor="category"
                >
                    Category
                </label>

                <select
                    id="category"
                    className="filter-select"
                    value={categoryId}
                    onChange={handleCategoryChange}
                >
                    <option value="">
                        All Categories
                    </option>

                    {categories.map((category) => (
                        <option
                            key={category.id}
                            value={category.id}
                        >
                            {category.name}
                        </option>
                    ))}
                </select>
            </div>

            <div className="filter-section">
                <label
                    className="filter-label"
                    htmlFor="status"
                >
                    Status
                </label>

                <select
                    id="status"
                    className="filter-select"
                    value={active}
                    onChange={handleActiveChange}
                >
                    <option value="">All</option>
                    <option value="true">
                        Active
                    </option>
                    <option value="false">
                        Inactive
                    </option>
                </select>
            </div>

            <div className="filter-section">
                <label
                    className="filter-label"
                    htmlFor="sort"
                >
                    Sort By
                </label>

                <select
                    id="sort"
                    className="filter-select"
                    value={sort}
                    onChange={handleSortChange}
                >
                    <option value="createdAt,desc">
                        Newest
                    </option>

                    <option value="createdAt,asc">
                        Oldest
                    </option>

                    <option value="name,asc">
                        Name A-Z
                    </option>

                    <option value="name,desc">
                        Name Z-A
                    </option>

                    <option value="price,asc">
                        Price Low-High
                    </option>

                    <option value="price,desc">
                        Price High-Low
                    </option>
                </select>
            </div>
        </aside>
    );
}

export default ProductFilters;