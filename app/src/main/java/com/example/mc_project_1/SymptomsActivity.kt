package com.example.mc_project_1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RatingBar
import android.widget.Spinner
import androidx.activity.ComponentActivity
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class SymptomsActivity : AppCompatActivity() {

    var spinnerSymptom: Spinner? = null
    var ratingBar: RatingBar? = null
    var btnUpload: Button? = null
    var btnSubmit:android.widget.Button? = null
    var spinnerSelection = ""
    var selectedRating = 0f
    var user_name = ""
    var symptomsArray = arrayOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MobileComputing)
        setContentView(R.layout.activity_symptoms)

        val backBtn = findViewById<Button>(R.id.button2)

        // Find views
        val ratingBar = findViewById<RatingBar>(R.id.ratingSymptom)
        Log.d("RatingBar", ratingBar.toString())
        val spinnerSymptom = findViewById<Spinner>(R.id.symptom_spinner)

        val dbHelper = SQLiteHelper(this, null)

//        dbHelper.deleteTable("symptoms_table")

        if (spinnerSymptom != null) {
            // Set up ArrayAdapter
            val symptomsArray = resources.getStringArray(R.array.list_of_symptoms)
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, symptomsArray)
            spinnerSymptom.adapter = spinnerAdapter

            spinnerSymptom.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {
                    spinnerSelection = symptomsArray[position]
//                    Toast.makeText(this@SymptomsActivity,
//                        getString(R.string.selected_item) + " " +
//                                "" + symptomsArray[position], Toast.LENGTH_SHORT).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }

        ratingBar?.setOnRatingBarChangeListener { _, _, _ ->
            // Update selectedRating when the rating changes
            selectedRating = ratingBar.rating;
            Log.d("SelectedRating", selectedRating.toString())
        };
        uploadSymptoms(dbHelper)
        // Set an OnClickListener for the button
        backBtn.setOnClickListener {
            // Create an Intent to navigate to the SymptomsActivity
            val intent = Intent(this@SymptomsActivity, MainActivity::class.java)

            // Start the new activity
            startActivity(intent)
        }
    }

    @SuppressLint("Range")
    private fun uploadSymptoms(dbHelper: SQLiteHelper) {
        btnUpload = findViewById(R.id.btnUpload)
        btnUpload?.setOnClickListener {
            // Get the selected symptom and its rating
            val selectedSymptom = spinnerSelection
            Log.d("SelectedSymptom", selectedSymptom)
            val rating = selectedRating;
            Log.d("Rating", rating.toString())

            val dbModel = DBModel()

            val cursor = dbHelper.getLastSymptom()

                if (cursor != null && cursor.moveToFirst()) {
                    val heartRateIndex = cursor.getColumnIndex(SQLiteHelper.HEART_RATE_COL)
                    Log.d("Heart Rate index", heartRateIndex.toString())
                    dbModel.HEART_RATE = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.HEART_RATE_COL))
                    dbModel.RESPIRATORY_RATE = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.RESPIRATORY_RATE_COL))
                    dbModel.NAUSEA = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.NAUSEA_COL))
                    dbModel.HEADACHE = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.HEADACHE_COL))
                    dbModel.DIARRHEA = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.DIARRHEA_COL))
                    dbModel.SORE_THROAT = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.THROAT_COL))
                    dbModel.FEVER = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.FEVER_COL))
                    dbModel.MUSCLE_PAIN = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.MUSCLE_ACHE_COL))
                    dbModel.SMELL_TASTE = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.SMELL_TASTE_COL))
                    dbModel.COUGH = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.COUGH_COL))
                    dbModel.SHORTNESS_BREATH = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.BREATH_COL))
                    dbModel.TIRED = cursor.getFloat(cursor.getColumnIndex(SQLiteHelper.TIRED_COL))

                    cursor.close()
                } else {
                    cursor?.close()
                }

            when (selectedSymptom) {
                "Nausea" -> dbModel.NAUSEA = rating
                "Headache" -> dbModel.HEADACHE = rating
                "Diarrhea" -> dbModel.DIARRHEA = rating
                "Sore Throat" -> dbModel.SORE_THROAT = rating
                "Fever" -> dbModel.FEVER = rating
                "Muscle Ache" -> dbModel.MUSCLE_PAIN = rating
                "Loss of Smell or Taste" -> dbModel.SMELL_TASTE = rating
                "Cough" -> dbModel.COUGH = rating
                "Shortness of Breath" -> dbModel.SHORTNESS_BREATH = rating
                "Feeling Tired" -> dbModel.TIRED = rating
            }



            // Add the selected symptom and its rating to the database
            if (rating != null) {
//                dbHelper.deleteTable("symptoms_table")
                dbHelper.addSymptom(dbModel)
                // Remove "selectedRating" from the updatedArray
//                val updatedArray = resources.getStringArray(R.array.list_of_symptoms).toMutableList().filterNot { it == "Nausea" }.toTypedArray()
//                Log.d("UpdatedArray", updatedArray.joinToString(", "))
//                dbHelper.addAllOtherSymptoms(updatedArray, 0.0)
            }

            // Display a message to indicate successful upload
            Toast.makeText(this, "Symptom Uploaded: $selectedSymptom, Rating: $rating", Toast.LENGTH_SHORT).show()
        }
    }
}