package com.example.memoryjoystick.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.memoryjoystick.viewmodel.MainViewModel
import com.example.memoryjoystick.R

class GameFragment : Fragment() {
    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startGameButton = view.findViewById<Button>(R.id.startGameButton)
        startGameButton.setOnClickListener {
            viewModel.startGame()
        }

        viewModel.gameState.observe(viewLifecycleOwner) { newState ->
            Log.d("GameFragment", "Nowy stan gry: $newState")
        }
    }
}