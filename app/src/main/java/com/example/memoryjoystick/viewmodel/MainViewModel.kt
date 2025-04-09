package com.example.memoryjoystick.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.memoryjoystick.repository.GameRepository

class MainViewModel : ViewModel() {
    private val repository = GameRepository()
    private val _gameState = MutableLiveData<List<Int>>()
    val gameState: LiveData<List<Int>> = _gameState

    fun startGame() {
        _gameState.value = repository.getShuffledCards()
    }
}