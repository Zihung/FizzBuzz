package com.bignerdranch.android.gamenews

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

private const val TAG = "GameListFragment"
private const val SAVED_SUBTITLE_VISIBLE = "subtitle"

class GameListFragment : Fragment() {

    private lateinit var gameRecyclerView: RecyclerView
    private var adapter: GameAdapter? = GameAdapter(emptyList())
    private val gameListViewModel: GameListViewModel by lazy {
        ViewModelProviders.of(this).get(GameListViewModel::class.java)
    }
    private var callbacks: Callbacks? = null

    interface Callbacks {
        fun onGameSelected(gameId: UUID)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        callbacks = context as? Callbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game_list, container, false)

        gameRecyclerView =
                view.findViewById(R.id.game_recycler_view) as RecyclerView
        gameRecyclerView.layoutManager = LinearLayoutManager(context)
        gameRecyclerView.adapter = adapter

        return view
    }

    override fun onStart() {
        super.onStart()
        gameListViewModel.gameListLiveData.observe(
            viewLifecycleOwner,
            Observer { games ->
                games?.let {
                    Log.i(TAG, "Got gameLiveData ${games.size}")
                    updateUI(games)
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_game_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_game -> {
                val game = Game()
                gameListViewModel.addGame(game)
                callbacks?.onGameSelected(game.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(games: List<Game>) {
        adapter?.let {
            it.games = games
        } ?: run {
            adapter = GameAdapter(games)
        }
        gameRecyclerView.adapter = adapter
    }

    private inner class GameHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var game: Game

        private val titleTextView: TextView = itemView.findViewById(R.id.game_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.game_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.game_solved)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(game: Game) {
            this.game = game
            titleTextView.text = this.game.title
            dateTextView.text = this.game.date.toString()
            solvedImageView.visibility = if (game.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        override fun onClick(v: View) {
            callbacks?.onGameSelected(game.id)
        }
    }

    private inner class GameAdapter(var games: List<Game>)
        : RecyclerView.Adapter<GameHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : GameHolder {
            val layoutInflater = LayoutInflater.from(context)
            val view = layoutInflater.inflate(R.layout.list_item_game, parent, false)
            return GameHolder(view)
        }

        override fun onBindViewHolder(holder: GameHolder, position: Int) {
            val game = games[position]
            holder.bind(game)
        }

        override fun getItemCount() = games.size
    }

    companion object {
        fun newInstance(): GameListFragment {
            return GameListFragment()
        }
    }
}