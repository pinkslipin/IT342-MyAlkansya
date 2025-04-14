package edu.cit.myalkansya.repository;

import edu.cit.myalkansya.entity.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Integer> {
    List<ExpenseEntity> findByUser_UserId(int userId);
    List<ExpenseEntity> findByUser_UserIdAndDateBetween(int userId, LocalDate startDate, LocalDate endDate);
    List<ExpenseEntity> findByBudgetId(int budgetId);
    List<ExpenseEntity> findByUserUserIdAndCategory(int userId, String category);
    
    // Adding back the removed method
    List<ExpenseEntity> findByUserUserId(int userId);

    @Query("SELECT e FROM ExpenseEntity e WHERE e.user.userId = :userId AND FUNCTION('YEAR', e.date) = :year")
    List<ExpenseEntity> findByUserIdAndYear(int userId, int year);

    @Query("SELECT e FROM ExpenseEntity e WHERE e.user.userId = :userId AND FUNCTION('MONTH', e.date) = :month AND FUNCTION('YEAR', e.date) = :year")
    List<ExpenseEntity> findByUserIdAndMonthAndYear(int userId, int month, int year);
}