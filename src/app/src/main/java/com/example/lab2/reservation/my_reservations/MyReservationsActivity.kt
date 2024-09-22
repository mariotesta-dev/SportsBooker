package com.example.lab2.reservation.my_reservations

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.lab2.R
import com.example.lab2.notifications.NotificationsActivity
import com.example.lab2.profile.show_profile.ShowProfileActivity
import com.example.lab2.ranking.RankingActivity
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.NotificationVM
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyReservationsActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var myProfileButton: ShimmerFrameLayout
    private lateinit var notificationsButton: FrameLayout
    private lateinit var rankingButton: ImageView

    @Inject
    lateinit var mainVM: MainVM
    lateinit var notificationVM: NotificationVM

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reservations)
        setSupportActionBar()

        notificationVM = ViewModelProvider(this)[NotificationVM::class.java]
        notificationVM.setNotificationsNumberListener()

        notificationsButton =
            supportActionBar?.customView?.findViewById<FrameLayout>(R.id.notifications)!!
        notificationsButton.setOnClickListener {
            val intentNotifications = Intent(this, NotificationsActivity::class.java)
            launcher.launch(intentNotifications)
        }

        navController = (
                supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
                ).navController

        myProfileButton =
            supportActionBar?.customView?.findViewById<ShimmerFrameLayout>(R.id.custom_my_profile)!!
        myProfileButton.setOnClickListener {
            val intentShowProfile = Intent(this, ShowProfileActivity::class.java)
            launcher.launch(intentShowProfile)
        }

        val numberOfUnseenNotificationsTextView =
            supportActionBar?.customView?.findViewById<TextView>(
                R.id.number_of_notifications
            )

        notificationVM.numberInvitations.observe(this) {
            val n = notificationVM.numberOfUnseenNotifications
            if (n > 0) {
                numberOfUnseenNotificationsTextView?.visibility = TextView.VISIBLE
                numberOfUnseenNotificationsTextView?.text = n.toString()
            } else {
                numberOfUnseenNotificationsTextView?.visibility = TextView.GONE
            }
        }

        notificationVM.numberReviews.observe(this) {
            val n = notificationVM.numberOfUnseenNotifications
            if (n > 0) {
                numberOfUnseenNotificationsTextView?.visibility = TextView.VISIBLE
                numberOfUnseenNotificationsTextView?.text = n.toString()
            } else {
                numberOfUnseenNotificationsTextView?.visibility = TextView.GONE
            }
        }

        mainVM.user.observe(this) {
            val shimmerFrame =
                supportActionBar?.customView?.findViewById<ShimmerFrameLayout>(R.id.custom_my_profile)
            val profilePicture =
                supportActionBar?.customView?.findViewById<ImageView>(R.id.toolbar_profile_image)
            setProfileImage(shimmerFrame!!,profilePicture!!)
        }
    }

    private fun setSupportActionBar() {
        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.toolbar_with_profile)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        val shimmerFrame =
            supportActionBar?.customView?.findViewById<ShimmerFrameLayout>(R.id.custom_my_profile)
        val profilePicture =
            supportActionBar?.customView?.findViewById<ImageView>(R.id.toolbar_profile_image)

        rankingButton = supportActionBar?.customView?.findViewById(R.id.ranking_icon)!!

        rankingButton.setOnClickListener {
            val rankingIntent = Intent(this, RankingActivity::class.java)
            launcher.launch(rankingIntent)
        }

        setProfileImage(shimmerFrame!!, profilePicture!!)

        titleTextView?.setText(R.string.my_reservations_title)
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