package com.news.kimo.utils;

/**
 * Generic pagination helper for Firebase RecyclerView adapters.
 * Tracks page state, loading status, and item count with loading footer support.
 *
 * @param <T> The model type being paginated
 */
public class PaginationHelper<T> {

    private final int pageSize;
    private final OnPageRequestListener<T> listener;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int currentPage = 0;
    private int totalItemCount = 0;

    // The last item key for Firebase pagination (document ID or key)
    private String lastItemKey = null;
    private String firstItemKey = null;

    /**
     * Listener interface for pagination events.
     */
    public interface OnPageRequestListener<T> {
        /**
         * Called when the next page should be loaded.
         *
         * @param page        The page number (0-indexed)
         * @param pageSize    The number of items to load
         * @param lastItemKey The key of the last item in the current list (for cursor-based)
         */
        void onLoadNextPage(int page, int pageSize, String lastItemKey);

        /**
         * Called when the previous page should be loaded.
         *
         * @param page         The page number
         * @param pageSize     The number of items to load
         * @param firstItemKey The key of the first item in the current list (for cursor-based)
         */
        void onLoadPreviousPage(int page, int pageSize, String firstItemKey);
    }

    /**
     * Creates a new PaginationHelper with default page size from Constants.
     *
     * @param listener The listener for page requests
     */
    public PaginationHelper(OnPageRequestListener<T> listener) {
        this(Constants.PAGINATION_SIZE, listener);
    }

    /**
     * Creates a new PaginationHelper with a custom page size.
     *
     * @param pageSize The number of items per page
     * @param listener The listener for page requests
     */
    public PaginationHelper(int pageSize, OnPageRequestListener<T> listener) {
        this.pageSize = pageSize;
        this.listener = listener;
    }

    /**
     * Request the next page of data.
     * Does nothing if currently loading or already at the last page.
     */
    public void nextPage() {
        if (isLoading || isLastPage) {
            return;
        }
        isLoading = true;
        listener.onLoadNextPage(currentPage, pageSize, lastItemKey);
    }

    /**
     * Request the previous page of data.
     * Does nothing if currently loading or at the first page.
     */
    public void previousPage() {
        if (isLoading || currentPage <= 0) {
            return;
        }
        isLoading = true;
        listener.onLoadPreviousPage(currentPage, pageSize, firstItemKey);
    }

    /**
     * Check if currently loading data.
     *
     * @return true if a page load is in progress
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * Check if the last page has been reached.
     *
     * @return true if there are no more pages to load
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Set the last page flag.
     *
     * @param lastPage true if no more pages are available
     */
    public void setLastPage(boolean lastPage) {
        this.isLastPage = lastPage;
    }

    /**
     * Reset all pagination state. Call when changing the data source.
     */
    public void reset() {
        this.isLoading = false;
        this.isLastPage = false;
        this.currentPage = 0;
        this.totalItemCount = 0;
        this.lastItemKey = null;
        this.firstItemKey = null;
    }

    /**
     * Get the current page number.
     *
     * @return The current page (0-indexed)
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Get the total number of items loaded so far.
     *
     * @return Total item count
     */
    public int getTotalItemCount() {
        return totalItemCount;
    }

    /**
     * Get the page size.
     *
     * @return Number of items per page
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get the item count including the loading footer.
     * If loading, returns totalItemCount + 1 for the footer.
     *
     * @return Item count including footer
     */
    public int getItemCount() {
        if (isLoading) {
            return totalItemCount + 1; // +1 for loading footer
        }
        return totalItemCount;
    }

    /**
     * Get the item type at a given position.
     *
     * @param position The adapter position
     * @return VIEW_TYPE_ITEM for normal items, VIEW_TYPE_LOADING for the loading footer
     */
    public int getItemViewType(int position) {
        return (position >= totalItemCount) ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    /**
     * Check if the loading footer is visible at the given position.
     *
     * @param position The adapter position
     * @return true if this position is the loading footer
     */
    public boolean isLoadingFooter(int position) {
        return position >= totalItemCount;
    }

    // View type constants
    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_LOADING = 1;

    // ============================================================
    // Methods to be called by the adapter after data is loaded
    // ============================================================

    /**
     * Call this after a next page has been successfully loaded.
     *
     * @param newItemsCount The number of items loaded in this page
     * @param newLastKey    The key of the last item in the new page
     */
    public void onPageLoaded(int newItemsCount, String newLastKey) {
        if (newItemsCount > 0) {
            if (firstItemKey == null && currentPage == 0) {
                // First page loaded - firstItemKey can be set by the adapter
            }
            this.lastItemKey = newLastKey;
        }
        if (newItemsCount < pageSize) {
            this.isLastPage = true;
        }
        this.totalItemCount += newItemsCount;
        this.currentPage++;
        this.isLoading = false;
    }

    /**
     * Call this after a previous page has been successfully loaded.
     *
     * @param newItemsCount The number of items loaded in this page
     * @param newFirstKey   The key of the first item in the new page
     */
    public void onPreviousPageLoaded(int newItemsCount, String newFirstKey) {
        if (newItemsCount > 0) {
            this.firstItemKey = newFirstKey;
        }
        this.totalItemCount += newItemsCount;
        this.currentPage--;
        this.isLoading = false;
    }

    /**
     * Set the first item key (call from adapter after first page loads).
     *
     * @param key The key of the first item
     */
    public void setFirstItemKey(String key) {
        this.firstItemKey = key;
    }

    /**
     * Set the last item key directly.
     *
     * @param key The key of the last item
     */
    public void setLastItemKey(String key) {
        this.lastItemKey = key;
    }

    /**
     * Set the total item count directly.
     *
     * @param count Total items loaded
     */
    public void setTotalItemCount(int count) {
        this.totalItemCount = count;
    }

    /**
     * Set the loading state directly.
     *
     * @param loading true if loading is in progress
     */
    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }

    /**
     * Set the current page number.
     *
     * @param page The page number
     */
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    /**
     * Called when a page load fails.
     */
    public void onPageLoadError() {
        this.isLoading = false;
    }
}
