package com.example.lab2.notifications

import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R
import com.example.lab2.common.RatingModalBottomSheet
import com.example.lab2.common.calendar.displayText
import com.example.lab2.entities.Invitation
import com.example.lab2.entities.MatchToReview
import com.example.lab2.entities.Notification
import com.example.lab2.view_models.NotificationVM
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.Timestamp
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity(), NotificationAdapter.OnClickListener {


    @Inject
    lateinit var notificationVM: NotificationVM

    private lateinit var backButton: ImageView
    private lateinit var noNotifications: ConstraintLayout
    private lateinit var recyclerViewNotifications: RecyclerView
    private lateinit var notificationsContainer: ConstraintLayout
    private lateinit var loadingContainer: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setSupportActionBar()
        findViews()


        recyclerViewNotifications.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val adapterCard = NotificationAdapter(mutableListOf(), this)
        recyclerViewNotifications.adapter = adapterCard
        val swipeToDeleteCallback = SwipeToDeleteCallback(adapterCard, applicationContext)
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewNotifications)
        recyclerViewNotifications.scrollToPosition(0)

        notificationVM.notifications.observe(this){
            Log.i("notifications", it.toString())
            if(it.isEmpty()){
                noNotifications.visibility = View.VISIBLE
                notificationsContainer.visibility = View.GONE
            }else{
                notificationsContainer.visibility = View.VISIBLE
                noNotifications.visibility = View.GONE
                adapterCard.setNotification(it)
                recyclerViewNotifications.scrollToPosition(0)
                it.forEach { n -> if(n is Invitation) notificationVM.playerHasSeenNotification(n) }
            }
        }

        notificationVM.error.observe(this){
            if(it != null){
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        notificationVM.loadingState.observe(this){
            setLoadingScreen(it)
        }

    }

    private fun setLoadingScreen(state: Boolean) {
        if(state) { //is Loading
            noNotifications.visibility = View.GONE
            notificationsContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }else{ // is not loading
            loadingContainer.visibility = View.GONE
            notificationsContainer.visibility = View.VISIBLE
        }
    }

    private fun findViews() {
        recyclerViewNotifications = findViewById<RecyclerView>(R.id.recyclerViewNotifications)
        notificationsContainer = findViewById(R.id.notifications_container)
        loadingContainer = findViewById(R.id.loading_notifications)
        noNotifications = findViewById(R.id.no_notifications_layout)
    }

    override fun onClickAccept(invitation: Invitation) {
        notificationVM.joinTheMatch(invitation)
    }

    override fun onClickDecline(invitationId: String) {
        notificationVM.deleteNotification(invitationId)
    }

    override fun onClickDeleteReviewNotification(invitationId: String, matchId: String) {
        notificationVM.deleteReviewNotification(invitationId, matchId)
    }

    override fun onClickRateNow(matchToReview: MatchToReview) {

        val bundle = Bundle()
        bundle.putString("matchId", matchToReview.match?.matchId)
        val modalBottomSheet = RatingModalBottomSheet()
        modalBottomSheet.matchToReview = matchToReview
        modalBottomSheet.notifications = notificationVM._notifications
        modalBottomSheet.arguments = bundle
        modalBottomSheet.show(supportFragmentManager, RatingModalBottomSheet.TAG)

    }

    private fun setSupportActionBar() {
        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.toolbar)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.setText(R.string.notifications_title)

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}


