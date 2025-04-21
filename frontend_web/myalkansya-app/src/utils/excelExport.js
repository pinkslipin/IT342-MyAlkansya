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
            cell.numFmt = '#,##0.00';
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
      const lastRowD = headerRow + data.length + 1;
      for (let r = 1; r <= lastRowD; r++) {
        for (let c = 1; c <= lastColD; c++) {
          const cell = ws.getCell(r, c);
          cell.border = {
            top:{style:'thin'}, left:{style:'thin'},
            bottom:{style:'thin'}, right:{style:'thin'}
          };
          cell.font = { name:'Calibri', size:11 };
        }
      }
      ws.columns.forEach(col => {
        let max = 10;
        col.eachCell(cell => {
          const len = cell.value ? cell.value.toString().length : 0;
          if (len > max) max = len;
        });
        col.width = max + 2;
      });

    } else {
      // — STANDARD SHEETS —
      // Header row
      columns.forEach((col, idx) => {
        const cell = ws.getCell(1, idx + 1);
        cell.value = col.header;
        cell.font = { bold:true, color:{ argb:'FFFFFFFF' } };
        cell.fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFFFC107' } };
        cell.alignment = { vertical:'middle', horizontal:'center' };
      });
      ws.views = [{ state:'frozen', ySplit:1 }];

      // Data rows + zebra
      data.forEach((row, i) => {
        const r = i + 2;
        columns.forEach((col, j) => {
          const cell = ws.getCell(r, j + 1);
          if (col.key === 'date' || col.key === 'targetDate') {
            cell.value = new Date(row[col.key]);
            cell.numFmt = 'mm/dd/yyyy';
          } else {
            cell.value = row[col.key];
          }
        });
        if (i % 2 === 0) {
          ws.getRow(r).fill = { type:'pattern', pattern:'solid', fgColor:{ argb:'FFF2F2F2' } };
        }
      });

      // — SHEET‑SPECIFIC FORMATTING —

      // Income/Expenses: two‑decimal on Amount
      if (name === 'Income' || name === 'Expenses') {
        const amtCol = columns.length;
        data.forEach((_, i) => {
          const cell = ws.getCell(i+2, amtCol);
          if (typeof cell.value === 'number') {
            cell.numFmt = '#,##0.00';
            cell.alignment = { horizontal:'right' };
          }
        });
      }

      // Budget: two‑decimal + colored Remaining
      if (name === 'Budget') {
        const mb = columns.findIndex(c=>c.key==='monthlyBudget')+1;
        const ts = columns.findIndex(c=>c.key==='totalSpent')   +1;
        const rm = columns.findIndex(c=>c.key==='remaining')    +1;
        data.forEach((row,i) => {
          const r = i+2;
          [mb,ts,rm].forEach(colNum => {
            const cell = ws.getCell(r,colNum);
            if (typeof cell.value==='number') {
              cell.numFmt = '#,##0.00';
              cell.alignment = { horizontal:'right' };
            }
          });
          ws.getCell(r, rm).font = {
            name:'Calibri', size:11, bold:true,
            color:{ argb: row.remaining<0 ? 'FFDC3545':'FF18864F' }
          };
        });
      }

      // SavingsGoal: amounts, date, and percent
      if (name === 'SavingsGoal') {
        const ta = columns.findIndex(c=>c.key==='targetAmount')  +1;
        const ca = columns.findIndex(c=>c.key==='currentAmount') +1;
        const td = columns.findIndex(c=>c.key==='targetDate')    +1;
        const pg = columns.findIndex(c=>c.key==='progress')      +1;
        data.forEach((row,i) => {
          const r = i+2;
          [ta,ca].forEach(colNum => {
            const cell = ws.getCell(r,colNum);
            if (typeof cell.value==='number') {
              cell.numFmt = '#,##0.00';
              cell.alignment = { horizontal:'right' };
            }
          });
          ws.getCell(r, td).numFmt = 'mm/dd/yyyy';
          const pc = ws.getCell(r, pg);
          pc.value = row.progress/100;
          pc.numFmt = '0%';
          const color = row.progress>=90?'FF18864F'
                       :row.progress>=50?'FFFFC107'
                       :'FFDC3545';
          pc.font = { name:'Calibri', size:11, bold:true, color:{argb:color} };
          pc.alignment = { horizontal:'center' };
        });
      }

      // — BORDERS & AUTOSIZE —
      const lastRow = data.length + 1;
      const lastCol = columns.length;
      for (let r=1; r<=lastRow; r++){
        for (let c=1; c<=lastCol; c++){
          const cell = ws.getCell(r,c);
          cell.border = {
            top:{style:'thin'}, left:{style:'thin'},
            bottom:{style:'thin'}, right:{style:'thin'}
          };
          cell.font = { name:'Calibri', size:11 };
        }
      }
      ws.columns.forEach(col => {
        let max = 10;
        col.eachCell(cell => {
          const len = cell.value ? cell.value.toString().length : 0;
          if (len > max) max = len;
        });
        col.width = max + 2;
      });

      // — FIX DATE COLUMNS WIDTH TO 12 —
      columns.forEach((col, idx) => {
        if (col.key === 'date' || col.key === 'targetDate') {
          ws.getColumn(idx+1).width = 12;
        }
      });
    }
  }

  // — Add conditional formatting with data bars —
  for (const sheet of sheets) {
    const { name, columns, data } = sheet;
    const ws = wb.getWorksheet(name);
    
    if (name === 'SavingsGoal') {
      // Add color formatting to the progress column (no data bars)
      const pgIdx = columns.findIndex(c => c.key === 'progress') + 1;
      if (pgIdx > 0) {
        const startRow = 2;
        const endRow = data.length + 1;
        const colLetter = getExcelColumnLetter(pgIdx);
        const range = `${colLetter}${startRow}:${colLetter}${endRow}`;
        
        // Red for low progress (<50%)
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs',
            operator: 'lessThan',
            priority: 1,
            formulae: [0.5],
            style: { font: { color: { argb: 'FFDC3545' } } } // Red
          }]
        });
        
        // Yellow for medium progress (50-90%)
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs',
            operator: 'between',
            priority: 2,
            formulae: [0.5, 0.9],
            style: { font: { color: { argb: 'FFFFC107' } } } // Yellow
          }]
        });
        
        // Green for high progress (>=90%)
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs',
            operator: 'greaterThanOrEqual',
            priority: 3,
            formulae: [0.9],
            style: { font: { color: { argb: 'FF18864F' } } } // Theme green
          }]
        });
      }
    }
    
    if (name === 'Budget') {
      // Only add color formatting to text (no data bars)
      const rmIdx = columns.findIndex(c => c.key === 'remaining') + 1;
      if (rmIdx > 0) {
        const startRow = 2;
        const endRow = data.length + 1;
        const colLetter = getExcelColumnLetter(rmIdx);
        const range = `${colLetter}${startRow}:${colLetter}${endRow}`;
        
        // Red for negative remaining
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs',
            operator: 'lessThan',
            priority: 1,
            formulae: [0],
            style: { font: { color: { argb: 'FFDC3545' } } } // Red
          }]
        });
        
        // Green for positive remaining
        ws.addConditionalFormatting({
          ref: range,
          rules: [{
            type: 'cellIs',
            operator: 'greaterThanOrEqual',
            priority: 2,
            formulae: [0],
            style: { font: { color: { argb: 'FF18864F' } } } // Theme green
          }]
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

  // — finalize & download —
  const buffer = await wb.xlsx.writeBuffer();
  saveAs(new Blob([buffer]), fileName);
}
