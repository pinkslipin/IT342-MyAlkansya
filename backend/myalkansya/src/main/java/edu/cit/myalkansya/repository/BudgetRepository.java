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
}