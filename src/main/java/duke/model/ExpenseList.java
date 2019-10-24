package duke.model;

import duke.commons.LogsCenter;
import duke.exception.DukeException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ExpenseList extends DukeList<Expense> {


    private static final Logger logger = LogsCenter.getLogger(ExpenseList.class);

    private enum SortCriteria {
        AMOUNT(Comparator.comparing(Expense::getAmount)),
        TIME(Comparator.comparing(Expense::getTime)),
        DESCRIPTION(Comparator.comparing(Expense::getDescription));

        private Comparator<Expense> comparator;

        SortCriteria(Comparator<Expense> comparator) {
            this.comparator = comparator;
        }
    }

    public enum ViewScopeName {
        DAY, WEEK, MONTH, YEAR, ALL;
    }

    public class ViewScope {
        private int viewScopeNumber;
        private ViewScopeName viewScopeName;

        public ViewScope(String viewScopeName, int viewScopeNumber) throws DukeException {
            this.viewScopeNumber = viewScopeNumber;
            try {
                this.viewScopeName = ViewScopeName.valueOf(viewScopeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new DukeException(String.format(
                    DukeException.MESSAGE_EXPENSE_VIEW_SCOPE_NAME_INVALID, viewScopeName));
            }
        }

        public ViewScope(ViewScopeName viewScopeName) {
            this.viewScopeNumber = 0;
            this.viewScopeName = viewScopeName;
        }


        private List<Expense> dayView(List<Expense> currentList) {
            return currentList.stream()
                .filter(e -> {
                    LocalDate dateOfExpense = e.getTime().toLocalDate();
                    LocalDate current = LocalDate.now().minusDays(viewScopeNumber);
                    return dateOfExpense.equals(current);
                })
                .collect(Collectors.toList());
        }

        private List<Expense> weekView(List<Expense> currentList) {
            return currentList.stream()
                .filter(e -> {
                    int dayOfWeek = e.getTime().getDayOfWeek().getValue();
                    LocalDate start = e.getTime().minusDays(dayOfWeek - 1).toLocalDate(); // Sunday of week of expense.
                    LocalDate end = e.getTime().plusDays(7 - dayOfWeek).toLocalDate(); // Monday of week of expense.
                    LocalDate current = LocalDate.now().minusWeeks(viewScopeNumber);

                    return (current.equals(end) || current.equals(start)
                        || (current.isAfter(start) && current.isBefore(end)));
                })
                .collect(Collectors.toList());
        }

        private List<Expense> monthView(List<Expense> currentList) {
            return currentList.stream()
                .filter(e -> {
                    LocalDate dateOfExpense = e.getTime().toLocalDate();
                    LocalDate current = LocalDate.now().minusMonths(viewScopeNumber);
                    boolean isSameYear = dateOfExpense.getYear() == current.getYear();
                    boolean isSameMonth = dateOfExpense.getMonth().equals(current.getMonth());
                    return (isSameYear && isSameMonth);
                })
                .collect(Collectors.toList());
        }

        private List<Expense> yearView(List<Expense> currentList) {
            return currentList.stream()
                .filter(e -> {
                    LocalDate dateOfExpense = e.getTime().toLocalDate();
                    LocalDate current = LocalDate.now().minusYears(viewScopeNumber);
                    return dateOfExpense.getYear() == current.getYear();
                })
                .collect(Collectors.toList());
        }

        /**
         * Returns a filtered list based on the view scope.
         * @param currentList List of Expenses we want to filter down
         * @return the filtered List of Expense
         */
        public List<Expense> view(List<Expense> currentList) {
            switch (viewScopeName) {
            case DAY:
                return dayView(currentList);

            case WEEK:
                return weekView(currentList);

            case MONTH:
                return monthView(currentList);

            case YEAR:
                return yearView(currentList);

            default: // case ALL:
                return currentList; // the viewScope here is ALL.
            }
        }

        public ViewScopeName getViewScopeName() {
            return viewScopeName;
        }
    }

    private SortCriteria sortCriteria;
    private ViewScope viewScope;
    private String filterCriteria;

    private List<Expense> filteredSortedViewedList;
    private ObservableList<Expense> internalFinalList;
    private ObservableList<Expense> externalFinalList;


    public ExpenseList(List<Expense> internalList) {
        super(internalList, "expense");
        viewScope = new ViewScope(ViewScopeName.ALL);
        sortCriteria = SortCriteria.TIME;
        externalList = FXCollections.observableArrayList();
        externalFinalList = FXCollections.unmodifiableObservableList(externalList);
        updateExternalList();
    }

    private void updateExternalList() {
        filteredSortedViewedList = filter(sort(view(internalList)));
        internalFinalList = FXCollections.observableArrayList(filteredSortedViewedList);
        externalList.setAll(internalFinalList);
    }

    @Override
    public void add(Expense expense) {
        super.add(expense);
        updateExternalList();
        logger.info("externalList lengths " + externalList.size());
    }

    @Override
    public void remove(int index) throws DukeException {
        super.remove(index);
        updateExternalList();
    }

    @Override
    public void clear() {
        super.clear();
        updateExternalList();
    }

    /**
     * Updates {@code externalList}, then returns it.
     *
     * @return {@code externalList}.
     */
    @Override
    public ObservableList<Expense> getExternalList() {
        return externalFinalList;
    }

    @Override
    public List<Expense> getInternalList() {
        return internalList;
    }

    /**
     * Sets the sort criteria.
     * Sort criteria include AMOUNT, TIME, DESCRIPTION.
     *
     * @param sortCriteria The String indicating the criteria for sorting.
     * @throws DukeException If the format of sort criteria is incorrect.
     */
    @Override
    public void setSortCriteria(String sortCriteria) throws DukeException {
        try {
            this.sortCriteria = SortCriteria.valueOf(sortCriteria.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DukeException(String.format(DukeException.MESSAGE_EXPENSE_SORT_CRITERIA_INVALID, sortCriteria));
        }
        updateExternalList();
    }

    @Override
    public void setFilterCriteria(String filterCriteria) throws DukeException {
        this.filterCriteria = filterCriteria;
        updateExternalList();
    }

    public SortCriteria getSortCriteria() {
        return sortCriteria;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public ViewScope getViewScope() {
        return viewScope;
    }

    /**
     * Sets the view scope.
     * View scopes include DAY, WEEK, MONTH, YEAR, ALL;
     *
     * @param viewScopeName The string indicating the time scope of displayed list.
     * @throws DukeException If the format of view scope is incorrect.
     */
    @Override
    public void setViewScope(String viewScopeName, int viewScopeNumber) throws DukeException {
        this.viewScope = new ViewScope(viewScopeName, viewScopeNumber);
        updateExternalList();
    }

    /**
     * Sorts the given List with the given criteria and returns the sorted List.
     *
     * @param currentList The List going to be sorted.
     * @return The sorted List.
     */
    @Override
    public List<Expense> sort(List<Expense> currentList) {
        currentList.sort(sortCriteria.comparator);
        return currentList;
    }

    /**
     * To be implemented when tags are specified.
     *
     * @param currentList The List going to be filtered.
     * @return The filtered List.
     */
    @Override
    public List<Expense> filter(List<Expense> currentList) {
        return currentList;
    }

    /**
     * Tailors the given List so that only {@code Expense} within the given time scope are preserved.
     * The time scope is composed of time unit(e.g. week) and how many (e.g. weeks) ago.
     * Returns the tailored List.
     *
     * @param currentList The list going to be modified.
     * @return The tailored List.
     */
    @Override
    public List<Expense> view(List<Expense> currentList) {
        return viewScope.view(currentList);
    }

    /**
     * Returns an item from its storage string. Although this method is present in the item builders,
     * it is declared here to make it easier to implement (otherwise requires reflection).
     *
     * @param storageString the storage string of the item.
     * @return the item.
     * @throws DukeException if the item could not be created from the storage string.
     */
    public static Expense itemFromStorageString(String storageString) throws DukeException {
        return new Expense.Builder(storageString).build();
    }

    /**
     * Returns the total amount of money spent.
     *
     * @return BigDecimal of the total amount of money spent.
     */
    public BigDecimal getTotalAmount() {
        return internalList.stream()
                .filter(expense -> !expense.isTentative())
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * returns the total Amount given a specific tag.
     *
     * @param tag the tag of
     * @return A BigDecimal which is the sum of all items of a single tag
     */
    public BigDecimal getTagAmount(String tag) {
        return internalList.stream()
                .filter(expense -> expense.getTags().contains(tag))
                .filter(expense -> !expense.isTentative())
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the total amount of money spent on currently visible expenses i.e. those in {@code externalList}.
     *
     * @return BigDecimal of the total amount of money spent on currently visible expenses.
     */
    public BigDecimal getTotalExternalAmount() {
        return externalList.stream()
            .filter(expense -> !expense.isTentative())
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}