package edu.cit.myalkansya.service;

import edu.cit.myalkansya.entity.*;
import edu.cit.myalkansya.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardExportService {

    @Autowired private IncomeRepository incomeRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // Main method unchanged
    public byte[] exportDashboardDataAsBytes(int userId, int month, int year) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            // Get user data
            UserEntity user = userRepository.findById(userId).orElseThrow(() -> 
                new RuntimeException("User not found with ID: " + userId));

            // Get income data
            List<IncomeEntity> incomes = incomeRepository.findByUserUserId(userId).stream()
                    .filter(i -> i.getDate().getMonthValue() == month && i.getDate().getYear() == year)
                    .collect(Collectors.toList());

            // Get expense data
            List<ExpenseEntity> expenses = expenseRepository.findByUserUserId(userId).stream()
                    .filter(e -> e.getDate().getMonthValue() == month && e.getDate().getYear() == year)
                    .collect(Collectors.toList());

            // Get budget data
            List<BudgetEntity> budgets = budgetRepository.findByUserUserId(userId).stream()
                    .filter(b -> b.getBudgetMonth() == month && b.getBudgetYear() == year)
                    .collect(Collectors.toList());

            // Calculate totals
            double totalIncome = incomes.stream().mapToDouble(IncomeEntity::getAmount).sum();
            double totalExpenses = expenses.stream().mapToDouble(ExpenseEntity::getAmount).sum();
            double totalBudget = budgets.stream().mapToDouble(BudgetEntity::getMonthlyBudget).sum();
            double totalSavings = user.getTotalSavings();

            // Create workbook sheets
            createSummarySheet(workbook, totalIncome, totalExpenses, totalBudget, totalSavings);
            createIncomeSheet(workbook, incomes);
            createExpenseSheet(workbook, expenses);
            createBudgetSheet(workbook, budgets);

            // Write workbook to byte array
            workbook.write(out);
            out.flush();
            return out.toByteArray();
        } finally {
            try {
                workbook.close();
            } catch (Exception e) {
                System.err.println("Error closing workbook: " + e.getMessage());
            }
            try {
                out.close();
            } catch (Exception e) {
                System.err.println("Error closing output stream: " + e.getMessage());
            }
        }
    }
    
    // Summary sheet unchanged
    private void createSummarySheet(Workbook workbook, double totalIncome, double totalExpenses, 
                                  double totalBudget, double totalSavings) {
        Sheet sheet = workbook.createSheet("Summary");
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);
        
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Amount (PHP)");
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Total Income");
        row1.createCell(1).setCellValue(totalIncome);
        
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Total Expenses");
        row2.createCell(1).setCellValue(totalExpenses);
        
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("Total Budget");
        row3.createCell(1).setCellValue(totalBudget);
        
        Row row4 = sheet.createRow(4);
        row4.createCell(0).setCellValue("Total Savings");
        row4.createCell(1).setCellValue(totalSavings);
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        for (int i = 0; i < 2; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }
    }
    
    // Use a safer approach for IncomeEntity
    private void createIncomeSheet(Workbook workbook, List<IncomeEntity> incomes) {
        Sheet sheet = workbook.createSheet("Income");
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 4000);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Date");
        headerRow.createCell(1).setCellValue("Description");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Amount (PHP)");
        
        // Create data rows
        int rowNum = 1;
        for (IncomeEntity income : incomes) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(income.getDate().format(DATE_FORMATTER));
            
            // For description, try to get it safely
            String description = safeGetStringValue(income, "description", "");
            row.createCell(1).setCellValue(description);
            
            // For category, try to get it safely
            String category = safeGetStringValue(income, "category", 
                             safeGetStringValue(income, "source", ""));
            row.createCell(2).setCellValue(category);
            
            row.createCell(3).setCellValue(income.getAmount());
        }
        
        // Add styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        for (int i = 0; i < 4; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }
    }
    
    // Use a safer approach for ExpenseEntity
    private void createExpenseSheet(Workbook workbook, List<ExpenseEntity> expenses) {
        Sheet sheet = workbook.createSheet("Expenses");
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 4000);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Date");
        headerRow.createCell(1).setCellValue("Description");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Amount (PHP)");
        
        // Create data rows
        int rowNum = 1;
        for (ExpenseEntity expense : expenses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(expense.getDate().format(DATE_FORMATTER));
            
            // For description, try to get it safely
            String description = safeGetStringValue(expense, "description", "");
            row.createCell(1).setCellValue(description);
            
            // For category, try to get it safely
            String category = safeGetStringValue(expense, "category", "");
            row.createCell(2).setCellValue(category);
            
            row.createCell(3).setCellValue(expense.getAmount());
        }
        
        // Add styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        for (int i = 0; i < 4; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }
    }
    
    // Use a safer approach for BudgetEntity
    private void createBudgetSheet(Workbook workbook, List<BudgetEntity> budgets) {
        Sheet sheet = workbook.createSheet("Budget");
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Monthly Budget (PHP)");
        headerRow.createCell(2).setCellValue("Notes");
        
        // Create data rows
        int rowNum = 1;
        for (BudgetEntity budget : budgets) {
            Row row = sheet.createRow(rowNum++);
            
            // For category, try to get it safely
            String category = safeGetStringValue(budget, "category", "");
            row.createCell(0).setCellValue(category);
            
            row.createCell(1).setCellValue(budget.getMonthlyBudget());
            
            // For notes, try to get it safely
            String notes = safeGetStringValue(budget, "notes", 
                          safeGetStringValue(budget, "description", 
                          safeGetStringValue(budget, "comment", "")));
            row.createCell(2).setCellValue(notes);
        }
        
        // Add styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        for (int i = 0; i < 3; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }
    }
    
    // Helper method to safely get a property value using reflection
    private String safeGetStringValue(Object obj, String propertyName, String defaultValue) {
        try {
            // Try the getter method first (e.g., "getCategory")
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method method = obj.getClass().getMethod(getterName);
            Object result = method.invoke(obj);
            return result != null ? result.toString() : defaultValue;
        } catch (Exception e) {
            // If getter fails, try direct field access (won't work with private fields)
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(propertyName);
                field.setAccessible(true);
                Object result = field.get(obj);
                return result != null ? result.toString() : defaultValue;
            } catch (Exception ex) {
                // If both approaches fail, return the default value
                return defaultValue;
            }
        }
    }
    
    // For backward compatibility
    public InputStream exportDashboardData(int userId, int month, int year) throws Exception {
        byte[] data = exportDashboardDataAsBytes(userId, month, year);
        return new ByteArrayInputStream(data);
    }
}
