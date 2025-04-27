package com.example.memoryjoystick.model

import com.example.memoryjoystick.R

// Klasa reprezentująca kartę
data class Card(
    val id: Long = System.nanoTime(), // Unikalny identyfikator karty (generowany w momencie tworzenia)
    val imageResId: Int,   // Id obrazu z drawable
    var isFaceUp: Boolean = false,   // Stan karty (odkryta/zakryta)
    var isMatched: Boolean = false  // Sprawdzenie, czy para została dopasowana
)

// Funkcja generująca listę kart w zależności od poziomu trudności
fun generateCards(level: String): List<Card> {
    val cardImages = listOf(
        R.drawable.card_1, R.drawable.card_2, R.drawable.card_3, R.drawable.card_4,
        R.drawable.card_5, R.drawable.card_6, R.drawable.card_7, R.drawable.card_8
    ) // Załóżmy, że mamy 8 różnych obrazków

    val numPairs: Int = when (level) {
        "easy" -> 4   // 4 pary kart (8 kart)
        "medium" -> 8  // 8 par kart (16 kart)
        "hard" -> 16  // 16 par kart (32 karty)
        else -> 4
    }

    val cards = mutableListOf<Card>()

    // Tworzymy pary kart
    for (i in 0 until numPairs) {
        val image = cardImages[i % cardImages.size]
        cards.add(Card(imageResId = image))
        cards.add(Card(imageResId = image))
    }

    // Mieszamy karty
    cards.shuffle()
    return cards
}

// Funkcja sprawdzająca, czy gra została zakończona
fun checkGameOver(cards: List<Card>): Boolean {
    return cards.all { it.isMatched }
}