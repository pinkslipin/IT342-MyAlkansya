package edu.cit.myalkansya.repository;

import edu.cit.myalkansya.entity.IncomeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<IncomeEntity, Integer> {
    List<IncomeEntity> findByUser_UserId(int userId);
    List<IncomeEntity> findByUser_UserIdAndDateBetween(int userId, LocalDate startDate, LocalDate endDate);
    
    // Adding back the removed method
    List<IncomeEntity> findByUserUserId(int userId);

    @Query("SELECT i FROM IncomeEntity i WHERE i.user.userId = :userId AND FUNCTION('YEAR', i.date) = :year")
    List<IncomeEntity> findByUserIdAndYear(int userId, int year);
}