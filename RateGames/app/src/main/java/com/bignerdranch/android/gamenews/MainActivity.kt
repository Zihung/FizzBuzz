package com.bignerdranch.android.gamenews

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class MainActivity : AppCompatActivity(), GameListFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = GameListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onGameSelected(gameId: UUID) {
        val fragment = GameFragment.newInstance(gameId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
