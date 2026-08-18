package com.voyageops.app.trip.infrastructure;

import com.voyageops.app.trip.domain.Activity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long > {
    List<Activity> findByTripId(Long tripId);
}
