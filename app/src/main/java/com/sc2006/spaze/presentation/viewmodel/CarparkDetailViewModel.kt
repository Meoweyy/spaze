package com.sc2006.spaze.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sc2006.spaze.data.repository.CarparkDetail
import com.sc2006.spaze.data.repository.CarparkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarparkDetailViewModel @Inject constructor(
    private val carparkRepository: CarparkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val carparkNumber: String = savedStateHandle.get<String>("carparkNumber") ?: ""

    private val _uiState = MutableStateFlow(CarparkDetailUiState(carparkNumber = carparkNumber))
    val uiState: StateFlow<CarparkDetailUiState> = _uiState.asStateFlow()

    init {
        loadCarparkDetail()
    }

    fun refresh() {
        loadCarparkDetail()
    }

    private fun loadCarparkDetail() {
        if (carparkNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Missing carpark number"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = carparkRepository.getCarparkDetail(carparkNumber)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = CarparkDetailUiState(
                        carparkNumber = carparkNumber,
                        isLoading = false,
                        error = null,
                        detail = detail
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to load carpark details"
                    )
                }
            )
        }
    }
}

data class CarparkDetailUiState(
    val carparkNumber: String,
    val isLoading: Boolean = false,
    val error: String? = null,
    val detail: CarparkDetail? = null
)

