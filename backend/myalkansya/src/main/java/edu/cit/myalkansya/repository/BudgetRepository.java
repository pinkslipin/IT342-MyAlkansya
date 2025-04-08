package edu.cit.myalkansya.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.entity.UserEntity;

@Repository
public interface BudgetRepository extends JpaRepository<BudgetEntity, Integer> {
    List<BudgetEntity> findByUser(UserEntity user);
    List<BudgetEntity> findByUserUserId(int userId);
    List<BudgetEntity> findByUserUserIdAndCategory(int userId, String category);
    Optional<BudgetEntity> findFirstByUserUserIdAndCategory(int userId, String category);
    
    // New methods for filtering by month/year
    List<BudgetEntity> findByUserUserIdAndBudgetMonthAndBudgetYear(int userId, int month, int year);
    List<BudgetEntity> findByUserUserIdAndBudgetMonth(int userId, int month); // Month only, across years
    
    // Find if a budget already exists for this user, category, month, and year
    Optional<BudgetEntity> findByUserUserIdAndCategoryAndBudgetMonthAndBudgetYear(
            int userId, String category, int budgetMonth, int budgetYear);
            
}