package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.repository.IncomeRepository;

@Service
public class IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    // CREATE
    public IncomeEntity createIncome(IncomeEntity income) {
        return incomeRepository.save(income);
    }

    // READ
    public List<IncomeEntity> getAllIncomes() {
        return incomeRepository.findAll();
    }

    public IncomeEntity getIncomeById(int incomeId) {
        return incomeRepository.findById(incomeId)
                .orElseThrow(() -> new NoSuchElementException("Income with ID " + incomeId + " not found."));
    }

    // UPDATE
    public IncomeEntity updateIncome(int incomeId, IncomeEntity newIncomeDetails) {
        IncomeEntity existingIncome = getIncomeById(incomeId);
        existingIncome.setSource(newIncomeDetails.getSource());
        existingIncome.setDate(newIncomeDetails.getDate());
        existingIncome.setAmount(newIncomeDetails.getAmount());
        existingIncome.setCurrency(newIncomeDetails.getCurrency());
        return incomeRepository.save(existingIncome);
    }

    // DELETE
    public String deleteIncome(int incomeId) {
        if (incomeRepository.existsById(incomeId)) {
            incomeRepository.deleteById(incomeId);
            return "Income with ID " + incomeId + " successfully deleted.";
        } else {
            return "Income with ID " + incomeId + " not found.";
        }
    }
}