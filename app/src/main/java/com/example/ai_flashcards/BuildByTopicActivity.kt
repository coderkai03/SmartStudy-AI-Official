package com.example.ai_flashcards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch

class BuildByTopicActivity : AppCompatActivity() {
    private val openaiApiKey = "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"
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var inputTopic: EditText
    private lateinit var loading: TextView
    lateinit var topicCV: CardView
    lateinit var term_screen: Button
    lateinit var study_screen: Button
    lateinit var uname_text: TextView

    lateinit var auth: FirebaseAuth
    lateinit var fs: FirebaseFirestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_build_by_topic)

        fs = Firebase.firestore

        //sharedPreferences
        sharedPrefs = this.getSharedPreferences(this.packageName, Context.MODE_PRIVATE)

        inputTopic = findViewById<EditText>(R.id.input_topic)
        uname_text = findViewById(R.id.uname)
        term_screen = findViewById(R.id.by_term)
        study_screen = findViewById(R.id.buildByTopic)
        topicCV = findViewById(R.id.topic_buttonCV)

        auth = FirebaseAuth.getInstance()

        val currUser = auth.currentUser // fix user ID, wrong id?
        if (currUser != null){
            val currUid = currUser.uid
            fs.collection("users")
                .document(currUid)
                .get()
                .addOnSuccessListener { document ->
//                    Log.d("SEARCH USER", "${document.id} => ${currUid}")
                    Log.d("USER", "${document.id} => ${document.getString("username")}")
                    if (document.exists()) {
                        val name = document.getString("username")
                        uname_text.setText(name)
                        Log.d("USER", "${document.id} => ${name}")
                    }
                    else {
                        Log.d("USER", "doesnt exist")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("USER", "Error getting documents.", exception)
                }
        }


        //uname_text.setText(auth.currentUser?.email)

        loading = findViewById(R.id.loading_topic)
        loading.visibility = View.INVISIBLE


        // Add_button add clicklistener
        term_screen.setOnClickListener {
            val intent = Intent(this, BuildByTerm::class.java)
            startActivity(intent)
        }

        // Add_button add clicklistener
        study_screen.setOnClickListener {
            loading.visibility = View.VISIBLE

            val disable = getColor(R.color.disabledClay)
            val colorStateList = ColorStateList.valueOf(disable)
            study_screen.isEnabled = false
            study_screen.backgroundTintList = colorStateList
            topicCV.backgroundTintList = colorStateList

            val intent = Intent(this, StudyActivity::class.java)
            val apiTask = ApiTask(object : OnApiDataReceivedListener {
                override fun onApiDataReceived(data: String) {
                    // This callback is triggered when JSON data is received
                    Log.d("ByTopic", "Received API data: $data")

                    // Process the data or update shared preferences as needed
                    val spsize = sharedPrefs.all.size
                    var cardIndex = if (sharedPrefs.contains("input_topic")) spsize else spsize + 1

                    val content = parseJsonResponse(data)

                    with(sharedPrefs.edit()) {
                        for (i in 0 until content.size){
                            putString("card$cardIndex", content[i])
                            cardIndex+=1
                        }
                        apply()
                    }

                    val spterm = sharedPrefs.getString("card$cardIndex", "not found")
                    val term = if (spterm == null) "not found" else spterm
                    Log.d("ByTopic", "SHAREDPREFS: ${sharedPrefs.all}")


                    Log.d("ByTopic","Building...")

                    // start the activity connect to the specified class
                    startActivity(intent)
                }
            })

            apiTask.execute()


        }
    }

    private fun parseJsonResponse(data: String): List<String> {
        val resultList = mutableListOf<String>()

        try {
            val jsonObject = JSONObject(data)
            val choicesArray = jsonObject.getJSONArray("choices")

            if (choicesArray.length() > 0) {
                val firstChoice = choicesArray.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                val content = message.getString("content")

                // Split content into lines
                var lines = content.split("|").drop(1)

                // Process each line and extract term and definition
                var term: String? = null
                for (line in lines) {
                    if (term != null) {
                        val definition = line.trim()
                        resultList.add("$term|$definition")
                        term = null
                    } else {
                        term = line.trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ByTopic", "Error parsing JSON: ${e.message}")
        }

        return resultList
    }


    private inner class ApiTask(private val onDataReceivedListener: OnApiDataReceivedListener) : AsyncTask<Void, Void, String>() {
        private lateinit var countDownLatch: CountDownLatch

        override fun doInBackground(vararg params: Void?): String {
            return makeApiCall()
        }

        override fun onPostExecute(result: String) {
            // Handle the API response here
            Log.d("API_RESPONSE", result)
            onDataReceivedListener.onApiDataReceived(result)
            //responseTextView.text = result

        }

        private fun makeApiCall(): String {
            val apiUrl = "https://api.openai.com/v1/chat/completions"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $openaiApiKey")
                connection.doOutput = true

                val data = """
                    {
                        "model": "gpt-3.5-turbo",
                        "messages": [
                            {
                                "role": "user",
                                "content": "Generate 10 important terms and definitions corresponding to ${inputTopic.text.toString()}. Limit 1 sentence per definition. Use format: |term|definition|. Concatenate entire output into one paragraph."
                            }
                        ]
                    }
                """.trimIndent()

                Log.d("ByTopic", data)

                val outputStream = DataOutputStream(connection.outputStream)
                outputStream.writeBytes(data)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    return response.toString()
                } else {
                    return "Error: $responseCode"
                }
            } finally {
                Log.d("ByTopic", "API")
                connection.disconnect()
            }
        }
    }
}
