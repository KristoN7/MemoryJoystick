package com.example.memoryjoystick

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.memoryjoystick.model.Card
import android.widget.GridLayout
import android.widget.Button
import com.example.memoryjoystick.viewmodel.GameViewModel
import androidx.lifecycle.Observer

class GameFragment : Fragment() {

    private lateinit var gameViewModel: GameViewModel
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

        gameViewModel = ViewModelProvider(this).get(GameViewModel::class.java)

        gameViewModel.cards.observe(viewLifecycleOwner, Observer { updatedCards ->
            cards = updatedCards
            setupBoard(updatedCards)
        })
    }

    private fun setupBoard(cards: List<Card>) {
        val gridLayout = view?.findViewById<GridLayout>(R.id.grid_layout) ?: return
        gridLayout.removeAllViews() // Usuń poprzednie widoki przy aktualizacji planszy

        val numColumns = when (cards.size) {
            8 -> 4
            16 -> 4
            32 -> 4 // Możesz dostosować liczbę kolumn w zależności od liczby kart
            else -> 4
        }
        gridLayout.columnCount = numColumns
        gridLayout.rowCount = cards.size / numColumns // Automatycznie oblicz liczbę wierszy

        cards.forEach { card ->
            val button = Button(requireContext()).apply {
                setBackgroundResource(if (card.isFaceUp) card.imageResId else R.drawable.card_back)
                isEnabled = !card.isMatched
                setOnClickListener {
                    gameViewModel.cardClicked(card)
                }
            }
            gridLayout.addView(button)
        }
    }
}