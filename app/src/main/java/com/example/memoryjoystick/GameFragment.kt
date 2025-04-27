package com.example.memoryjoystick

import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.memoryjoystick.model.Card
import com.example.memoryjoystick.viewmodel.GameViewModel

class GameFragment : Fragment() {

    private lateinit var gameViewModel: GameViewModel
    private var gridLayout: GridLayout? = null
    private lateinit var timerTextView: TextView
    private lateinit var gameOverTextView: TextView
    private val args: GameFragmentArgs by navArgs()

    // Zmienna do kontroli czy obracamy karty
    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private var isProcessingTurn = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameViewModel = activityViewModels<GameViewModel>().value
        gridLayout = view.findViewById(R.id.grid_layout)
        timerTextView = view.findViewById(R.id.timerTextView)
        gameOverTextView = view.findViewById(R.id.gameOverTextView)

        Log.d("GameFragment", "Fragment załadowany, liczba kart: ${args.numberOfCards}")

        gameViewModel.setNumberOfCards(args.numberOfCards)

        //Obserwowanie zmian kart
        gameViewModel.cards.observe(viewLifecycleOwner, Observer { currentCards ->
            setupBoard(currentCards)
            highlightSelectedCard(gameViewModel.selectedCardIndex.value ?: 0)
        })

        //Obserwowanie zmiany indeksu wybranej karty
        gameViewModel.selectedCardIndex.observe(viewLifecycleOwner, Observer { index ->
            highlightSelectedCard(index)
        })

        //Obserwowanie zmiany czasu
        gameViewModel.elapsedTime.observe(viewLifecycleOwner, Observer { time ->
            val minutes = time / 1000 / 60
            val seconds = (time / 1000) % 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
        })

        //Obserwowanie zakończenia gry
        gameViewModel.isGameOver().observe(viewLifecycleOwner, Observer { gameOver ->
            if (gameOver) {
                gameOverTextView.visibility = View.VISIBLE
                gameOverTextView.text = "YOU WON!"
                disableAllCards()
            }
        })

        //Obsługa akcji joysticka
        gameViewModel.joystickAction.observe(viewLifecycleOwner, Observer { action ->
            gameViewModel.handleJoystickAction(action.toByte())

            val selectedCardIndex = gameViewModel.selectedCardIndex.value ?: 0
            highlightSelectedCard(selectedCardIndex)
        })
    }

    private fun highlightSelectedCard(selectedIndex: Int) {
        for (i in 0 until (gridLayout?.childCount ?: 0)) {
            val button = gridLayout?.getChildAt(i) as? Button
            val card = gameViewModel.cards.value?.getOrNull(i)

            val drawable = ContextCompat.getDrawable(requireContext(),
                if (card?.isFaceUp == true) card.imageResId else R.drawable.card_back)

            val frame = if (i == selectedIndex) {
                ContextCompat.getDrawable(requireContext(), R.drawable.selected_card_background)
            } else {
                null
            }

            val layerDrawable = if (frame != null) {
                LayerDrawable(arrayOf(drawable, frame))
            } else {
                drawable
            }

            button?.background = layerDrawable
        }
    }

    private fun setupBoard(cards: List<Card>) {
        gridLayout?.removeAllViews()

        val numColumns = when (cards.size) {
            8 -> 4
            16 -> 4
            32 -> 4
            else -> 4
        }
        val numRows = (cards.size + numColumns - 1) / numColumns
        gridLayout?.columnCount = numColumns
        gridLayout?.rowCount = numRows

        val spacing = 16

        cards.forEachIndexed { index, card ->
            val button = Button(requireContext()).apply {
                //przezroczysta
                val backgroundResId = if (card.isFaceUp) {
                    card.imageResId
                } else {
                    R.drawable.card_back
                }
                //karta + ramka
                val drawable = ContextCompat.getDrawable(context, backgroundResId)
                val frame = ContextCompat.getDrawable(context, R.drawable.selected_card_background)


                val layerDrawable = LayerDrawable(arrayOf(drawable, frame))

                background = layerDrawable

                isEnabled = !card.isMatched
                setOnClickListener {
                    cardClicked(card, index)
                }
            }

            val params = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0
                height = 0
                setMargins(spacing, spacing, spacing, spacing)
            }
            button.layoutParams = params
            gridLayout?.addView(button)
        }
    }

    private fun cardClicked(card: Card, index: Int) {
        if (isProcessingTurn) return
        if (card.isFaceUp || card.isMatched) return

        card.isFaceUp = true
        gameViewModel.updateCards(gameViewModel.cards.value ?: emptyList())

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
                gameViewModel.updateCards(gameViewModel.cards.value ?: emptyList())
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    firstSelectedCard?.isFaceUp = false
                    secondSelectedCard?.isFaceUp = false

                    firstSelectedCard = null
                    secondSelectedCard = null
                    isProcessingTurn = false
                    gameViewModel.updateCards(gameViewModel.cards.value ?: emptyList())
                }, 2000)
            }
        }
    }

    private fun disableAllCards() {
        //Zablokowanie wszystkich kart po zakończeniu gry
        for (i in 0 until (gridLayout?.childCount ?: 0)) {
            val button = gridLayout?.getChildAt(i) as? Button
            button?.isEnabled = false
        }
    }
}
