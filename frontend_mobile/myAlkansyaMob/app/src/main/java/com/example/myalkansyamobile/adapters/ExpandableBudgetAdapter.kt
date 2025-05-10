package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.model.Expense
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

class ExpandableBudgetAdapter(
    private var budgetList: List<Budget>,
    private val onBudgetClick: (Budget) -> Unit,
    private var defaultCurrency: String = "PHP",
    private val onExpenseClick: (Expense) -> Unit = {}
) : RecyclerView.Adapter<ExpandableBudgetAdapter.BudgetViewHolder>() {

    // Map to store expenses for each budget category
    private val categoryExpenses = mutableMapOf<String, List<Expense>>()
    // Map to keep track of expanded state for each position
    private val expandedPositions = mutableSetOf<Int>()
    
    // Date formatter for expenses
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    class BudgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textCategory: TextView = view.findViewById(R.id.tvCategory)
        val textBudget: TextView = view.findViewById(R.id.tvBudgetAmount)
        val textSpent: TextView = view.findViewById(R.id.tvSpentAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val percentageText: TextView = view.findViewById(R.id.tvPercentage)
        val arrowIcon: ImageView = view.findViewById(R.id.ivExpandArrow)
        val expandableLayout: LinearLayout = view.findViewById(R.id.expandableLayout)
        val recyclerViewExpenses: RecyclerView = view.findViewById(R.id.recyclerViewExpenses)
        val tvNoExpenses: TextView = view.findViewById(R.id.tvNoExpenses)
        val remainingText: TextView = view.findViewById(R.id.tvRemaining)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_expandable, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgetList[position]
        
        holder.textCategory.text = budget.category
        
        // Format amounts with appropriate currency symbols
        val formatter = getCurrencyFormatter(budget.currency)
        holder.textBudget.text = formatter.format(budget.monthlyBudget)
        holder.textSpent.text = formatter.format(budget.totalSpent)
        
        // Calculate and display remaining budget
        val remaining = budget.getRemainingBudget()
        holder.remainingText.text = "Remaining: ${formatter.format(remaining)}"
        
        if (remaining < 0) {
            holder.remainingText.setTextColor(holder.itemView.context.getColor(R.color.red))
        } else {
            holder.remainingText.setTextColor(holder.itemView.context.getColor(R.color.green_primary))
        }
        
        // Set progress bar value
        val progressPercentage = budget.getSpendingPercentage()
        holder.progressBar.progress = progressPercentage
        
        // Set percentage text
        holder.percentageText.text = "$progressPercentage%"
        
        // Set text color based on spending behavior
        val context = holder.itemView.context
        if (budget.totalSpent > budget.monthlyBudget) {
            holder.textSpent.setTextColor(context.getColor(R.color.red))
            holder.percentageText.setTextColor(context.getColor(R.color.red))
            holder.progressBar.progressDrawable = context.getDrawable(R.drawable.progress_bar_red)
        } else if (progressPercentage > 80) {
            holder.textSpent.setTextColor(context.getColor(R.color.yellow_primary))
            holder.percentageText.setTextColor(context.getColor(R.color.yellow_primary))
            holder.progressBar.progressDrawable = context.getDrawable(R.drawable.progress_bar_yellow)
        } else {
            holder.textSpent.setTextColor(context.getColor(R.color.green_primary))
            holder.percentageText.setTextColor(context.getColor(R.color.green_primary))
            holder.progressBar.progressDrawable = context.getDrawable(R.drawable.progress_bar_green)
        }
        
        // Set up expandable functionality
        val isExpanded = expandedPositions.contains(position)
        holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.arrowIcon.setImageResource(
            if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        )
        
        // Set click listener for expansion
        holder.itemView.setOnClickListener {
            toggleExpansion(holder, position)
        }
        
        // Set click listener for edit button (separate from expansion)
        holder.textCategory.setOnLongClickListener {
            onBudgetClick(budget)
            true
        }
        
        // Set up expenses list if expanded
        if (isExpanded) {
            setupExpensesList(holder, budget.category)
        }
    }
    
    private fun toggleExpansion(holder: BudgetViewHolder, position: Int) {
        val isExpanded = expandedPositions.contains(position)
        
        if (isExpanded) {
            expandedPositions.remove(position)
            holder.expandableLayout.visibility = View.GONE
            holder.arrowIcon.setImageResource(R.drawable.ic_arrow_down)
        } else {
            expandedPositions.add(position)
            holder.expandableLayout.visibility = View.VISIBLE
            holder.arrowIcon.setImageResource(R.drawable.ic_arrow_up)
            setupExpensesList(holder, budgetList[position].category)
        }
    }
    
    private fun setupExpensesList(holder: BudgetViewHolder, category: String) {
        val expenses = categoryExpenses[category] ?: emptyList()
        
        if (expenses.isEmpty()) {
            holder.tvNoExpenses.visibility = View.VISIBLE
            holder.recyclerViewExpenses.visibility = View.GONE
        } else {
            holder.tvNoExpenses.visibility = View.GONE
            holder.recyclerViewExpenses.visibility = View.VISIBLE
            
            // Create compact expense adapter
            val adapter = CompactExpenseAdapter(expenses, onExpenseClick)
            holder.recyclerViewExpenses.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.recyclerViewExpenses.adapter = adapter
        }
    }

    override fun getItemCount() = budgetList.size
    
    fun updateData(newList: List<Budget>) {
        budgetList = newList
        notifyDataSetChanged()
    }
    
    fun updateExpensesForCategory(category: String, expenses: List<Expense>) {
        categoryExpenses[category] = expenses
        notifyDataSetChanged()
    }
    
    fun updateAllExpenses(allExpenses: List<Expense>) {
        // Group expenses by category
        val groupedExpenses = allExpenses.groupBy { it.category }
        
        // Update the category expenses map
        categoryExpenses.clear()
        for (category in budgetList.map { it.category }) {
            categoryExpenses[category] = groupedExpenses[category] ?: emptyList()
        }
        
        notifyDataSetChanged()
    }
    
    fun updateDefaultCurrency(newCurrency: String) {
        defaultCurrency = newCurrency
        notifyDataSetChanged()
    }
    
    private fun getCurrencyFormatter(currencyCode: String): NumberFormat {
        val formatter = NumberFormat.getCurrencyInstance()
        try {
            formatter.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            formatter.currency = Currency.getInstance(defaultCurrency)
        }
        return formatter
    }
    
    // Compact adapter for expenses in the expanded view
    inner class CompactExpenseAdapter(
        private val expenses: List<Expense>,
        private val onItemClick: (Expense) -> Unit
    ) : RecyclerView.Adapter<CompactExpenseAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvSubject: TextView = view.findViewById(R.id.tvSubject)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense_compact, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val expense = expenses[position]
            
            holder.tvDate.text = expense.date.format(dateFormatter)
            holder.tvSubject.text = expense.subject
            
            // Format amount with currency
            val formatter = getCurrencyFormatter(expense.currency)
            holder.tvAmount.text = formatter.format(expense.amount)
            
            // Set click listener
            holder.itemView.setOnClickListener {
                onItemClick(expense)
            }
        }
        
        override fun getItemCount() = expenses.size
    }
}
