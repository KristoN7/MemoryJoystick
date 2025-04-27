package com.example.memoryjoystick.viewmodel

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.memoryjoystick.model.Card
import com.example.memoryjoystick.model.generateCards
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _cards = MutableLiveData<List<Card>>()
    val cards: LiveData<List<Card>> = _cards
    private val _elapsedTime = MutableLiveData<Long>(0)
    val elapsedTime: LiveData<Long> = _elapsedTime

    private var firstCardTime: Long = 0
    private var gameOver = MutableLiveData<Boolean>(false)
    private var numberOfCards: Int = 8 // Domyślna liczba kart
    val connectedDeviceAddress = MutableLiveData<String>()


    private var isProcessingTurn = false
    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private val _selectedCardIndex = MutableLiveData<Int>(0)
    val selectedCardIndex: LiveData<Int> = _selectedCardIndex
    val joystickAction = MutableLiveData<Int>() // Zachowujemy Int

    // Flag to check if timer has started
    private var timerStarted = false

    fun setNumberOfCards(count: Int) {
        numberOfCards = count
        generateNewCards()
        _selectedCardIndex.value = 0
    }

    private fun generateNewCards() {
        resetTimer()

        val level = when (numberOfCards) {
            8 -> "easy"
            16 -> "medium"
            32 -> "hard"
            else -> "easy"
        }
        _cards.value = generateCards(numberOfCards)
    }

    fun resetTimer() {
        _elapsedTime.value = 0
        firstCardTime = 0
        timerStarted = false
        gameOver.value = false
    }

    fun handleJoystickAction(action: Byte) {
        Log.d("GameViewModel", "Karty zostały wygenerowane, liczba kart: ${_cards.value?.size}")
        val currentCards = _cards.value
        val currentSize = currentCards?.size ?: 0
        if (currentSize == 0) {
            Log.d("JOYSTICK_ACTION", "Karty nie zostały załadowane.")
            return
        }


        when (action.toInt()) {
            1 -> { // LEFT
                _selectedCardIndex.value =
                    (_selectedCardIndex.value!! - 1 + currentSize) % currentSize
                Log.d(
                    "JOYSTICK_MOVE",
                    "Przesunięto w lewo, nowy indeks: ${_selectedCardIndex.value}"
                )
            }

            2 -> { // DOWN
                val numColumns = when (currentSize) {
                    8 -> 4
                    16 -> 4
                    32 -> 6
                    else -> 4
                }
                _selectedCardIndex.value = (_selectedCardIndex.value!! + numColumns) % currentSize
                Log.d(
                    "JOYSTICK_MOVE",
                    "Przesunięto w dół, nowy indeks: ${_selectedCardIndex.value}"
                )
            }

            3 -> { // RIGHT
                _selectedCardIndex.value = (_selectedCardIndex.value!! + 1) % currentSize
                Log.d(
                    "JOYSTICK_MOVE",
                    "Przesunięto w prawo, nowy indeks: ${_selectedCardIndex.value}"
                )
            }

            4 -> { // INTERACT
                _cards.value?.getOrNull(_selectedCardIndex.value!!)
                    ?.let { card -> cardClicked(card) }
                Log.d("JOYSTICK_ACTION", "Akcja SELEKCJA na indeksie: ${_selectedCardIndex.value}")
            }

            else -> Log.d("ViewModel", "Nieznana akcja dżojstika: $action")
        }
    }


    fun cardClicked(card: Card) {
        if (isProcessingTurn) return
        if (card.isFaceUp || card.isMatched) return

        if (!timerStarted) {
            startTimer()
            timerStarted = true
        }

        card.isFaceUp = true
        _cards.postValue(_cards.value)

        if (firstSelectedCard == null) {
            firstSelectedCard = card
        } else if (secondSelectedCard == null) {
            secondSelectedCard = card
            isProcessingTurn = true

            if (firstSelectedCard?.imageResId == secondSelectedCard?.imageResId) {
                firstSelectedCard?.isMatched = true
                secondSelectedCard?.isMatched = true

                firstSelectedCard = null
                secondSelectedCard = null
                isProcessingTurn = false
                _cards.postValue(_cards.value)

                checkGameOver()
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    //Zakrywanie karty po 2 sekundach, jeśli się nie dopasowały
                    firstSelectedCard?.isFaceUp = false
                    secondSelectedCard?.isFaceUp = false

                    firstSelectedCard = null
                    secondSelectedCard = null
                    isProcessingTurn = false
                    _cards.postValue(_cards.value)
                }, 2000)
            }
        }
    }

    private fun checkGameOver() {
        if (_cards.value?.all { it.isMatched } == true) {
            gameOver.value = true
            stopTimer()
        }
    }

    private fun startTimer() {
        firstCardTime = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            while (!gameOver.value!!) {
                _elapsedTime.value = SystemClock.elapsedRealtime() - firstCardTime
                delay(1000)
            }
        }
    }

    fun isGameOver(): LiveData<Boolean> {
        return gameOver
    }

    private fun stopTimer() {
        _elapsedTime.value = SystemClock.elapsedRealtime() - firstCardTime
    }

    fun updateCards(cards: List<Card>) {
        _cards.value = cards
    }
}