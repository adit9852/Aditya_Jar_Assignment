package com.myjar.jarassignment.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myjar.jarassignment.createRetrofit
import com.myjar.jarassignment.data.model.ComputerItem
import com.myjar.jarassignment.data.repository.JarRepository
import com.myjar.jarassignment.data.repository.JarRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val items: List<ComputerItem> = emptyList(),
    val filteredItems: List<ComputerItem> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

class JarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Backward compatibility
    val listStringData: StateFlow<List<ComputerItem>>
        get() = MutableStateFlow(_uiState.value.filteredItems).asStateFlow()

    private val repository: JarRepository = JarRepositoryImpl(createRetrofit())

    fun fetchData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.fetchResults()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
                .collect { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = items,
                        filteredItems = items,
                        error = null
                    )
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterItems(query)
    }

    private fun filterItems(query: String) {
        val filtered = if (query.isBlank()) {
            _uiState.value.items
        } else {
            _uiState.value.items.filter { item ->
                item.name.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(filteredItems = filtered)
    }

    fun retry() {
        fetchData()
    }

    fun getItemById(itemId: String): ComputerItem? {
        return _uiState.value.items.find { it.id == itemId }
    }
}