package com.example.myalkansyamobile.api

import android.util.Log
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class IncomeRepository(private val apiService: IncomeApiService) {

    suspend fun getIncomes(token: String): Resource<List<Income>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getIncomes("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response from server")
            } else {
                Resource.Error("Failed to fetch incomes: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error fetching incomes", e)
            when (e) {
                is HttpException -> Resource.Error("Server error: ${e.message()}")
                is IOException -> Resource.Error("Network error: Check your connection")
                else -> Resource.Error("Unknown error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun getIncomeById(incomeId: Int, token: String): Resource<Income> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getIncomeById(incomeId, "Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response from server")
            } else {
                Resource.Error("Failed to fetch income: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error fetching income", e)
            when (e) {
                is HttpException -> Resource.Error("Server error: ${e.message()}")
                is IOException -> Resource.Error("Network error: Check your connection")
                else -> Resource.Error("Unknown error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun addIncome(incomeRequest: IncomeRequest, token: String): Resource<Income> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addIncome("Bearer $token", incomeRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response from server")
            } else {
                Resource.Error("Failed to add income: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error adding income", e)
            when (e) {
                is HttpException -> Resource.Error("Server error: ${e.message()}")
                is IOException -> Resource.Error("Network error: Check your connection")
                else -> Resource.Error("Unknown error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun updateIncome(incomeId: Int, income: Income, token: String): Resource<Income> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateIncome(incomeId, income, "Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response from server")
            } else {
                Resource.Error("Failed to update income: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error updating income", e)
            when (e) {
                is HttpException -> Resource.Error("Server error: ${e.message()}")
                is IOException -> Resource.Error("Network error: Check your connection")
                else -> Resource.Error("Unknown error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun deleteIncome(incomeId: Int, token: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteIncome(incomeId, "Bearer $token")
            if (response.isSuccessful) {
                Resource.Success("Income deleted successfully")
            } else {
                Resource.Error("Failed to delete income: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("IncomeRepository", "Error deleting income", e)
            when (e) {
                is HttpException -> Resource.Error("Server error: ${e.message()}")
                is IOException -> Resource.Error("Network error: Check your connection")
                else -> Resource.Error("Unknown error: ${e.localizedMessage}")
            }
        }
    }
}
