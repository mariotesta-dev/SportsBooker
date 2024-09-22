package com.example.lab2.profile.player_profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.entities.User
import com.example.lab2.profile.BadgeView
import com.example.lab2.profile.InterestView
import com.example.lab2.profile.SkillAdapter
import com.example.lab2.profile.StatisticView
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.MyReservationsVM
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class PlayerProfileActivity : AppCompatActivity() {

    private lateinit var fullName: TextView
    private lateinit var nickname: TextView
    private lateinit var location: TextView
    private lateinit var description: TextView
    private lateinit var age: TextView
    private lateinit var profileImage: ImageView
    private lateinit var interestsLayout: LinearLayout
    private lateinit var badgesLayout: RelativeLayout
    private lateinit var statisticsLayout: LinearLayout
    private lateinit var backButton: ImageView
    private lateinit var profileContainer: ScrollView
    private lateinit var loadingContainer: ConstraintLayout


    @Inject
    lateinit var vm: MainVM
    lateinit var reservationVm: MyReservationsVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_profile)
        setSupportActionBar()
        findViews()

        // Set ViewModels & Current User Data
        reservationVm = ViewModelProvider(this)[MyReservationsVM::class.java]

        val playerString = intent.getStringExtra("playerString")
        val player = Json.decodeFromString(User.serializer(), playerString!!)

        reservationVm.setPlayerToShow(player)

        reservationVm.playerToShow.observe(this) {
            updateContent()
            updateActionBarTitle()
        }

        reservationVm.refreshMyStatistics(playerId = player.userId)

        badgesLayout.setOnClickListener { showCustomDialog() }

        reservationVm.getMyStatistics().observe(this) {
            updateStatistics()
        }

        backButton.setOnClickListener {
            finish()
        }

        reservationVm.error.observe(this){
            if(it != null){
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        reservationVm.loadingState.observe(this){
            setLoadingScreen(it)
        }

    }

    private fun setLoadingScreen(state: Boolean) {
        if(state) { //is Loading
            profileContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }else{ // is not loading
            loadingContainer.visibility = View.GONE
            profileContainer.visibility = View.VISIBLE
        }
    }

    private fun findViews() {
        fullName = findViewById(R.id.nameSurname)
        nickname = findViewById(R.id.nickname)
        location = findViewById(R.id.location)
        description = findViewById(R.id.description)
        profileImage = findViewById(R.id.profile_image)
        age = findViewById(R.id.age)
        interestsLayout = findViewById(R.id.profile_interests)
        badgesLayout = findViewById(R.id.profile_badges)
        statisticsLayout = findViewById(R.id.profile_statistics)
        profileContainer = findViewById(R.id.profile_container)
        loadingContainer = findViewById(R.id.loading_profile)
    }

    @SuppressLint("SetTextI18n")
    private fun updateContent() {
        fullName.text = reservationVm.playerToShow.value?.full_name
        nickname.text = "@${reservationVm.playerToShow.value?.nickname}"
        description.text = reservationVm.playerToShow.value?.description
        location.text = reservationVm.playerToShow.value?.address
        age.text = "${reservationVm.playerToShow.value?.getAge()}yo"

        if (reservationVm.playerToShow.value?.image == "") {
            profileImage.setBackgroundResource(R.drawable.profile_picture)
        } else {
            val profileImageUrl = reservationVm.playerToShow.value?.image
            Picasso.get().load(profileImageUrl).into(profileImage)
        }

        interestsLayout.removeAllViews()
        statisticsLayout.removeAllViews()

        val interests = reservationVm.playerToShow.value?.interests?.sortedBy { it.name }
            ?.map { InterestView(this, sport = it) }
        interests?.forEach { interestsLayout.addView(it) }

    }

    private fun updateStatistics() {
        val scores = reservationVm.getMyStatistics().value?.second
        val statistics = reservationVm.getMyStatistics().value?.first?.sortedBy { it.sport.name }
            ?.map {
                StatisticView(
                    this,
                    statistic = it,
                    score = scores?.get(it.sport.name.lowercase().replaceFirstChar(Char::titlecase))
                )
            }

        if (statistics.isNullOrEmpty()) {
            findViewById<TextView>(R.id.no_stats).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.no_stats).visibility = View.GONE
            statistics.forEach { statisticsLayout.addView(it) }
        }
    }

    private fun setSupportActionBar() {
        supportActionBar?.elevation = 0f
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.setCustomView(R.layout.toolbar)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.setText(R.string.profile_title)

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
    }

    private fun updateActionBarTitle() {
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = reservationVm.playerToShow.value?.nickname
    }

    private fun showCustomDialog() {

        val skillsDialog = Dialog(this)
        skillsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE) // Remove default title
        skillsDialog.setCancelable(true) // Allow user to exit dialog by clicking outside
        skillsDialog.setContentView(R.layout.skills_dialog)
        skillsDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val exitButton = skillsDialog.findViewById<Button>(R.id.close_skills_dialog)
        val skillsContainer = skillsDialog.findViewById<GridView>(R.id.skills_container)
        skillsContainer.adapter = SkillAdapter(this, reservationVm.playerToShow.value?.badges!!)


        exitButton.setOnClickListener {
            skillsDialog.dismiss()
        }

        skillsDialog.show()
    }

}

