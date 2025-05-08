// src/utils/excelExport.js
import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';

/**
 * Export multiple sheets to an Excel file with branding & styling.
 *
 * @param {Array<{
 *   name: string,
 *   columns: { header: string, key: string }[],
 *   data: Record<string, any>[]
 * }>} sheets
 * @param {string} fileName
 * @param {object} options
 * @param {{firstname:string,lastname:string,email:string}} options.user
 * @param {number} options.selectedMonth
 * @param {number} options.selectedYear
 * @param {{value:number,label:string}[]} options.months
 */
export async function exportSheets(sheets, fileName, options = {}) {
  const { user, selectedMonth, selectedYear, months } = options;
  const wb = new ExcelJS.Workbook();
  wb.creator = 'MyAlkansya';
  wb.created = new Date();

  let finalLastRowForBudget; // Variable to store the actual last row for Budget sheet

  for (const sheet of sheets) {
    const { name, columns, data } = sheet;
    const ws = wb.addWorksheet(name);

    if (name === 'Dashboard') {
      // — USER INFO —
      ws.getCell('A1').value = 'Name:';
      ws.getCell('B1').value = `${user?.firstname || ''} ${user?.lastname || ''}`.trim();
      ws.getCell('A2').value = 'Email:';
      ws.getCell('B2').value = user?.email || '';
      ws.getCell('A3').value = 'Export Date:';
      ws.getCell('B3').value = new Date().toLocaleString();
      ws.getCell('A4').value = 'Period:';
      ws.getCell('B4').value =
        (selectedMonth === 0
          ? 'All Months'
          : months.find(m => m.value === selectedMonth)?.label) +
        ' / ' +
        (selectedYear === 0 ? 'All Years' : selectedYear);

      ['A1','A2','A3','A4'].forEach(ref => {
        const c = ws.getCell(ref);
        c.font = { bold: true, color: { argb: 'FFFFFFFF' } };
        c.fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FF18864F' } };
      });

      // — HEADER AT ROW 6 —
      const headerRow = 6;
      columns.forEach((col, idx) => {
        const cell = ws.getCell(headerRow, idx + 1);
        cell.value = col.header;
        cell.font = { bold: true };
        cell.alignment = { vertical:'middle', horizontal:'center' };
        cell.fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFFFC107' } };
      });
      ws.views = [{ state:'frozen', ySplit:headerRow }];

      // — DATA ROWS FROM ROW 7 —
      data.forEach((row, i) => {
        const r = headerRow + i + 1;
        columns.forEach((col, j) => {
          const cell = ws.getCell(r, j + 1);
          if (col.key === 'Value') {
            cell.value = row[col.key];
            cell.numFmt = '#,##0.00'; // Default currency format for Dashboard values
            cell.alignment = { horizontal:'right' };
          } else {
            cell.value = row[col.key];
          }
        });
        if (i % 2 === 0) {
          ws.getRow(r).fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFF2F2F2' } };
        }
      });

      // — BORDERS & AUTOSIZE —
      const lastColD = columns.length;
      const lastRowD = headerRow + data.length + 1; // +1 for the row after data
      for (let r = 1; r <= lastRowD; r++) {
        for (let c = 1; c <= lastColD; c++) {
          const cell = ws.getCell(r, c);
          cell.border = {
            top:{style:'thin'}, left:{style:'thin'},
            bottom:{style:'thin'}, right:{style:'thin'}
          };
          if (r > 4 && r < headerRow) { // Empty rows between user info and header
             // No specific font override, keep default or remove if not needed
          } else {
            cell.font = { name:'Calibri', size:11 };
          }
        }
      }
      ws.columns.forEach(col => {
        let max = 10;
        col.eachCell({ includeEmpty: true }, cell => { // includeEmpty might be useful
          const len = cell.value ? cell.value.toString().length : 0;
          if (len > max) max = len;
        });
        col.width = max + 2;
      });

    } else {
      // — STANDARD SHEETS (Income, Expenses, SavingsGoal, etc., excluding Budget special handling) —
      // Header row
      columns.forEach((col, idx) => {
        const cell = ws.getCell(1, idx + 1);
        cell.value = col.header;
        cell.font = { bold:true, color:{ argb:'FFFFFFFF' } };
        cell.fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFFFC107' } };
        cell.alignment = { vertical:'middle', horizontal:'center' };
      });
      ws.views = [{ state:'frozen', ySplit:1 }];

      // Data rows + zebra (specific handling for Budget sheet is separate)
      if (name !== 'Budget') {
        data.forEach((row, i) => {
          const r = i + 2;
          columns.forEach((col, j) => {
            const cell = ws.getCell(r, j + 1);
            if (col.key === 'date' || col.key === 'targetDate') {
              cell.value = row[col.key] ? new Date(row[col.key]) : null;
              cell.numFmt = 'mm/dd/yyyy';
            } else {
              cell.value = row[col.key]; // This handles 'subject' for Description in Expenses
            }
          });
          if (i % 2 === 0) {
            ws.getRow(r).fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFF2F2F2' } };
          }
        });
      }

      // — SHEET‑SPECIFIC FORMATTING —

      // Income/Expenses: two‑decimal on Amount, including currency symbol
      if (name === 'Income' || name === 'Expenses') {
        const amtColIdx = columns.findIndex(c => c.key === 'amount');
        if (amtColIdx !== -1) {
          data.forEach((row, i) => {
            const cell = ws.getCell(i + 2, amtColIdx + 1);
            if (typeof cell.value === 'number') {
              const currencySymbol = row.currency || ''; // Use currency from data row
              cell.numFmt = currencySymbol ? `"${currencySymbol}"#,##0.00;[Red]-"${currencySymbol}"#,##0.00` : '#,##0.00;[Red]-#,##0.00';
              cell.alignment = { horizontal:'right' };
            }
          });
        }
      }

      // Budget: two‑decimal + colored Remaining + Expense Breakdown
      if (name === 'Budget') {
        let budgetExcelRowIndex = 2; // Start data from row 2
        const mainBudgetRowIndexes = [];

        data.forEach((budgetItem) => {
          mainBudgetRowIndexes.push(budgetExcelRowIndex);
          const mainBudgetCurrentRow = budgetExcelRowIndex;

          // Write main budget item
          columns.forEach((col, j) => {
            const cell = ws.getCell(mainBudgetCurrentRow, j + 1);
            cell.value = budgetItem[col.key];
            if (['monthlyBudget', 'totalSpent', 'remaining'].includes(col.key)) {
              const currencySymbol = budgetItem.currency || '';
              cell.numFmt = currencySymbol ? `"${currencySymbol}"#,##0.00;[Red]-"${currencySymbol}"#,##0.00` : '#,##0.00;[Red]-#,##0.00';
              cell.alignment = { horizontal: 'right' };
            }
            if (col.key === 'currency') {
              cell.alignment = { horizontal: 'center' };
            }
          });
          const remainingCell = ws.getCell(mainBudgetCurrentRow, columns.findIndex(c => c.key === 'remaining') + 1);
          remainingCell.font = {
            name: 'Calibri', size: 11, bold: true,
            color: { argb: budgetItem.remaining < 0 ? 'FFDC3545' : 'FF18864F' }
          };
          budgetExcelRowIndex++;

          // Expense Breakdown
          if (budgetItem.expenses && budgetItem.expenses.length > 0) {
            const breakdownHeaderCell = ws.getCell(budgetExcelRowIndex, 2); // Indent to Column B
            breakdownHeaderCell.value = 'Expense Breakdown:';
            breakdownHeaderCell.font = { bold: true, italic: true, color: { argb: 'FF495057' } }; // Dark grey text
            ws.mergeCells(budgetExcelRowIndex, 2, budgetExcelRowIndex, 4); // Merge B, C, D for the breakdown title
            budgetExcelRowIndex++;

            const expenseSubHeaders = ['Date', 'Description', 'Amount'];
            expenseSubHeaders.forEach((header, k) => {
              const cell = ws.getCell(budgetExcelRowIndex, k + 2); // Start in Col B
              cell.value = header;
              cell.font = { bold: true, color: { argb: 'FF6c757d' } }; // Medium grey text
              cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE9ECEF' } }; // Light grey fill
              cell.alignment = { indent: (k === 0 ? 1 : 0), horizontal: (k === 2 ? 'right' : 'left') };
            });
            budgetExcelRowIndex++;

            budgetItem.expenses.forEach(expense => {
              const dateCell = ws.getCell(budgetExcelRowIndex, 2); // Col B
              dateCell.value = expense.date ? new Date(expense.date) : null;
              dateCell.numFmt = 'mm/dd/yyyy';
              dateCell.alignment = { indent: 1 };

              const descCell = ws.getCell(budgetExcelRowIndex, 3); // Col C
              descCell.value = expense.subject;
              descCell.alignment = { indent: 1 };

              const amountCell = ws.getCell(budgetExcelRowIndex, 4); // Col D
              amountCell.value = expense.amount;
              const expenseCurrencySymbol = expense.currency || '';
              amountCell.numFmt = expenseCurrencySymbol ? `"${expenseCurrencySymbol}"#,##0.00;[Red]-"${expenseCurrencySymbol}"#,##0.00` : '#,##0.00;[Red]-#,##0.00';
              amountCell.alignment = { horizontal: 'right' };

              // Light fill for expense breakdown rows
              [2, 3, 4].forEach(colIdx => {
                 ws.getCell(budgetExcelRowIndex, colIdx).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF8F9FA' } };
              });
              budgetExcelRowIndex++;
            });
          }
        });
        
        mainBudgetRowIndexes.forEach((rowIndex, index) => {
          if (index % 2 === 0) { // Zebra stripe main budget rows
             for(let c = 1; c <= columns.length; c++) {
                ws.getCell(rowIndex, c).fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFF2F2F2' } };
             }
          }
        });
        finalLastRowForBudget = budgetExcelRowIndex -1; // Update the last row for border/autosize
      }

      // SavingsGoal: amounts, date, and percent
      if (name === 'SavingsGoal') {
        const taIdx = columns.findIndex(c=>c.key==='targetAmount');
        const caIdx = columns.findIndex(c=>c.key==='currentAmount');
        const tdIdx = columns.findIndex(c=>c.key==='targetDate');
        const pgIdx = columns.findIndex(c=>c.key==='progress');

        data.forEach((row,i) => {
          const r = i+2;
          [taIdx, caIdx].forEach(colKeyIdx => {
            if (colKeyIdx !== -1) {
              const cell = ws.getCell(r, colKeyIdx + 1);
              if (typeof cell.value === 'number') {
                const currencySymbol = row.currency || ''; // Assuming currency might be part of savings goal data
                cell.numFmt = currencySymbol ? `"${currencySymbol}"#,##0.00` : '#,##0.00';
                cell.alignment = { horizontal:'right' };
              }
            }
          });
          if (tdIdx !== -1) ws.getCell(r, tdIdx + 1).numFmt = 'mm/dd/yyyy';
          if (pgIdx !== -1) {
            const pc = ws.getCell(r, pgIdx + 1);
            pc.value = row.progress/100; // Assuming progress is 0-100
            pc.numFmt = '0%';
            const progressValue = row.progress || 0;
            const color = progressValue>=90?'FF18864F'
                         :progressValue>=50?'FFFFC107'
                         :'FFDC3545';
            pc.font = { name:'Calibri', size:11, bold:true, color:{argb:color} };
            pc.alignment = { horizontal:'center' };
          }
        });
      }

      // — BORDERS & AUTOSIZE (for standard sheets and updated for Budget) —
      const lastRow = (name === 'Budget' && finalLastRowForBudget) ? finalLastRowForBudget : (data.length + 1);
      const lastCol = columns.length;
      for (let r=1; r<=lastRow; r++){
        for (let c=1; c<=lastCol; c++){
          const cell = ws.getCell(r,c);
          // Apply borders to all relevant cells, including breakdown headers in Budget
          if (name === 'Budget') {
            // For budget, apply borders to main budget items and breakdown items
            // Check if the current cell (r,c) is part of the main budget table or breakdown
            const isMainBudgetHeader = r === 1 && c <= columns.length;
            const isMainBudgetItem = data.some((_, budgetIdx) => {
                const budgetStartRow = mainBudgetRowIndexes[budgetIdx];
                return r === budgetStartRow && c <= columns.length;
            });
            const isBreakdownHeaderOrItem = r > 1 && c >=2 && c <= 4 && r < finalLastRowForBudget +1 ; // Crude check, refine if needed

            if(isMainBudgetHeader || isMainBudgetItem || isBreakdownHeaderOrItem) {
                 cell.border = {
                    top:{style:'thin'}, left:{style:'thin'},
                    bottom:{style:'thin'}, right:{style:'thin'}
                 };
            }
          } else { // For other sheets
             cell.border = {
                top:{style:'thin'}, left:{style:'thin'},
                bottom:{style:'thin'}, right:{style:'thin'}
             };
          }
          // Apply default font if not already set by specific formatting
          if (!cell.font) {
            cell.font = { name:'Calibri', size:11 };
          }
        }
      }
      ws.columns.forEach((col, cIdx) => { // cIdx is 0-based
        let max = 10; // Default min width
        // For Budget sheet, column B, C, D might need special width handling due to breakdown
        if (name === 'Budget' && cIdx >=1 && cIdx <=3) { // Columns B, C, D (0-indexed 1,2,3)
            max = 15; // A bit wider for breakdown
        }
        col.eachCell({ includeEmpty: true }, cell => {
          const len = cell.value ? cell.value.toString().length : 0;
          if (len > max) max = len;
        });
        col.width = max + 2;
      });

      // — FIX DATE COLUMNS WIDTH TO 12 (for non-Budget sheets) —
      if (name !== 'Budget') {
        columns.forEach((col, idx) => {
          if (col.key === 'date' || col.key === 'targetDate') {
            ws.getColumn(idx+1).width = 12;
          }
        });
      }
    }
  }

  // Helper function to convert column index to Excel column letter
  function getExcelColumnLetter(columnNumber) {
    let columnLetter = '';
    while (columnNumber > 0) {
      const modulo = (columnNumber - 1) % 26;
      columnLetter = String.fromCharCode(65 + modulo) + columnLetter;
      columnNumber = Math.floor((columnNumber - modulo) / 26);
    }
    return columnLetter;
  }
  
  // — Add conditional formatting —
  for (const sheet of sheets) {
    const { name, columns, data } = sheet;
    const ws = wb.getWorksheet(name);
    
    if (name === 'SavingsGoal') {
      const pgIdx = columns.findIndex(c => c.key === 'progress') + 1;
      if (pgIdx > 0 && data.length > 0) {
        const startRow = 2;
        const endRow = data.length + 1;
        const colLetter = getExcelColumnLetter(pgIdx);
        const range = `${colLetter}${startRow}:${colLetter}${endRow}`;
        
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs', operator: 'lessThan', priority: 1, formulae: [0.5],
            style: { font: { color: { argb: 'FFDC3545' } } } // Red
          }]
        });
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs', operator: 'between', priority: 2, formulae: [0.5, 0.9],
            style: { font: { color: { argb: 'FFFFC107' } } } // Yellow
          }]
        });
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs', operator: 'greaterThanOrEqual', priority: 3, formulae: [0.9],
            style: { font: { color: { argb: 'FF18864F' } } } // Green
          }]
        });
      }
    }
    
    if (name === 'Budget') {
      const rmIdx = columns.findIndex(c => c.key === 'remaining') + 1;
      // Conditional formatting applies to the main budget items, not the breakdown
      const mainBudgetItemCount = data.filter(item => item.category).length; // Count actual budget items
      if (rmIdx > 0 && mainBudgetItemCount > 0) {
        // This needs to apply to potentially non-contiguous rows if breakdown is sparse
        // For simplicity, applying to a block, assuming main budget items are somewhat grouped
        // A more precise way would be to iterate mainBudgetRowIndexes and apply CF to each
        const startRow = 2; // First main budget item
        const endRow = 1 + mainBudgetItemCount + data.reduce((acc, curr) => acc + (curr.expenses ? curr.expenses.length + 2 : 0), 0); // Estimate last row
        const colLetter = getExcelColumnLetter(rmIdx);
        const range = `${colLetter}${startRow}:${colLetter}${endRow}`; // This range might be too broad
                                                                    // and cover breakdown items if not careful.
                                                                    // The font style on remainingCell already handles color.
                                                                    // Conditional formatting here might be redundant or conflict.
                                                                    // For now, let's keep it simple, but this is an area for refinement.

        // The direct styling on the 'remaining' cell's font color is more precise for main budget items.
        // ws.getCell(mainBudgetCurrentRow, rmIdx).font = { color: ... }
        // So, perhaps this conditional formatting block for Budget 'remaining' is not strictly needed
        // if direct cell styling is preferred and already implemented.
        // However, if CF is desired:
        // Red for negative remaining
        // ws.addConditionalFormatting({
        //   ref: range, // This range needs to be specific to main budget 'remaining' cells
        //   rules: [{
        //     type: 'cellIs', operator: 'lessThan', priority: 1, formulae: [0],
        //     style: { font: { bold: true, color: { argb: 'FFDC3545' } } }
        //   }]
        // });
        // Green for positive remaining
        // ws.addConditionalFormatting({
        //   ref: range, // Same range concern
        //   rules: [{
        //     type: 'cellIs', operator: 'greaterThanOrEqual', priority: 2, formulae: [0],
        //     style: { font: { bold: true, color: { argb: 'FF18864F' } } }
        //   }]
        // });
      }
    }
  }

  // — finalize & download —
  const buffer = await wb.xlsx.writeBuffer();
  saveAs(new Blob([buffer]), fileName);
}