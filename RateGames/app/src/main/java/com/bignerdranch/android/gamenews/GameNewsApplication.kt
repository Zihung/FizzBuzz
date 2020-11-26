package com.bignerdranch.android.gamenews

import android.app.Application

class GameNewsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GameRepository.initialize(this)
    }
}