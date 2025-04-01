package edu.cit.myalkansya.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.UserEntity;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Integer> {
    List<ExpenseEntity> findByUser(UserEntity user);
    List<ExpenseEntity> findByUserUserId(int userId);
    List<ExpenseEntity> findByUserUserIdAndCategory(int userId, String category);
}