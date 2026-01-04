package com.xshopai.orderprocessor.repository;

import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order Processing Saga entities
 */
@Repository
public interface OrderProcessingSagaRepository extends JpaRepository<OrderProcessingSaga, UUID> {

    /**
     * Find saga by order ID
     */
    Optional<OrderProcessingSaga> findByOrderId(UUID orderId);

    /**
     * Find all sagas with specific status
     */
    List<OrderProcessingSaga> findByStatus(SagaStatus status);

    /**
     * Find sagas that are stuck (older than specified time and still processing)
     */
    @Query("SELECT s FROM OrderProcessingSaga s WHERE s.status IN (:statuses) AND s.updatedAt < :cutoffTime")
    List<OrderProcessingSaga> findStuckSagas(
        @Param("statuses") List<SagaStatus> statuses,
        @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Find sagas that can be retried
     */
    @Query("SELECT s FROM OrderProcessingSaga s WHERE s.status = 'FAILED' AND s.retryCount < 3")
    List<OrderProcessingSaga> findRetriableSagas();

    /**
     * Update saga status
     */
    @Modifying
    @Query("UPDATE OrderProcessingSaga s SET s.status = :status, s.updatedAt = :updatedAt WHERE s.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") SagaStatus status, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Check if saga exists for order
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Count sagas by status
     */
    long countByStatus(SagaStatus status);

    /**
     * Count sagas by multiple statuses
     */
    long countByStatusIn(List<SagaStatus> statuses);

    /**
     * Count stuck sagas
     */
    @Query("SELECT COUNT(s) FROM OrderProcessingSaga s WHERE s.status IN (:statuses) AND s.updatedAt < :cutoffTime")
    long countStuckSagas(
        @Param("statuses") List<SagaStatus> statuses,
        @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Find sagas created within time range
     */
    List<OrderProcessingSaga> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
