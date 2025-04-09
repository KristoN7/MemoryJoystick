package com.example.memoryjoystick.repository

class GameRepository {
    fun getShuffledCards(): List<Int> {
        return (1..10).shuffled() // Na razie prosta lista
    }
}