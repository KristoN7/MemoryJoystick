package com.example.memoryjoystick.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.memoryjoystick.model.Card
import com.example.memoryjoystick.model.generateCards
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class GameViewModel : ViewModel() {
    private val _cards = MutableLiveData<List<Card>>()
    val cards: LiveData<List<Card>> = _cards
    val connectedDeviceAddress = MutableLiveData<String>()
    val joystickAction = MutableLiveData<Int>()

    private val _selectedCards = MutableLiveData<List<Card>>(emptyList())
    val selectedCards: LiveData<List<Card>> = _selectedCards

    private val _selectedCardIndex = MutableLiveData<Int>(0)
    val selectedCardIndex: LiveData<Int> = _selectedCardIndex

    private var numberOfCards: Int = 8 // Domyślna liczba kart
    private var canClick = true

    fun setNumberOfCards(count: Int) {
        numberOfCards = count
        generateNewCards()
        _selectedCardIndex.value = 0
    }

    private fun generateNewCards() {
        val level = when (numberOfCards) {
            8 -> "easy"
            16 -> "medium"
            32 -> "hard"
            else -> "easy"
        }
        _cards.value = generateCards(level)
        _selectedCardIndex.value = 0
    }

    fun cardClicked(card: Card) {
        if (!canClick) return

        val currentSelected = _selectedCards.value ?: emptyList()

        if (card.isFaceUp || card.isMatched) {
            return
        }

        val mutableSelected = currentSelected.toMutableList()
        mutableSelected.add(card)
        _selectedCards.value = mutableSelected

        val currentCards = _cards.value?.toMutableList() ?: return
        val index = currentCards.indexOf(card)
        if (index != -1) {
            currentCards[index] = card.copy(isFaceUp = true)
            _cards.value = currentCards
        }

        if (mutableSelected.size == 2) {
            canClick = false
            if (mutableSelected[0].imageResId == mutableSelected[1].imageResId) {
                markCardsAsMatched(mutableSelected[0], mutableSelected[1])
                _selectedCards.value = emptyList()
                canClick = true
            } else {
                viewModelScope.launch {
                    delay(1000)
                    resetSelectedCards()
                    canClick = true
                }
            }
        } else if (mutableSelected.size > 2) {
            resetSelectedCards()
            cardClicked(card)
        }
    }

    private fun markCardsAsMatched(card1: Card, card2: Card) {
        val currentCards = _cards.value?.toMutableList() ?: return
        val index1 = currentCards.indexOf(card1)
        val index2 = currentCards.indexOf(card2)
        if (index1 != -1 && index2 != -1) {
            currentCards[index1] = currentCards[index1].copy(isMatched = true)
            currentCards[index2] = currentCards[index2].copy(isMatched = true)
            _cards.value = currentCards
        }
    }

    private fun resetSelectedCards() {
        val currentCards = _cards.value?.toMutableList() ?: return
        val selectedAndNotMatched = _selectedCards.value?.filter { !it.isMatched } ?: emptyList()

        selectedAndNotMatched.forEach { card ->
            val index = currentCards.indexOf(card)
            if (index != -1) {
                currentCards[index] = currentCards[index].copy(isFaceUp = false)
            }
        }
        _cards.value = currentCards
        _selectedCards.value = emptyList()
    }

    fun handleJoystickAction(action: Int) {
        val currentSize = _cards.value?.size ?: 0
        if (currentSize == 0) return

        when (action) {
            1 -> { // LEFT
                _selectedCardIndex.value = (_selectedCardIndex.value!! - 1 + currentSize) % currentSize
            }
            2 -> { // DOWN
                // Logika ruchu w dół (uwzględnij liczbę kolumn)
                val numColumns = 4 // Założona liczba kolumn
                _selectedCardIndex.value = (_selectedCardIndex.value!! + numColumns) % currentSize
            }
            3 -> { // RIGHT
                _selectedCardIndex.value = (_selectedCardIndex.value!! + 1) % currentSize
            }
            4 -> { // INTERACT
                // Wybierz kartę na aktualnym indeksie
                _cards.value?.getOrNull(_selectedCardIndex.value!!)?.let { card ->
                    cardClicked(card)
                }
            }
            // Możliwa akcja UP
            // 5 -> { // UP
            //     val numColumns = 4 // Założona liczba kolumn
            //     _selectedCardIndex.value = (_selectedCardIndex.value!! - numColumns + currentSize) % currentSize
            // }
            else -> Log.d("ViewModel", "Nieznana akcja dżojstika: $action")
        }
    }

    // Metoda do "wybrania" karty za pomocą dżojstika (odpowiednik kliknięcia)
    fun cardSelected(index: Int) {
        _cards.value?.getOrNull(index)?.let { card ->
            cardClicked(card)
        }
    }

    // Metoda do aktualizacji zaznaczonego indeksu (może być potrzebne, jeśli UI zainicjuje zaznaczenie)
    fun setSelectedCardIndex(index: Int) {
        _selectedCardIndex.value = index
    }
}