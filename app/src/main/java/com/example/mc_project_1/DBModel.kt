package com.example.mc_project_1

import android.os.Parcel
import android.os.Parcelable


data class DBModel(
    var HEART_RATE: Float? = null,
    var RESPIRATORY_RATE: Float? = null,
    var NAUSEA: Float? = 0f,
    var HEADACHE: Float? = 0f,
    var DIARRHEA: Float? = 0f,
    var SORE_THROAT: Float? = 0f,
    var FEVER: Float? = 0f,
    var MUSCLE_PAIN: Float? = 0f,
    var SMELL_TASTE: Float? = 0f,
    var COUGH: Float? = 0f,
    var SHORTNESS_BREATH: Float? = 0f,
    var TIRED: Float? = 0f,
    var SESSION_ID: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(String::class.java.classLoader) as? String
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(HEART_RATE)
        parcel.writeValue(RESPIRATORY_RATE)
        parcel.writeValue(NAUSEA)
        parcel.writeValue(HEADACHE)
        parcel.writeValue(DIARRHEA)
        parcel.writeValue(SORE_THROAT)
        parcel.writeValue(FEVER)
        parcel.writeValue(MUSCLE_PAIN)
        parcel.writeValue(SMELL_TASTE)
        parcel.writeValue(COUGH)
        parcel.writeValue(SHORTNESS_BREATH)
        parcel.writeValue(TIRED)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DBModel> {
        override fun createFromParcel(parcel: Parcel): DBModel {
            return DBModel(parcel)
        }

        override fun newArray(size: Int): Array<DBModel?> {
            return arrayOfNulls(size)
        }
    }


}