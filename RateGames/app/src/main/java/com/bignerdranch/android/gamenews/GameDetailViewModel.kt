package com.bignerdranch.android.gamenews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class GameDetailViewModel : ViewModel() {

    private val gameRepository = GameRepository.get()
    private val gameIdLiveData = MutableLiveData<UUID>()

    val gameLiveData: LiveData<Game?> =
        Transformations.switchMap(gameIdLiveData) { gameId ->
            gameRepository.getGame(gameId)
        }
    
    fun loadGame(gameId: UUID) {
        gameIdLiveData.value = gameId
    }

    fun saveGame(game: Game) {
        gameRepository.updateGame(game)
    }

    fun getPhotoFile(game: Game): File {
        return gameRepository.getPhotoFile(game)
    }
}