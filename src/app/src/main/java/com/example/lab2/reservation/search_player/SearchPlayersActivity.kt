package com.example.lab2.reservation.search_player

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R
import com.example.lab2.entities.User
import com.example.lab2.profile.player_profile.PlayerProfileActivity
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.NotificationVM
import com.example.lab2.entities.Match
import com.example.lab2.entities.MatchWithCourtAndEquipments
import com.example.lab2.view_models.SearchPlayersVM
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject


@AndroidEntryPoint
class SearchPlayersActivity : AppCompatActivity(), AdapterPlayersList.OnClickListener {

    @Inject
    lateinit var mainVM: MainVM
    lateinit var searchPlayersVM: SearchPlayersVM

    @Inject
    lateinit var notificationVM: NotificationVM

    private lateinit var backButton: ImageView
    private lateinit var searchBar: SearchView
    private lateinit var recyclerViewPlayers: RecyclerView
    private lateinit var searchContainer: ScrollView
    private lateinit var loadingContainer: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_players)
        setSupportActionBar()
        findViews()

        searchPlayersVM = ViewModelProvider(this)[SearchPlayersVM::class.java]

        val stringRes = intent.getStringExtra("jsonReservation")
        val reservation =
            Json.decodeFromString(MatchWithCourtAndEquipments.serializer(), stringRes!!)


        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(p0: String?): Boolean {
                // filter visible content
                searchPlayersVM.filterPlayers(p0)
                return false
            }

            override fun onQueryTextSubmit(p0: String?): Boolean {
                // Do nothing on submit
                searchBar.clearFocus()
                return false
            }
        })


        recyclerViewPlayers.layoutManager = LinearLayoutManager(this)
        val adapterCard = AdapterPlayersList(emptyList(), this, mainVM.userId, reservation.match)
        recyclerViewPlayers.adapter = adapterCard

        searchPlayersVM.allPlayers.observe(this) {
            adapterCard.setPlayers(it.filter { player ->
                !reservation.match.listOfPlayers.contains(
                    player.userId
                )
            })
        }

        searchPlayersVM.error.observe(this){
            if(it != null){
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        searchPlayersVM.invitationSuccess.observe(this){
            if(it){
                Toast.makeText(this, "Player invited successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        searchPlayersVM.loadingState.observe(this){
            setLoadingScreen(it)
        }

    }

    private fun findViews(){
        searchBar = findViewById(R.id.search_view)
        recyclerViewPlayers = findViewById<RecyclerView>(R.id.recyclerViewPlayers)
        searchContainer = findViewById(R.id.search_container)
        loadingContainer = findViewById(R.id.loading_search)
    }
    private fun setLoadingScreen(state: Boolean) {
        if(state) { //is Loading
            searchContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }else{ // is not loading
            loadingContainer.visibility = View.GONE
            searchContainer.visibility = View.VISIBLE
        }
    }

    private fun setSupportActionBar() {
        supportActionBar?.setCustomView(R.layout.toolbar)
        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.setText(R.string.search_players_title)

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onClickInvite(
        sender: String,
        recipient: User,
        match: Match,
        callback: (it: Boolean) -> Unit
    ) {
       searchPlayersVM.sendInvitation(sender, recipient, match)
        callback(true)
    }
}

class PlayerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val fullName: TextView = v.findViewById(R.id.sender_name)
    val nickname: TextView = v.findViewById(R.id.nick)
    val profileImage: ImageView = v.findViewById(R.id.profile_image)
    val shimmer: ShimmerFrameLayout = v.findViewById(R.id.shimmer_layout)
    val inviteButton: Button = v.findViewById(R.id.invite_button)
}

class AdapterPlayersList(
    private var list: List<User>,
    private val listener: OnClickListener,
    private val userId: String,
    private val match: Match
) : RecyclerView.Adapter<PlayerViewHolder>() {

    interface OnClickListener {
        fun onClickInvite(
            sender: String,
            recipient: User,
            match: Match,
            callback: (it: Boolean) -> Unit
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_card, parent, false)
        return PlayerViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.fullName.text = list[position].full_name
        holder.nickname.text = list[position].nickname
        if (list[position].image != "") {
            Picasso.get().load(list[position].image).into(holder.profileImage, object :
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
                Json.encodeToString(User.serializer(), list[holder.absoluteAdapterPosition])
            playerIntent.putExtra("playerString", playerString)
            holder.itemView.context.startActivity(playerIntent)
        }

        holder.inviteButton.setOnClickListener {
            listener.onClickInvite(
                sender = userId,
                recipient = list[holder.absoluteAdapterPosition],
                match = match
            ) {
                if (it) {
                    holder.inviteButton.isClickable = false
                    holder.inviteButton.setText(R.string.invited)
                    holder.inviteButton.alpha = 0.5f
                }
            }
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    fun setPlayers(newPlayers: List<User>) {

        val diffs = DiffUtil.calculateDiff(
            UserDiffCallback(list, newPlayers)
        )
        list = newPlayers
        diffs.dispatchUpdatesTo(this)
    }

}

class UserDiffCallback(
    private val players: List<User>,
    private val newPlayers: List<User>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = players.size

    override fun getNewListSize(): Int = newPlayers.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return players[oldItemPosition].email == newPlayers[newItemPosition].email
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return players[oldItemPosition].image == newPlayers[newItemPosition].image &&
                players[oldItemPosition].full_name == newPlayers[newItemPosition].full_name
    }

}