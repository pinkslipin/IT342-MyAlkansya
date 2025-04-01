package edu.cit.myalkansya.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.entity.UserEntity;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoalEntity, Integer> {
    List<SavingsGoalEntity> findByUser(UserEntity user);
    List<SavingsGoalEntity> findByUserUserId(int userId);
}