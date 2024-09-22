package com.example.lab2.profile.edit_profile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cunoraz.tagview.*
import com.example.lab2.R
import com.example.lab2.entities.*
import com.example.lab2.view_models.MainVM
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.io.*
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var vm: MainVM

    private lateinit var editedUser: User
    private var cameraHasFinished: Boolean = true

    private val listAllInterests: List<Sport> = Sport.values().toList()

    private lateinit var profileImage: ImageView
    private lateinit var cameraImageButton: ImageButton
    private lateinit var full_name_m: EditText
    private lateinit var nickname_m: EditText
    private lateinit var description_m: EditText
    private lateinit var address_m: EditText
    //private lateinit var email_m: EditText
    private lateinit var birthday_m: EditText
    private lateinit var skills_m: MutableMap<BadgeType, Int>
    private lateinit var tagGroup: TagView
    private lateinit var confirmButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var contentEdit: ScrollView
    private lateinit var loadingEdit: ProgressBar

    var image_uri: Uri? = null
    var file_name: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        setSupportActionBar()
        findViews()

        // Get the user information sended by the showProfile Activity,
        // than update the content of the views
        val extras = intent.extras
        if (extras != null) {
            editedUser = User.fromJson(extras.getString("user")!!)
        }

        initDatePicker()
        updateContent()

        val adapterCard = EditSkillsAdapter(skills_m)
        val listReservationsRecyclerView =
            findViewById<RecyclerView>(R.id.edit_your_skills_recycler_view)
        listReservationsRecyclerView.adapter = adapterCard
        listReservationsRecyclerView.layoutManager = LinearLayoutManager(this)


        var valid = true

        //TODO Validation
        confirmButton.setOnClickListener {
            if (full_name_m.text.toString().trim() == "" ||
                nickname_m.text.toString().trim() == "" ||
                address_m.text.toString().trim() == "" ||
                //email_m.text.toString().trim() == "" ||
                birthday_m.text.toString().trim() == ""
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                valid = false
            } /* else if (!isValidEmail(email_m.text.trim())) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                valid = false
            } */ else if (nickname_m.text.contains(" ")) {
                Toast.makeText(this, "Please enter a valid nickname", Toast.LENGTH_SHORT).show()
                valid = false
            } else if (description_m.text.length > 150) {
                Toast.makeText(
                    this,
                    "The description must have at most 150 characters",
                    Toast.LENGTH_SHORT
                ).show()
                valid = false
            } else {
                valid = true
            }

            if (valid) saveData()
        }

        setupTags()
        tagGroup.setOnTagClickListener { tag, _ ->
            var uppercaseTagName =
                tag.text.uppercase(Locale.getDefault()) // i.e. from "Football" to "FOOTBALL", which is the constant in the enum
            if (editedUser.interests.any { it.name == uppercaseTagName }) {
                editedUser.interests =
                    editedUser.interests.filterNot { it.name == uppercaseTagName }.toMutableList()
                setupTags()
            } else {
                if (editedUser.interests.size < 3) {
                    editedUser.interests.add(Sport.valueOf(uppercaseTagName))

                    setupTags()
                } else {
                    Toast.makeText(this, "You can add at most 3 interests", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    //TODO This function fills the TagView with all the interests:
    // - The interests of the user are green
    // - The other interests are white
    // This method must be called everytime we make a change in our interests to update the colors.
    private fun setupTags() {
        // We want to make a union between the interests of the user and all the other interests
        tagGroup.addTags(listAllInterests.union(editedUser.interests).map { currentInterest ->
            /*
            * The enum constants are in uppercase, like "FOOTBALL". We want a tag like "Football".
            * The following code is equivalent to inputString.toLowerCase().capitalize(),
            * however these functions are deprecated. The IDE suggested the code below instead.
            * */

            var tag = Tag(currentInterest.name.lowercase(Locale.getDefault())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
            tag.tagTextSize = 18F

            // If the list of the user's interests contains the current interest, it should be green
            if (editedUser.interests.contains(currentInterest)) {
                tag.layoutColor = ContextCompat.getColor(this, R.color.example_1_bg)
                tag.tagTextColor = Color.WHITE
            } else {
                // otherwise it is white
                tag.layoutColor = Color.WHITE
                tag.tagTextColor = Color.BLACK
                tag.layoutBorderColor = Color.BLACK
                tag.layoutBorderSize = 1F
            }
            tag
        })
    }

    private fun setSupportActionBar() {

        supportActionBar?.elevation = 0f
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.setCustomView(R.layout.toolbar_edit_profile)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title_edit_profile)
        titleTextView?.setText(R.string.edit_profile_title)
        backButton =
            supportActionBar?.customView?.findViewById<ImageButton>(R.id.edit_profile_back_button)!!

        backButton.setOnClickListener {
            finish()
        }

    }

    //TODO A function that calls all the findViewById() methods for each UI element
    private fun findViews() {
        full_name_m = findViewById(R.id.editNameSurname)
        description_m = findViewById(R.id.editDescription)
        nickname_m = findViewById(R.id.editNickname)
        address_m = findViewById(R.id.editLocation)
        //email_m = findViewById(R.id.editEmail)
        birthday_m = findViewById(R.id.editBod)
        profileImage = findViewById(R.id.profile_image)
        cameraImageButton = findViewById(R.id.edit_picture)
        confirmButton = findViewById(R.id.confirm_button)
        tagGroup = findViewById(R.id.tag_group)
        contentEdit = findViewById(R.id.contentEdit)
        loadingEdit = findViewById(R.id.loadingEdit)

        cameraImageButton.setOnClickListener { popupMenuSetup() }
    }

    //TODO This is the popup menu that is displayed as we click on the ImageButton overlapped to the profile image.
    // It contains two options: "Camera" or "Library".
    // - If we press on the first option, the app checks and asks the permission to open the camera
    // and then the user is ready to take a photo.
    // - If we press on the second option, the app opens the photo library and the user can pick one photo.
    private fun popupMenuSetup() {
        val popupMenu = PopupMenu(this, cameraImageButton)
        popupMenu.inflate(R.menu.popup_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.take_photo -> {
                    checkPermissionsAndOpenCamera()
                    true
                }

                R.id.choose_photo_from_gallery -> {
                    openPhotoLibrary()
                    true
                }

                else -> true
            }
        }
        try {
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(menu, true)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            popupMenu.show()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        full_name_m.setText(savedInstanceState.getString("full_name"))
        nickname_m.setText(savedInstanceState.getString("nickname"))
        description_m.setText(savedInstanceState.getString("description"))
        address_m.setText(savedInstanceState.getString("address"))
        //email_m.setText(savedInstanceState.getString("email"))
        birthday_m.setText(savedInstanceState.getString("birthday"))
        skills_m = Gson().fromJson(
            savedInstanceState.getString("skills"),
            MutableMap::class.java
        ) as MutableMap<BadgeType, Int>

        // Open and load the photo
        file_name = savedInstanceState.getString("image")
        if (file_name != "") {
            val inputStream = applicationContext.openFileInput(file_name)
            val rotated = BitmapFactory.decodeStream(inputStream)
            profileImage.setImageBitmap(rotated)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("full_name", full_name_m.text.toString())
        outState.putString("nickname", nickname_m.text.toString())
        outState.putString("description", description_m.text.toString())
        outState.putString("address", address_m.text.toString())
        //outState.putString("email", email_m.text.toString())
        outState.putString("image", file_name ?: "")
        outState.putString("birthday", birthday_m.text.toString())

        /*
        * We are using the Gson library to serialize a user's interests object into a JSON string,
        * and then storing it in a Bundle as a string with the key "interests".
        * */

        val gsonInterests = Gson()
        val jsonInterests = gsonInterests.toJson(editedUser.interests)
        outState.putString("interests", jsonInterests)

        val gsonSkills = Gson()
        val jsonSkills = gsonSkills.toJson(skills_m)
        outState.putString("skills", jsonSkills)
    }


    //TODO This function fills the fields
    private fun updateContent() {
        full_name_m.setText(editedUser.full_name)
        nickname_m.setText(editedUser.nickname)
        description_m.setText(editedUser.description)
        address_m.setText(editedUser.address)
        //email_m.setText(editedUser.email)
        skills_m = editedUser.badges as MutableMap<BadgeType, Int>

        if (editedUser.image == "") {
            profileImage.setBackgroundResource(R.drawable.profile_picture)
        } else {
            val profileImageUrl = editedUser.image
            Picasso.get().load(profileImageUrl).into(profileImage)
        }

    }

    //TODO capture the image using camera and display it
    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
                if (it.resultCode === RESULT_OK) {
                    val inputImage = uriToBitmap(image_uri!!)
                    val rotated = rotateBitmap(inputImage!!)
                    rotated?.let {
                        // Set the new image to the ImageView
                        profileImage.setImageBitmap(it)
                        file_name = "image.jpg"
                        // Create an outputStream to write the image data to a file in internal storage.
                        // The first parameter is the name of the file, and the second parameter is the
                        // file mode which determines the access level of the file. We use MODE_PRIVATE
                        // to make the file accessible only to our app.
                        // This operation is time-consuming, so we define a thread.
                        thread {
                            runOnUiThread {
                                setLoading(true)
                            }
                            val outputStream =
                                applicationContext.openFileOutput(file_name, Context.MODE_PRIVATE)
                            // We compress the bitmap data to PNG format and write it to the outputStream
                            it.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            outputStream.flush()
                            outputStream.close()
                            runOnUiThread {
                                setLoading(false)
                            }
                        }
                    }
                }
            }
        )

    private var galleryActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
                if (it.resultCode === RESULT_OK) {
                    image_uri = it.data?.data
                    val inputImage = uriToBitmap(image_uri!!)
                    val rotated = rotateBitmap(inputImage!!)
                    rotated?.let {
                        // Set the new image to the ImageView
                        profileImage.setImageBitmap(it)
                        file_name = "image.jpg"
                        // Create an outputStream to write the image data to a file in internal storage.
                        // The first parameter is the name of the file, and the second parameter is the
                        // file mode which determines the access level of the file. We use MODE_PRIVATE
                        // to make the file accessible only to our app.
                        // This operation is time-consuming, so we define a thread.
                        thread {
                            runOnUiThread {
                                setLoading(true)
                            }
                            val outputStream =
                                applicationContext.openFileOutput(file_name, Context.MODE_PRIVATE)
                            // We compress the bitmap data to PNG format and write it to the outputStream
                            it.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            outputStream.flush()
                            outputStream.close()
                            runOnUiThread {
                                setLoading(false)
                            }
                        }
                    }
                }
            }
        )

    //TODO takes URI of the image and returns bitmap
    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    //TODO Most phone cameras are landscape, meaning if you take the photo in portrait,
    // the resulting photos will be rotated 90 degrees. So we need a function to get the orientation
    // of the photo and to rotate it accordingly.
    @SuppressLint("Range")
    fun rotateBitmap(input: Bitmap): Bitmap? {
        val orientationColumn =
            arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur: Cursor? = contentResolver.query(image_uri!!, orientationColumn, null, null, null)
        var orientation = -1
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]))
        }
        Log.d("tryOrientation", orientation.toString() + "")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    private fun checkPermissionsAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_DENIED
            ) {
                val permission = arrayOf<String>(
                    android.Manifest.permission.CAMERA
                )
                requestPermissions(permission, 112)
            } else {
                openCamera()
            }
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }

    private fun openPhotoLibrary() {
        val galleryIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryActivityResultLauncher.launch(galleryIntent)
    }

    private fun saveData() {
        val result = Intent()

        val editedUser = User(
            userId = vm.user.value?.userId!!,
            full_name = full_name_m.text.toString().trim(),
            nickname = nickname_m.text.toString().trim(),
            address = address_m.text.toString().trim(),
            description = description_m.text.toString().trim(),
            email = vm.user.value?.email!!,
            image = editedUser.image,
            birthday = editedUser.birthday,
            interests = editedUser.interests,
            badges = skills_m,
            score = vm.user.value?.score ?: mutableMapOf()
        )

        vm.updateUser(editedUser)

        if (image_uri != null) {
            setLoading(true)
            vm.updateUserImage(image_uri!!){
                setLoading(false)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        } else {
            setResult(Activity.RESULT_OK, result)
            finish()
        }


    }

    //TODO This function initializes the DatePickerDialog
    private fun initDatePicker() {
        birthday_m.setText("${editedUser.birthday.dayOfMonth}/${editedUser.birthday.monthValue}/${editedUser.birthday.year}")
        birthday_m.setOnClickListener {
            val c = editedUser.birthday
            val uYear = c.year
            val uMonth = c.monthValue
            val uDay = c.dayOfMonth

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    val dat = (dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)
                    birthday_m.setText(dat)
                    editedUser.birthday = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                },
                uYear,
                uMonth,
                uDay
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun setLoading(value : Boolean) {
        if(value && contentEdit.visibility == View.VISIBLE) {
            contentEdit.visibility = View.INVISIBLE
            loadingEdit.visibility = View.VISIBLE
        } else if (!value) {
            contentEdit.visibility = View.VISIBLE
            loadingEdit.visibility = View.GONE
        }
    }
}

class EditSkillViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val skillImage: ImageView = v.findViewById(R.id.edit_skill_image)
    val skillName: TextView = v.findViewById(R.id.edit_skill_label)
    val skillRating: RatingBar = v.findViewById(R.id.edit_skill_rating)
}

