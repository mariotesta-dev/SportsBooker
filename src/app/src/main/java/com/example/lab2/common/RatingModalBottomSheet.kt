package com.example.lab2.common

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R
import com.example.lab2.entities.*
import com.example.lab2.utils.AppPreferences
import com.example.lab2.view_models.DetailsVM
import com.example.lab2.view_models.RatingModalVM
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter


@AndroidEntryPoint
class RatingModalBottomSheet : BottomSheetDialogFragment() {


    var matchToReview : MatchToReview? = null
    var notifications : MutableLiveData<MutableList<Notification>>? = null

    lateinit var ratingModalVM: RatingModalVM
    lateinit var detailsVM: DetailsVM

    private lateinit var cleanlinessCourtRatingBar: RatingBar
    private lateinit var playingSurfaceQualityRatingBar: RatingBar
    private lateinit var lightingRatingBar: RatingBar
    private lateinit var textReview: TextInputEditText
    private lateinit var courtName: TextView
    private lateinit var courtImageView: ImageView
    private lateinit var sportLabel: TextView
    private lateinit var location: TextView
    private lateinit var dateDetail: TextView
    private lateinit var hourDetail: TextView
    private lateinit var playersRecyclerView: RecyclerView


    private lateinit var appPreferences: AppPreferences

    private fun submit() {

        val court = detailsVM.matchWithCourt.value!!.court

        val ratingParametersMap = mapOf(
            "cleanliness" to cleanlinessCourtRatingBar.rating,
            "playingSurfaceQuality" to playingSurfaceQualityRatingBar.rating,
            "lighting" to lightingRatingBar.rating
        )

        val courtReview = CourtReview(
            court.courtId,
            textReview.text.toString(),
            ratingParametersMap
        )
        ratingModalVM.submitReview(courtReview)
        ratingModalVM.incrementMVPScore(court = court)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val bundle = arguments
        val matchId = bundle?.getString("matchId")
        //ratingModalVM.getMatchToReview(matchId!!)

        val view = inflater.inflate(R.layout.rating_field_layout, container, false)

        ratingModalVM = ViewModelProvider(requireActivity())[RatingModalVM::class.java]
        detailsVM = ViewModelProvider(requireActivity())[DetailsVM::class.java]

        detailsVM.getMatchDetails(matchId!!)

        detailsVM.matchWithCourt.observe(viewLifecycleOwner) {
            updateContent(it)
        }

        cleanlinessCourtRatingBar = view.findViewById(R.id.cleanlinessCourtRatingBar)
        playingSurfaceQualityRatingBar = view.findViewById(R.id.playingSurfaceQualityRatingBar)
        lightingRatingBar = view.findViewById(R.id.lightingRatingBar)
        textReview = view.findViewById(R.id.textReview)
        sportLabel = view.findViewById(R.id.sport_label)
        location = view.findViewById(R.id.location)
        dateDetail = view.findViewById(R.id.date_detail)
        hourDetail = view.findViewById(R.id.hour_detail)
        playersRecyclerView = view.findViewById(R.id.recycler_view_mvp)

        detailsVM.listOfPlayers.observe(viewLifecycleOwner) {
            playersRecyclerView.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            playersRecyclerView.adapter = AdapterPlayersMVP(it, ratingModalVM)
        }

        courtName = view.findViewById(R.id.courtName)
        courtName.text = "Campo 1"
        courtImageView = view.findViewById(R.id.courtImageView)
        //courtImageView.setImageBitmap(ratingModalVM.getCourtToReview().value!!.courtPhoto)

        val sendReviewButton = view.findViewById<Button>(R.id.send_review)
        sendReviewButton.setOnClickListener {
            // courtId
            submit()
            dismiss()
            // Fire this function which has been set in NotificationsActivity
            ratingModalVM.deleteReviewNotification(matchToReview?.id!!, matchToReview?.match?.matchId!!, notifications!!)
            val dialog = Dialog(requireContext(), R.style.RoundedDialog)
            dialog.setContentView(R.layout.thank_you_review_layout)
            dialog.setCancelable(true)
            dialog.show()
            val handler = Handler(Looper.getMainLooper())
            // Close the "thank you" dialog after 2.5 seconds
            handler.postDelayed({
                dialog.dismiss()
            }, 2500)
        }

        return view
    }

    fun updateContent(matchWithCourt: MatchWithCourt) {
        Picasso.get().load(matchWithCourt.court.image).into(courtImageView, object : Callback {
            override fun onSuccess() {
                sportLabel.text = matchWithCourt.court.sport
                courtName.text = matchWithCourt.court.name
                location.text = "Via Giovanni Magni, 32"
                dateDetail.text = matchWithCourt.match.date.format(
                    DateTimeFormatter.ofPattern("dd-MM")
                )
                hourDetail.text =
                    matchWithCourt.match.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                //loading.visibility = View.GONE
                //details.visibility = View.VISIBLE

            }

            override fun onError(e: Exception?) {
                Toast.makeText(context, "Unable to load the details", Toast.LENGTH_SHORT)
                    .show()
                dismiss()
            }

        })
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}

class ViewHolderPlayersMVP(v: View) : RecyclerView.ViewHolder(v) {
    val playerImage: ImageView? = v.findViewById(R.id.player_image_details) ?: null
    val shimmer: ShimmerFrameLayout? = v.findViewById(R.id.shimmer_layout) ?: null
    val selectedMVPLayout: FrameLayout? = v.findViewById(R.id.selectedPlayerLayout) ?: null
}

class AdapterPlayersMVP(
    private var listOfPlayers: List<User>,
    private val ratingModalVM: RatingModalVM
) : RecyclerView.Adapter<ViewHolderPlayersMVP>() {


    private var selectedPosition : Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderPlayersMVP {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.player_details, parent, false)
        return ViewHolderPlayersMVP(v)
    }

    override fun getItemCount(): Int = listOfPlayers.size

    override fun onBindViewHolder(holder: ViewHolderPlayersMVP, position: Int) {

        //ratingModalVM.setSelectedMVP(listOfPlayers[0].userId)

        Picasso.get().load(listOfPlayers[position].image)
            .into(holder.playerImage, object : Callback {
                override fun onSuccess() {
                    holder.shimmer?.stopShimmer()
                    holder.shimmer?.hideShimmer()
                }

                override fun onError(e: Exception?) {
                }

            })

        holder.playerImage?.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(previousSelectedPosition ?: 0)
            notifyItemChanged(selectedPosition!!)
            ratingModalVM.setSelectedMVP(listOfPlayers[position].userId)
        }

        holder.selectedMVPLayout?.visibility =
            if (selectedPosition == holder.bindingAdapterPosition) View.VISIBLE else View.GONE
    }
}
