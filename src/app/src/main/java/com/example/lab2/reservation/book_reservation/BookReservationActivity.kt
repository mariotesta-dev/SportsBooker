package com.example.lab2.reservation.book_reservation

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.lab2.R
import com.example.lab2.profile.show_profile.ShowProfileActivity
import com.example.lab2.view_models.MainVM
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BookReservationActivity : AppCompatActivity() {

    @Inject
    lateinit var mainVM: MainVM

    private lateinit var navController: NavController
    private lateinit var backButton: ImageView
    private lateinit var myProfileButton: ShimmerFrameLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reservation)

        setSupportActionBar()

        navController = (
                supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
                ).navController

    }

    private fun setSupportActionBar() {
        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.toolbar_with_profile_and_back)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = "Join a match"
        myProfileButton =
            supportActionBar?.customView?.findViewById<ShimmerFrameLayout>(R.id.custom_my_profile)!!
        val profilePicture =
            supportActionBar?.customView?.findViewById<ImageView>(R.id.toolbar_profile_image)!!

        setProfileImage(myProfileButton, profilePicture)
        myProfileButton.setOnClickListener {
            val intentShowProfile = Intent(this, ShowProfileActivity::class.java)
            startActivity(intentShowProfile)
        }
        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setProfileImage(shimmerFrame: ShimmerFrameLayout, profilePicture: ImageView) {
        Picasso.get().load(mainVM.user.value?.image).into(profilePicture, object :
            Callback {
            override fun onSuccess() {
                shimmerFrame.stopShimmer()
                shimmerFrame.hideShimmer()
            }

            override fun onError(e: Exception?) {
            }
        })
    }

}