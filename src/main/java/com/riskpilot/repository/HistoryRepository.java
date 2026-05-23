package com.riskpilot.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riskpilot.model.HistoryReport;
import com.riskpilot.model.UserCred;

/**
 * Repository for HistoryReport entity
 * Provides database operations and custom queries for scan history management
 */
@Repository
public interface HistoryRepository extends JpaRepository<HistoryReport, Long> {

    /**
     * Find all scan history records for a specific user
     */
    List<HistoryReport> findByUser(UserCred user);

    /**
     * Find all scan history records for a specific user ordered by creation date (newest first)
     */
    List<HistoryReport> findByUserOrderByCreatedAtDesc(UserCred user);

    /**
     * Find all scans of a specific type for a user
     */
    List<HistoryReport> findByUserAndScanType(UserCred user, String scanType);

    /**
     * Find all scans by risk level for a user
     */
    List<HistoryReport> findByUserAndRiskLevel(UserCred user, String riskLevel);

    /**
     * Find scans within a date range for a user
     */
    @Query("SELECT h FROM HistoryReport h WHERE h.user = :user AND h.createdAt BETWEEN :startDate AND :endDate ORDER BY h.createdAt DESC")
    List<HistoryReport> findByUserAndDateRange(@Param("user") UserCred user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find all HIGH risk scans for a user
     */
    @Query("SELECT h FROM HistoryReport h WHERE h.user = :user AND h.riskLevel = 'HIGH' ORDER BY h.createdAt DESC")
    List<HistoryReport> findHighRiskScans(@Param("user") UserCred user);

    /**
     * Find scans by email address
     */
    List<HistoryReport> findByEmail(String email);

    /**
     * Find scans by URL
     */
    List<HistoryReport> findByUrl(String url);

    /**
     * Find scans by company name
     */
    List<HistoryReport> findByCompanyName(String companyName);

    /**
     * Delete all scans for a specific user
     */
    void deleteByUser(UserCred user);

    /**
     * Count total scans for a user
     */
    long countByUser(UserCred user);

    /**
     * Count scans by type for a user
     */
    long countByUserAndScanType(UserCred user, String scanType);
}
