package edu.cit.myalkansya.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.cit.myalkansya.entity.BudgetEntity;

@Repository
public interface BudgetRepository extends JpaRepository<BudgetEntity, Integer> {
}