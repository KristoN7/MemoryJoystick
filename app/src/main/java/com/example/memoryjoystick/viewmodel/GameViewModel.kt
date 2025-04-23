package com.example.memoryjoystick.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.memoryjoystick.model.Card
import com.example.memoryjoystick.model.generateCards

class GameViewModel : ViewModel() {
    private val _cards = MutableLiveData<List<Card>>()
    val cards: LiveData<List<Card>> = _cards

    private val _selectedCards = MutableLiveData<List<Card>>(emptyList())
    val selectedCards: LiveData<List<Card>> = _selectedCards

    private var currentLevel: String = "medium" // Domyślny poziom trudności

    init {
        generateNewCards()
    }

    fun setLevel(level: String) {
        currentLevel = level
        generateNewCards()
    }

    private fun generateNewCards() {
        _cards.value = generateCards(currentLevel)
    }

    fun cardClicked(card: Card) {
        // Logika kliknięcia karty
        val currentSelected = _selectedCards.value ?: emptyList()

        // Ignoruj kliknięcia na już odkryte lub dopasowane karty
        if (card.isFaceUp || card.isMatched) {
            return
        }

        val mutableSelected = currentSelected.toMutableList()
        mutableSelected.add(card)
        _selectedCards.value = mutableSelected

        // Odkryj klikniętą kartę
        val currentCards = _cards.value?.toMutableList() ?: return
        val index = currentCards.indexOf(card)
        if (index != -1) {
            currentCards[index] = card.copy(isFaceUp = true)
            _cards.value = currentCards
        }

        // Sprawdź, czy wybrano dwie karty
        if (mutableSelected.size == 2) {
            // Sprawdź, czy pasują
            if (mutableSelected[0].imageResId == mutableSelected[1].imageResId) {
                // Para pasuje
                markCardsAsMatched(mutableSelected[0], mutableSelected[1])
                _selectedCards.value = emptyList() // Resetuj wybrane karty
            } else {
                // Para nie pasuje - zakryj karty po opóźnieniu
                // TODO: Implementuj opóźnienie i zakrywanie kart
                resetSelectedCards()
            }
        } else if (mutableSelected.size > 2) {
            // Zresetuj, jeśli wybrano więcej niż dwie
            resetSelectedCards()
            cardClicked(card) // Ponownie kliknij aktualną kartę
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
        // Zakryj wszystkie niepasujące odkryte karty
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
}