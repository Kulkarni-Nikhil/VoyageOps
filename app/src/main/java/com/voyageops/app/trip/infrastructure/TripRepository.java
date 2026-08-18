package com.voyageops.app.trip.infrastructure;

import com.voyageops.app.trip.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByStartDate(LocalDate startDate);

    List<Trip> findByStartDateBetween(LocalDate from, LocalDate to);
}
