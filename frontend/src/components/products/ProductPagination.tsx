interface ProductPaginationProps {
    page: number;
    totalPages: number;
    onPrevious: () => void;
    onNext: () => void;
}

function ProductPagination({
    page,
    totalPages,
    onPrevious,
    onNext,
}: ProductPaginationProps) {
    if (totalPages <= 1) {
        return null;
    }

    return (
        <div className="pagination">
            <button
                className="pagination-button"
                disabled={page === 0}
                onClick={onPrevious}
            >
                Previous
            </button>

            <span className="pagination-info">
                Page {page + 1} of {totalPages}
            </span>

            <button
                className="pagination-button"
                disabled={
                    page >= totalPages - 1
                }
                onClick={onNext}
            >
                Next
            </button>
        </div>
    );
}

export default ProductPagination;