package com.bignerdranch.android.gamenews

import androidx.lifecycle.ViewModel

class GameListViewModel : ViewModel() {

    private val gameRepository = GameRepository.get()
    val gameListLiveData = gameRepository.getGames()

    fun addGame(game: Game) {
        gameRepository.addGame(game)
    }
}