class NotificationAdapter(
    private var list: MutableList<Notification>,
    private val listener: OnClickListener
) : RecyclerView.Adapter<NotificationAdapter.NotificationVH>() {

    abstract class NotificationVH(v: View) : RecyclerView.ViewHolder(v) {
        abstract fun bind(notification: Notification)
    }

    companion object {
        private const val VIEW_TYPE_INVITATION = 0
        private const val VIEW_TYPE_MATCH_TO_REVIEW = 1
    }

    interface OnClickListener {
        fun onClickAccept(invitation: Invitation)
        fun onClickDecline(invitationId: String)
        fun onClickDeleteReviewNotification(invitationId: String, matchId: String)
        fun onClickRateNow(matchToReview: MatchToReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationVH {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.invitation_card -> InvitationViewHolder(v)
            R.layout.match_to_review_card -> MatchToReviewViewHolder(v)
            else -> throw IllegalArgumentException("Invalid type of data $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is Invitation -> R.layout.invitation_card
            is MatchToReview -> R.layout.match_to_review_card
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    override fun onBindViewHolder(holder: NotificationVH, position: Int) {
        holder.bind(list[position])
    }

    inner class InvitationViewHolder(v: View) : NotificationVH(v) {
        val senderName: TextView = v.findViewById(R.id.sender_name)
        val profileImage: ImageView = v.findViewById(R.id.profile_image)
        val shimmer: ShimmerFrameLayout = v.findViewById(R.id.shimmer_layout)
        val sportName: TextView = v.findViewById(R.id.sport_name)
        val dateDetail: TextView = v.findViewById(R.id.date_detail)
        val timeDetail: TextView = v.findViewById(R.id.hour_detail)
        val acceptButton: Button = v.findViewById(R.id.accept_invitation_button)
        val declineButton: Button = v.findViewById(R.id.decline_invitation_button)
        val notificationTime: TextView = v.findViewById(R.id.notification_time)

        override fun bind(notification: Notification) {
            val invitation = notification as Invitation
            senderName.text = invitation.sender.full_name
            Picasso.get().load(invitation.sender.image).into(profileImage, object :
                Callback {
                override fun onSuccess() {
                    shimmer.stopShimmer()
                    shimmer.hideShimmer()
                }

                override fun onError(e: Exception?) {
                }
            })
            sportName.text = invitation.court.sport
            dateDetail.text = setupDate(invitation.match?.date!!)
            timeDetail.text = invitation.match.time.format(DateTimeFormatter.ofPattern("HH:mm"))
            acceptButton.setOnClickListener {
                listener.onClickAccept(invitation)
            }
            declineButton.setOnClickListener {
                listener.onClickDecline(invitation.id!!)
            }
            notificationTime.text = getTimeAgo(invitation.timestamp)
        }
    }

    inner class MatchToReviewViewHolder(v: View) : NotificationVH(v) {
        val rateNowButton: Button = v.findViewById(R.id.rate_now_button)
        val sportName: TextView = v.findViewById(R.id.sport_name)
        val dateDetail: TextView = v.findViewById(R.id.date_detail)
        val hourDetail: TextView = v.findViewById(R.id.hour_detail)
        val notificationTime: TextView = v.findViewById(R.id.notification_time)

        override fun bind(notification: Notification) {
            val matchToReview = notification as MatchToReview
            sportName.text = matchToReview.court.sport
            dateDetail.text = setupDate(matchToReview.match?.date!!)
            hourDetail.text = matchToReview.match.time.format(DateTimeFormatter.ofPattern("HH:mm"))
            notificationTime.text = getTimeAgo(matchToReview.timestamp)
            rateNowButton.setOnClickListener {
                listener.onClickRateNow(matchToReview)
            }
        }
    }

    private fun getTimeAgo(timestamp: Timestamp): String {
        val now = Timestamp.now()
        val difference = now.seconds - timestamp.seconds

        val seconds = difference
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = weeks / 4
        val years = months / 12

        return when {
            years > 0 -> "$years ${if (years == 1L) "year" else "years"} ago"
            months > 0 -> "$months ${if (months == 1L) "month" else "months"} ago"
            weeks > 0 -> "$weeks ${if (weeks == 1L) "week" else "weeks"} ago"
            days > 0 -> "$days ${if (days == 1L) "day" else "days"} ago"
            hours > 0 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            minutes > 0 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            else -> "$seconds ${if (seconds == 1L) "second" else "seconds"} ago"
        }
    }

    fun setNotification(newListNotifications: MutableList<Notification>) {

        val diffs = DiffUtil.calculateDiff(
            NotificationDiffCallback(list, newListNotifications)
        )
        list = newListNotifications
        diffs.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun setupDate(date: LocalDate): String {
        return "${date.dayOfWeek.displayText()} ${date.format(DateTimeFormatter.ofPattern("dd"))} ${date.month.displayText()}"
    }

    fun deleteItem(position: Int) {
        when (getItemViewType(position)) {
            R.layout.invitation_card -> listener.onClickDecline(list[position].id!!)
            R.layout.match_to_review_card -> listener.onClickDeleteReviewNotification(list[position].id!!, list[position].match?.matchId!!)
            else -> throw IllegalArgumentException("Invalid type of data in notifications.")
        }
    }

}

class SwipeToDeleteCallback(
    private val adapter: NotificationAdapter,
    private val context: Context
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.baseline_delete_24)
    private val background = ColorDrawable(Color.RED)
    private val iconMargin = 16

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // No action needed for move gesture
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.absoluteAdapterPosition
        adapter.deleteItem(position)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            return
        }

        // Draw the background color
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // Calculate the position to draw the delete icon
        val iconTop = itemView.top + (itemHeight - deleteIcon?.intrinsicHeight!!) / 2
        val iconBottom = iconTop + deleteIcon.intrinsicHeight
        val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
        val iconRight = itemView.right - iconMargin

        // Draw the delete icon
        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val paint = Paint()
        paint.color = Color.RED
        c.drawRect(left, top, right, bottom, paint)
    }
}