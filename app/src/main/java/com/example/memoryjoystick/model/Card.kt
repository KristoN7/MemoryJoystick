package com.example.memoryjoystick.model

import com.example.memoryjoystick.R

data class Card(
    val id: Long = System.nanoTime(),
    val imageResId: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

fun generateCards(numberOfCards: Int): List<Card> {
    val availableImages = listOf(
        R.drawable.card_1, R.drawable.card_2, R.drawable.card_3, R.drawable.card_4,
        R.drawable.card_5, R.drawable.card_6, R.drawable.card_7, R.drawable.card_8,
        R.drawable.card_9, R.drawable.card_10, R.drawable.card_11, R.drawable.card_12,
        R.drawable.card_13, R.drawable.card_14, R.drawable.card_15, R.drawable.card_16
    )

    val numberOfPairs = numberOfCards / 2

    require(numberOfPairs <= availableImages.size) {
        "Za mało dostępnych obrazków do utworzenia $numberOfPairs par!"
    }

    val selectedImages = availableImages.shuffled().take(numberOfPairs)

    val cards = (selectedImages + selectedImages)
        .map { Card(imageResId = it) }
        .shuffled()

    return cards
}
