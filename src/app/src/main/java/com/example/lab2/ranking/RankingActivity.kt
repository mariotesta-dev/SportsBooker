package com.example.lab2.ranking

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R
import com.example.lab2.entities.Sport
import com.example.lab2.entities.User
import com.example.lab2.profile.player_profile.PlayerProfileActivity
import com.example.lab2.reservation.search_player.UserDiffCallback
import com.example.lab2.reservation.utils.AdapterRVSportFilter
import com.example.lab2.reservation.utils.FilterDiffCallback
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.NewMatchesVM
import com.example.lab2.view_models.NotificationVM
import com.example.lab2.view_models.RankingVM
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import javax.inject.Inject


@AndroidEntryPoint
class RankingActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var filterView: RecyclerView
    private lateinit var rankingContainer: ConstraintLayout
    private lateinit var loadingContainer: ConstraintLayout

    lateinit var vm: RankingVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)
        setSupportActionBar()

        vm = ViewModelProvider(this)[RankingVM::class.java]
        vm.getAllPlayers()

        filterView = findViewById(R.id.ranking_filter)
        rankingContainer = findViewById(R.id.ranking_container)
        loadingContainer = findViewById(R.id.loading_ranking)

        val adapterCardFilters = AdapterRVSportFilter(
            Sport.values().map { it.name.lowercase().replaceFirstChar(Char::titlecase) }.toList(),
            vm::setRanking
        )
        filterView.adapter = adapterCardFilters
        filterView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val recyclerViewPlayers = findViewById<RecyclerView>(R.id.recyclerViewRanking)
        recyclerViewPlayers.layoutManager = LinearLayoutManager(this)
        val adapterCard = AdapterPlayersRankList(emptyList(), this)
        recyclerViewPlayers.adapter = adapterCard

        vm.ranking.observe(this) {
            if (it != null){
                    adapterCard.setPlayers(it)
                recyclerViewPlayers.quickScrollToTop()
            }
        }

        vm.error.observe(this){
            if(it != null) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        vm.loadingState.observe(this){
            setLoadingScreen(it)
        }
    }

    private fun setLoadingScreen(state: Boolean) {
        if(state) { //is Loading
            rankingContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }else{ // is not loading
            loadingContainer.visibility = View.GONE
            rankingContainer.visibility = View.VISIBLE
        }
    }

    private fun setSupportActionBar() {
        supportActionBar?.setCustomView(R.layout.toolbar)
        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = "Ranking"

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}

class PlayerRankViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val card: MaterialCardView = v.findViewById(R.id.ranking_card)
    val fullName: TextView = v.findViewById(R.id.sender_name)
    val nickname: TextView = v.findViewById(R.id.nick)
    val profileImage: ImageView = v.findViewById(R.id.profile_image)
    val shimmer: ShimmerFrameLayout = v.findViewById(R.id.shimmer_layout)
    val points: TextView = v.findViewById(R.id.points)
}

class AdapterPlayersRankList(private var list: List<Pair<User, Long>>, private var context: Context) :
    RecyclerView.Adapter<PlayerRankViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerRankViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.player_card_rank, parent, false)
        return PlayerRankViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: PlayerRankViewHolder, position: Int) {

        val user = list[position].first
        val score = list[position].second

        if (holder.bindingAdapterPosition == 0) {
            holder.card.setCardBackgroundColor(ContextCompat.getColorStateList(this.context, R.color.gold))
        } else if (holder.bindingAdapterPosition == 1) {
            holder.card.setCardBackgroundColor(ContextCompat.getColorStateList(this.context, R.color.silver))
        } else if (holder.bindingAdapterPosition == 2) {
            holder.card.setCardBackgroundColor(ContextCompat.getColorStateList(this.context, R.color.bronze))
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColorStateList(this.context, R.color.white))
        }

        holder.fullName.text = user.full_name
        holder.nickname.text = user.nickname
        holder.points.text = "${score}pt"
        if (user.image != "") {
            Picasso.get().load(user.image).into(holder.profileImage, object :
                Callback {
                override fun onSuccess() {
                    holder.shimmer.stopShimmer()
                    holder.shimmer.hideShimmer()
                }
                override fun onError(e: Exception?) {
                }
            })
        } else {
            Picasso.get().load(R.drawable.profile_picture).into(holder.profileImage, object :
                Callback {
                override fun onSuccess() {
                    holder.shimmer.stopShimmer()
                    holder.shimmer.hideShimmer()
                }

                override fun onError(e: Exception?) {
                }
            })
        }

        holder.itemView.setOnClickListener {
            val playerIntent = Intent(holder.itemView.context, PlayerProfileActivity::class.java)
            val playerString =
                Json.encodeToString(User.serializer(), list[holder.absoluteAdapterPosition].first)
            playerIntent.putExtra("playerString", playerString)
            holder.itemView.context.startActivity(playerIntent)
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    fun setPlayers(newPlayers: List<Pair<User, Long>>) {

        val diffs = DiffUtil.calculateDiff(
            UserRankDiffCallback(list, newPlayers)
        )
        list = newPlayers
        diffs.dispatchUpdatesTo(this)
    }

}

class UserRankDiffCallback(
    private val players: List<Pair<User, Long>>,
    private val newPlayers: List<Pair<User, Long>>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = players.size

    override fun getNewListSize(): Int = newPlayers.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItemPosition == newItemPosition
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return players[oldItemPosition].first.image == newPlayers[newItemPosition].first.image &&
                players[oldItemPosition].first.full_name == newPlayers[newItemPosition].first.full_name &&
                players[oldItemPosition].second == newPlayers[newItemPosition].second
    }

}

/**
 * Smooth scroll to the top of the list, but if the distance is long,
 * first jump closer to the target and then start the scroll.
 * @param jumpThreshold the maximum number of items to scroll past
 * @param speedFactor modify the speed of the smooth scroll
 */
fun RecyclerView.quickScrollToTop(
    jumpThreshold: Int = 30,
    speedFactor: Float = 1f
) {
    val layoutManager = layoutManager as? LinearLayoutManager
        ?: error("Need to be used with a LinearLayoutManager or subclass of it")

    val smoothScroller = object : LinearSmoothScroller(context) {
        init {
            targetPosition = 0
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
            super.calculateSpeedPerPixel(displayMetrics) / speedFactor
    }

    val jumpBeforeScroll = layoutManager.findFirstVisibleItemPosition() > jumpThreshold
    if (jumpBeforeScroll) {
        layoutManager.scrollToPositionWithOffset(jumpThreshold, 0)
    }

    layoutManager.startSmoothScroll(smoothScroller)
}


class AdapterRankingFilter(
    private var listOfSport: List<String?>,
    val setRanking: (input: String?) -> Unit
) : RecyclerView.Adapter<AdapterRankingFilter.ViewHolderRankFilter>() {

    var selectedPosition = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderRankFilter {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.filter_button, parent, false)
        return ViewHolderRankFilter(v)
    }

    override fun getItemCount(): Int = listOfSport.size

    override fun onBindViewHolder(holder: ViewHolderRankFilter, position: Int) {
        val filterName = listOfSport[position]

        holder.selectionIndicator.visibility =
            if (selectedPosition == position) View.VISIBLE else View.INVISIBLE

        holder.name.text = filterName
        holder.layout.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            setRanking(filterName)
        }
    }

    inner class ViewHolderRankFilter(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.filter_name)
        val layout: ConstraintLayout = v.findViewById(R.id.filter_button_layout)
        val selectionIndicator: View = v.findViewById(R.id.selectionIndicator)
    }

}
