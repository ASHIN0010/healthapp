package com.example.healthapp.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.healthapp.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    val userProfileRepository: UserProfileRepository
) : ViewModel()
