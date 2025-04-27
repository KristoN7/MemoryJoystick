package com.example.memoryjoystick

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class DifficultyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_difficulty, container, false)
        Log.d("DifficultyFragment", "onCreateView")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DifficultyFragment", "onViewCreated")

        view.findViewById<View>(R.id.easyButton).setOnClickListener {
            navigateToGame(8) // Easy: 4 pary = 8 kart
        }

        view.findViewById<View>(R.id.mediumButton).setOnClickListener {
            navigateToGame(16) // Medium: 8 par = 16 kart
        }

        view.findViewById<View>(R.id.hardButton).setOnClickListener {
            navigateToGame(32) // Hard: 16 par = 32 karty
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("DifficultyFragment", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("DifficultyFragment", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("DifficultyFragment", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("DifficultyFragment", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DifficultyFragment", "onDestroy")
    }

    private fun navigateToGame(numberOfCards: Int) {
        val action = DifficultyFragmentDirections.actionDifficultyFragmentToGameFragment(numberOfCards)
        findNavController().navigate(action)
    }
}