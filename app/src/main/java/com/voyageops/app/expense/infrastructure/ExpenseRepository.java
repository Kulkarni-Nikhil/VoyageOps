package com.voyageops.app.expense.infrastructure;

import com.voyageops.app.expense.domain.Expense;
import com.voyageops.app.expense.domain.ExpenseCategory;
import com.voyageops.app.trip.domain.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = "paidBy")
    Page<Expense> findByTrip(Trip trip, Pageable pageable);

    Optional<Expense> findByIdAndTrip(Long id, Trip trip);

    long countByTrip(Trip trip);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.trip = :trip")
    BigDecimal sumAmountByTrip(@Param("trip") Trip trip);

    @Query("""
            select e.category as category, coalesce(sum(e.amount), 0) as total
            from Expense e
            where e.trip = :trip
            group by e.category
            """)
    List<CategoryTotal> groupSumByCategory(@Param("trip") Trip trip);

    interface CategoryTotal {
        ExpenseCategory getCategory();
        BigDecimal getTotal();
    }
}
