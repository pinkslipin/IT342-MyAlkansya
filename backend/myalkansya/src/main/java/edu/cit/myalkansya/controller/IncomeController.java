package edu.cit.myalkansya.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.service.IncomeService;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    @Autowired
    private IncomeService incomeService;

    // CREATE
    @PostMapping("/postIncome")
    public IncomeEntity postIncome(@RequestBody IncomeEntity income) {
        return incomeService.createIncome(income);
    }

    // READ
    @GetMapping("/getIncomes")
    public List<IncomeEntity> getIncomes() {
        return incomeService.getAllIncomes();
    }

    @GetMapping("/getIncome/{incomeId}")
    public IncomeEntity getIncomeById(@PathVariable int incomeId) {
        return incomeService.getIncomeById(incomeId);
    }

    // UPDATE
    @PutMapping("/putIncome/{incomeId}")
    public IncomeEntity putIncome(@PathVariable int incomeId, @RequestBody IncomeEntity newIncomeDetails) {
        return incomeService.updateIncome(incomeId, newIncomeDetails);
    }

    // DELETE
    @DeleteMapping("/deleteIncome/{incomeId}")
    public String deleteIncome(@PathVariable int incomeId) {
        return incomeService.deleteIncome(incomeId);
    }
}