package com.hllous.plantravel.presentation.group

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class SelectedGroupHolder @Inject constructor() {
    val selectedGroupId = MutableStateFlow<String?>(null)
}
