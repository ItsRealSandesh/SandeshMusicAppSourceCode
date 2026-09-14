package com.sandeshmusic.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sandeshmusic.app.data.preferences.ThemeMode
import com.sandeshmusic.app.data.preferences.ThemePreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val preferencesRepository: ThemePreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.DARK
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleLightDark() {
        val current = themeMode.value
        val next = if (current == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
        setThemeMode(next)
    }

    class Factory(
        private val preferencesRepository: ThemePreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemeViewModel(preferencesRepository) as T
        }
    }
}
