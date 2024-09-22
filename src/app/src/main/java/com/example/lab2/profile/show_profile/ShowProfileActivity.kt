package com.example.lab2.profile.show_profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.launcher.LauncherActivity
import com.example.lab2.profile.BadgeView
import com.example.lab2.profile.InterestView
import com.example.lab2.profile.SkillAdapter
import com.example.lab2.profile.StatisticView
import com.example.lab2.profile.edit_profile.EditProfileActivity
import com.example.lab2.ranking.RankingActivity
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.MyReservationsVM
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShowProfileActivity : AppCompatActivity() {

    private lateinit var fullName: TextView
    private lateinit var nickname: TextView
    private lateinit var location: TextView
    private lateinit var description: TextView
    private lateinit var age: TextView
    private lateinit var profileImage: ImageView
    private lateinit var interestsLayout: LinearLayout
    private lateinit var badgesLayout: RelativeLayout
    private lateinit var statisticsLayout: LinearLayout
    private lateinit var backButton: ImageButton
    private lateinit var profileContainer: ScrollView
    private lateinit var loadingContainer: ConstraintLayout

    @Inject
    lateinit var vm: MainVM
    lateinit var reservationVm: MyReservationsVM


    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { processResponse(it) }

    private fun processResponse(response: androidx.activity.result.ActivityResult) {
        if (response.resultCode == RESULT_OK) {
            updateContent()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar()
        findViews()

        // Set ViewModels & Current User Data
        reservationVm = ViewModelProvider(this)[MyReservationsVM::class.java]

        // TODO : Statistics need to be retrieved from Firebase!
        reservationVm.refreshMyStatistics(playerId = vm.userId)


        badgesLayout.setOnClickListener { showCustomDialog() }

        vm.user.observe(this) {
            updateContent()
        }

        reservationVm.getMyStatistics().observe(this) {
            updateContent()
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
        fullName.text = vm.user.value?.full_name
        nickname.text = "@${vm.user.value?.nickname}"
        description.text = vm.user.value?.description
        location.text = vm.user.value?.address
        age.text = "${vm.user.value?.getAge()}yo"

        if (vm.user.value?.image == "") {
            profileImage.setBackgroundResource(R.drawable.profile_picture)
        } else {
            val profileImageUrl = vm.user.value?.image
            Picasso.get().load(profileImageUrl).into(profileImage)
        }

        interestsLayout.removeAllViews()
        statisticsLayout.removeAllViews()

        val interests =
            vm.user.value?.interests?.sortedBy { it.name }?.map { InterestView(this, sport = it) }
        interests?.forEach { interestsLayout.addView(it) }

        val scores = reservationVm.getMyStatistics().value?.second
        Log.i("scoreProfile", scores.toString())
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
        supportActionBar?.setCustomView(R.layout.toolbar_show_profile)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title_show_profile)
        titleTextView?.setText(R.string.profile_title)
        backButton = supportActionBar?.customView?.findViewById(R.id.edit_profile_back_button)!!

    }

    private fun showCustomDialog() {

        val skillsDialog = Dialog(this)
        skillsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE) // Remove default title
        skillsDialog.setCancelable(true) // Allow user to exit dialog by clicking outside
        skillsDialog.setContentView(R.layout.skills_dialog)
        skillsDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val exitButton = skillsDialog.findViewById<Button>(R.id.close_skills_dialog)
        val skillsContainer = skillsDialog.findViewById<GridView>(R.id.skills_container)
        skillsContainer.adapter = SkillAdapter(this, vm.user.value?.badges!!)


        exitButton.setOnClickListener {
            skillsDialog.dismiss()
        }

        skillsDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editmenu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itemEdit -> {
                val intentEditProfile = Intent(this, EditProfileActivity::class.java).apply {
                    addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE)
                    putExtra("user", vm.user.value?.toJson())
                }
                launcher.launch(intentEditProfile)
                true
            }

            R.id.itemLogout -> {
                vm.logout {
                    val intent = Intent(this, LauncherActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}

