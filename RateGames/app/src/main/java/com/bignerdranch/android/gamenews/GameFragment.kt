package com.bignerdranch.android.gamenews

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.io.File
import java.util.*

private const val TAG = "GameFragment"
private const val ARG_GAME_ID = "game_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "EEE, MMM, dd"

class GameFragment : Fragment(), DatePickerFragment.Callbacks {

    private lateinit var game: Game
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private val gameDetailViewModel: GameDetailViewModel by lazy {
        ViewModelProviders.of(this).get(GameDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        game = Game()
        val gameId: UUID = arguments?.getSerializable(ARG_GAME_ID) as UUID
        gameDetailViewModel.loadGame(gameId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        titleField = view.findViewById(R.id.game_title) as EditText
        dateButton = view.findViewById(R.id.game_date) as Button
        solvedCheckBox = view.findViewById(R.id.game_solved) as CheckBox
        reportButton = view.findViewById(R.id.game_report) as Button
        suspectButton = view.findViewById(R.id.game_suspect) as Button
        photoButton = view.findViewById(R.id.game_camera) as ImageButton
        photoView = view.findViewById(R.id.game_photo) as ImageView

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gameId = arguments?.getSerializable(ARG_GAME_ID) as UUID
        gameDetailViewModel.loadGame(gameId)
        gameDetailViewModel.gameLiveData.observe(
            viewLifecycleOwner,
            Observer { game ->
                game?.let {
                    this.game = game
                    photoFile = gameDetailViewModel.getPhotoFile(game)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "com.bignerdranch.android.gamenews.fileprovider",
                        photoFile)
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                game.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This one too
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                game.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(game.date).apply {
                setTargetFragment(this@GameFragment, REQUEST_DATE)
                show(this@GameFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getGammeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.game_report_subject))
            }.also { intent ->
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        gameDetailViewModel.saveGame(game)
    }

    override fun onDetach() {
        super.onDetach()
        // Revoke photo permissions if the user leaves without taking a photo
        requireActivity().revokeUriPermission(photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        game.date = date
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(game.title)
        dateButton.text = game.date.toString()
        solvedCheckBox.isChecked = game.isSolved
        if (game.suspect.isNotEmpty()) {
            suspectButton.text = game.suspect
        }
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription =
                getString(R.string.game_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription =
                getString(R.string.game_photo_no_image_description)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                // Specify which fields you want your query to return values for.
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // Perform your query - the contactUri is like a "where" clause here
                val cursor = requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }

                    // Pull out the first column of the first row of data -
                    // that is your suspect's name.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    game.suspect = suspect
                    gameDetailViewModel.saveGame(game)
                    suspectButton.text = suspect
                }
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                updatePhotoView()
            }
        }
    }

    private fun getGammeReport(): String {
        val solvedString = if (game.isSolved) {
            getString(R.string.game_report_solved)
        } else {
            getString(R.string.game_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, game.date).toString()
        val suspect = if (game.suspect.isBlank()) {
            getString(R.string.game_report_no_suspect)
        } else {
            getString(R.string.game_report_suspect, game.suspect)
        }

        return getString(R.string.game_report,
            game.title, dateString, solvedString, suspect)
    }

    companion object {

        fun newInstance(gameId: UUID): GameFragment {
            val args = Bundle().apply {
                putSerializable(ARG_GAME_ID, gameId)
            }
            return GameFragment().apply {
                arguments = args
            }
        }
    }
}