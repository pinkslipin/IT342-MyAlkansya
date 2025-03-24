package edu.cit.myalkansya.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cit.myalkansya.entity.SavingsGoalEntity;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoalEntity, Integer> {
    
} 
