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
import androidx.navigation.fragment.navArgs

class GameFragment : Fragment() {

    private lateinit var gameViewModel: GameViewModel
    private var gridLayout: GridLayout? = null
    private var currentSelectedPosition = 0
    private val args: GameFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("GameFragment", "Fragment załadowany, liczba kart: ${args.numberOfCards}")

        gameViewModel = ViewModelProvider(this).get(GameViewModel::class.java)
        gridLayout = view.findViewById(R.id.grid_layout)

        gameViewModel.setNumberOfCards(args.numberOfCards)

        gameViewModel.joystickAction.observe(viewLifecycleOwner, Observer { action ->
            Log.d("GAME_ACTION", "Akcja dżojstika: $action")
            gameViewModel.handleJoystickAction(action)
        })

        gameViewModel.cards.observe(viewLifecycleOwner, Observer { currentCards ->
            Log.d("GAME_BOARD", "Zaktualizowano planszę: $currentCards")
            setupBoard(currentCards)
            highlightSelectedCard(gameViewModel.selectedCardIndex.value ?: 0)
        })

        gameViewModel.selectedCardIndex.observe(viewLifecycleOwner, Observer { index ->
            Log.d("GAME_SELECT", "Zaznaczony indeks: $index")
            highlightSelectedCard(index)
        })

        gameViewModel.connectedDeviceAddress.observe(viewLifecycleOwner, Observer { address ->
            Log.d("GAME", "Podłączono do: $address")
        })
    }

    private fun highlightSelectedCard(selectedIndex: Int) {
        for (i in 0 until (gridLayout?.childCount ?: 0)) {
            val button = gridLayout?.getChildAt(i) as? Button
            if (i == selectedIndex) {
                button?.setBackgroundResource(R.drawable.selected_card_background)
            } else {
                val card = gameViewModel.cards.value?.getOrNull(i)
                button?.setBackgroundResource(if (card?.isFaceUp == true) card.imageResId else R.drawable.card_back)
            }
        }
    }

    private fun setupBoard(cards: List<Card>) {
        gridLayout?.removeAllViews()

        val numColumns = when (cards.size) {
            8 -> 4
            16 -> 4
            32 -> 6 // Zwiększamy liczbę kolumn dla większej liczby kart
            else -> 4
        }
        gridLayout?.columnCount = numColumns
        gridLayout?.rowCount = (cards.size + numColumns - 1) / numColumns

        cards.forEachIndexed { index, card ->
            val button = Button(requireContext()).apply {
                setBackgroundResource(if (card.isFaceUp) card.imageResId else R.drawable.card_back)
                isEnabled = !card.isMatched
                setOnClickListener {
                    gameViewModel.cardClicked(card)
                    gameViewModel.setSelectedCardIndex(index)
                }
            }
            gridLayout?.addView(button)
        }
        if (cards.isNotEmpty()) {
            highlightSelectedCard(gameViewModel.selectedCardIndex.value ?: 0)
        }
    }
}