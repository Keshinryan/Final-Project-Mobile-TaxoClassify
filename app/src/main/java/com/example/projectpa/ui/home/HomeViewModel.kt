package com.example.projectpa.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Silahkan Masukkan Gambar Hewan"
    }
    val text: LiveData<String> = _text
}