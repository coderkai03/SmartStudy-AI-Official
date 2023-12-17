package com.example.ai_flashcards

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlin.math.log
import kotlin.random.Random

class StudyActivity : AppCompatActivity() {

    //RecyclerView + flashcard list
    lateinit var rv_flashcards: RecyclerView
    val flashcardList = mutableListOf<Flashcard>()

    //xml
    lateinit var uname_text: TextView

    //shared prefs
    private lateinit var sharedPrefs: SharedPreferences //delete

    //firebase
    lateinit var auth: FirebaseAuth
    lateinit var fs: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study)

        //set Flashcard hideEdits to true
        Flashcard.hideEdits = true

        //shared prefs
        sharedPrefs = this.getSharedPreferences(this.packageName, Context.MODE_PRIVATE)
        Log.d("StudyActivity", sharedPrefs.all.toString())

        //set up recyclerview
        setRVFlashcards()

        // Buttons to vals
        val shuffle = findViewById<Button>(R.id.shuffle)
        val edit = findViewById<Button>(R.id.edit)
        val add = findViewById<Button>(R.id.add)
        val home = findViewById<Button>(R.id.home)
        val reset = findViewById<Button>(R.id.reset)
        val resetCV = findViewById<CardView>(R.id.reset_cv)
        uname_text = findViewById(R.id.uname)

        // Find background icons
        val shuffle_icon = resources.getDrawable(R.drawable.shuffle_icon)
        val edit_icon = resources.getDrawable(R.drawable.edit_icon)
        val add_icon = resources.getDrawable(R.drawable.add_icon)
        val home_icon = resources.getDrawable(R.drawable.home_icon)

        //firebase setup
        fs = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        var currUser = auth.currentUser

        //get username
        if (currUser != null) {
            val currID = currUser.uid
            fs.collection("users")
                .document(currID)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val uname = doc.getString("username")
                        uname_text.setText(uname)
                    }
                }
        }


        // Set icons
        shuffle.background = shuffle_icon
        edit.background = edit_icon
        add.background = add_icon
        home.background = home_icon
        resetCV.visibility = View.INVISIBLE

        // Set button onClicks (shuffle/delete are unique)
        setResetButton(reset)
        shuffleCards(shuffle)
        setShowHideEdit(edit, resetCV)
        setScreenButtons(add, BuildByTopicActivity::class.java)
        setScreenButtons(home, MainActivity::class.java)
    }

    private fun setResetButton(reset: Button) {
        reset.setOnClickListener {
            flashcardList.clear()
            rv_flashcards.adapter?.notifyDataSetChanged()

            sharedPrefs.edit().clear()
        }
    }

    private fun setShowHideEdit(button: Button, reset: CardView) {
        button.setOnClickListener{
            Flashcard.showHideEdits()
            rv_flashcards.adapter?.notifyDataSetChanged()

            reset.visibility = if (reset.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun shuffleCards(button: Button) {

        button.setOnClickListener{
            val random = java.util.Random()

            //shuffle cards list
            for (i in 0 until flashcardList.size) {
                val j = random.nextInt(i+1)

                //swap elements
                val temp = flashcardList[i]
                flashcardList[i] = flashcardList[j]
                flashcardList[j] = temp
            }

            rv_flashcards.adapter?.notifyDataSetChanged()
        }
    }

    override fun onResume() { //is onResume necessary?
        super.onResume()

//        if (sharedPrefs.all.size != 0) {
//            Log.d("StudyActivity", "found existing cards")
//            loadFlashcards()
//        }


    }

    private fun setRVFlashcards() {
        // Initialize UI elements after setContentView
        rv_flashcards = findViewById(R.id.flashcardRecyclerView)

        // Initialize the RecyclerView and FlashcardList
        rv_flashcards.adapter = FlashcardAdapter(flashcardList)
        rv_flashcards.layoutManager = LinearLayoutManager(this@StudyActivity)
    }


    private fun loadFlashcards() {

        for ((key, value) in sharedPrefs.all.entries) {

            if (key != "input_topic" && key != "edit") {
                val value = sharedPrefs.getString(key, "nullVal")

                // Process key and value here
                val cardInfo = value?.toString()?.split("|")
                Log.d("StudyActivity", "loading $cardInfo")
                if (cardInfo != null && cardInfo.size == 2) {
                    Log.d("StudyActivity", "loading card!")
                    flashcardList.add(Flashcard(cardInfo[0], cardInfo[1]))
                    Log.d("StudyActivity", flashcardList.last().toString())
                }
            }
        }
        rv_flashcards.adapter?.notifyDataSetChanged()
    }

    private fun setScreenButtons(button: Button, screen: Class<*>) {
        //set icon button clicks
        button.setOnClickListener {
            val intent = Intent(this, screen)

            //only cache cards when going to add/edit
            if (screen == BuildByTopicActivity::class.java || screen == EditActivity::class.java) {
                cacheCards()
            }

            startActivity(intent)
        }
    }

    private fun cacheCards() {
        Log.d("StudyActivity", "caching cards")

        //clear sharedprefs first
        val topic = sharedPrefs.getString("input_topic", "null")
        sharedPrefs.edit().clear()

        //add topic if existed
        if (topic != "null") {
            sharedPrefs.edit().putString("input_topic", topic)
        }

        //add cards to sharedprefs
        with(sharedPrefs.edit()) {
            for (i in 0 until flashcardList.size) { //save cards to sharedPrefs

                val flashcard = arrayOf(
                    flashcardList[i].term,
                    flashcardList[i].definition

                ).joinToString("|")

                putString("card${i + 1}", flashcard)
            }

            apply()
        }
    }
}