package com.example.mc_project_1

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class SQLiteHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    private fun createTable(db: SQLiteDatabase) {
        Log.d("Create table", "Executing create table")
        val createTable = "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$HEART_RATE_COL REAL, " +
                "$RESPIRATORY_RATE_COL REAL, " +
                "$NAUSEA_COL REAL, " +
                "$HEADACHE_COL REAL, " +
                "$DIARRHEA_COL REAL, " +
                "$THROAT_COL REAL, " +
                "$FEVER_COL REAL, " +
                "$MUSCLE_ACHE_COL REAL, " +
                "$SMELL_TASTE_COL REAL, " +
                "$COUGH_COL REAL, " +
                "$BREATH_COL REAL, " +
                "$TIRED_COL REAL)"
        db.execSQL(createTable)
    }

    fun addSign(dbModel: DBModel) {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(HEART_RATE_COL, dbModel.HEART_RATE)
        values.put(RESPIRATORY_RATE_COL, dbModel.RESPIRATORY_RATE)

        // Check if the table exists, and if not, create it
        if (!isTableExists(db, TABLE_NAME)) {
            createTable(db)
        }

        db.insert(TABLE_NAME, null, values)
    }

    fun updateLastSign(key : String, value : Float?) {
        val db = this.writableDatabase
        val values = ContentValues()

        // Set the values to be updated
        values.put(key, value)
//        values.put(RESPIRATORY_RATE_COL, dbModel.RESPIRATORY_RATE)

        Log.d("Update values", values.toString())
        // Define the WHERE clause to update the last row
        val whereClause = "$ID = (SELECT MAX($ID) FROM $TABLE_NAME)"

        // Check if the table exists, and if not, create it
        if (!isTableExists(db, TABLE_NAME)) {
            createTable(db)
        }

        // Perform the update operation
        db.update(TABLE_NAME, values, whereClause, null)
    }

    // This method is for adding data in our database
    fun addSymptom(dbModel: DBModel) {
        val db = this.writableDatabase
        val values = ContentValues()

        // Assuming dbModel has properties like NAUSEA, HEADACHE, etc.
        values.put(NAUSEA_COL, dbModel.NAUSEA)
        values.put(HEADACHE_COL, dbModel.HEADACHE)
        values.put(DIARRHEA_COL, dbModel.DIARRHEA)
        values.put(THROAT_COL, dbModel.SORE_THROAT)
        values.put(FEVER_COL, dbModel.FEVER)
        values.put(MUSCLE_ACHE_COL, dbModel.MUSCLE_PAIN)
        values.put(SMELL_TASTE_COL, dbModel.SMELL_TASTE)
        values.put(COUGH_COL, dbModel.COUGH)
        values.put(BREATH_COL, dbModel.SHORTNESS_BREATH)
        values.put(TIRED_COL, dbModel.TIRED)

        // Check if the table exists, and if not, create it
        Log.d("Table exists", isTableExists(db, TABLE_NAME).toString())
        if (!isTableExists(db, TABLE_NAME)) {
            createTable(db)
        }

        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $ID DESC LIMIT 1", null)
        val rowExists = cursor.moveToFirst()

        if(rowExists) {
            val id = cursor.getInt(0)
            db.update(TABLE_NAME, values, "$ID = ?", arrayOf(id.toString()))
        }
        else {
            db.insert(TABLE_NAME, null, values)
        }
//        db.close()
    }

    private fun isTableExists(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName))
        val tableExists = cursor.moveToFirst()
        cursor.close()
        return tableExists
    }

    fun getAllSymptoms(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    fun getLastSymptom(): Cursor? {
        val db = this.readableDatabase
        if (!isTableExists(db, TABLE_NAME)) {
            createTable(db)
        }

        val query = "SELECT * FROM $TABLE_NAME ORDER BY $ID DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        if (cursor != null && cursor.moveToFirst()) {
            // Iterate through all columns and print their values
            val columnCount = cursor.columnCount
            for (i in 0 until columnCount) {
                val columnName = cursor.getColumnName(i)
                val columnValue = cursor.getString(i) // Change this to the appropriate type if needed
                println("$columnName: $columnValue")
            }
        }

        return cursor
    }

    // below method is to get
    // all data from our database
    fun getName(): Cursor? {

        // here we are creating a readable
        // variable of our database
        // as we want to read value from it
        val db = this.readableDatabase

        // below code returns a cursor to
        // read data from the database
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)

    }

    fun deleteTable(tableName: String) {
        val db = this.readableDatabase
        db.execSQL("DELETE FROM $tableName")
    }

    companion object{
        // here we have defined variables for our database

        // below is variable for database name
        private const val DATABASE_NAME = "MOBILE_COMPUTING"

        // below is the variable for database version
        private val DATABASE_VERSION = 2


        const val TABLE_NAME = "SYMPTOMS_TABLE"

        const val ID = "ID"
        const val HEART_RATE_COL = "HEART_RATE"
        const val RESPIRATORY_RATE_COL = "RESPIRATORY_RATE"
        const val NAUSEA_COL = "NAUSEA"
        const val HEADACHE_COL = "HEADACHE"
        const val DIARRHEA_COL = "DIARRHEA"
        const val THROAT_COL = "SORE_THROAT"
        const val FEVER_COL = "FEVER"
        const val MUSCLE_ACHE_COL = "MUSCLE_ACHE"
        const val SMELL_TASTE_COL = "LOSS_OF_SMELL_OR_TASTE"
        const val COUGH_COL = "COUGH"
        const val BREATH_COL = "SHORTNESS_OF_BREATH"
        const val TIRED_COL = "FEELING_TIRED"
    }
}