class EditSkillsAdapter(private var list: MutableMap<BadgeType, Int>) :
    RecyclerView.Adapter<EditSkillViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditSkillViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.edit_skill_card, parent, false)
        return EditSkillViewHolder(v)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: EditSkillViewHolder, position: Int) {

        val key = list.toList()[position].first

        when (key) {
            BadgeType.SPEED -> {
                holder.skillImage.setImageResource(R.drawable.badge_speed)
                holder.skillName.text = "Speed"
            }

            BadgeType.PRECISION -> {
                holder.skillImage.setImageResource(R.drawable.badge_precision)
                holder.skillName.text = "Precision"
            }

            BadgeType.TEAM_WORK -> {
                holder.skillImage.setImageResource(R.drawable.badge_team)
                holder.skillName.text = "Team Work"
            }

            BadgeType.STRATEGY -> {
                holder.skillImage.setImageResource(R.drawable.badge_strategy)
                holder.skillName.text = "Strategy"
            }

            BadgeType.ENDURANCE -> {
                holder.skillImage.setImageResource(R.drawable.badge_endurance)
                holder.skillName.text = "Endurance"
            }
        }
        holder.skillRating.stepSize = 1f
        holder.skillRating.rating = list[key]!!.toFloat()

        holder.skillRating.setOnRatingBarChangeListener { _, value, _ -> list[key] = value.toInt() }
    }

}