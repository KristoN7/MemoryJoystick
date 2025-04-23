package com.example.memoryjoystick

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.memoryjoystick.R
import com.example.memoryjoystick.model.Card
import com.example.memoryjoystick.model.generateCards
import android.widget.GridLayout
import android.widget.Button

class GameFragment : Fragment() {

    private var cards: List<Card> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            Log.d("GameFragment", "Fragment załadowany")

            cards = generateCards("medium")
            Log.d("GameFragment", "Karty wygenerowane: ${cards.size}")

            setupBoard(cards)
    }

    private fun setupBoard(cards: List<Card>) {
        val gridLayout = view?.findViewById<GridLayout>(R.id.grid_layout)
        gridLayout?.rowCount = 4
        gridLayout?.columnCount = 4

        cards.forEach { card ->
            val button = Button(requireContext())
            button.setBackgroundResource(R.drawable.card_back)  // Ustawiamy tylne strony kart
            button.setOnClickListener {
                onCardClicked(card, button)
            }
            gridLayout?.addView(button)
        }
    }

    private fun onCardClicked(card: Card, button: Button) {
        // Logika odkrywania kart
    }
}
