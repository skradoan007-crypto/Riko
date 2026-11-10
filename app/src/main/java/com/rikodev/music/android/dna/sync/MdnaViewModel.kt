package com.rikodev.music.android.dna.sync

import androidx.lifecycle.ViewModel
import com.rikodev.music.android.dna.settings.MdnaSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin Hilt bridge so a composable can reach the singleton [MdnaSyncHub] and [MdnaSettings]. */
@HiltViewModel
class MdnaViewModel @Inject constructor(
    val hub: MdnaSyncHub,
    val settings: MdnaSettings
) : ViewModel()
