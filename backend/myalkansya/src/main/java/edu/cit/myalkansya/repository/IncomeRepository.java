package edu.cit.myalkansya.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.entity.UserEntity;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<IncomeEntity, Integer> {
    List<IncomeEntity> findByUser(UserEntity user);
    List<IncomeEntity> findByUserUserId(int userId);
}