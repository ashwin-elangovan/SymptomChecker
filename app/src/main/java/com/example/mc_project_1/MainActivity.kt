package com.example.mc_project_1

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.mc_project_1.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

var heartRate = ""

var myDB = DBModel()

class MainActivity : AppCompatActivity() {

    private lateinit var mProgressDialog: ProgressDialog

    private lateinit var viewBinding: ActivityMainBinding

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val sensorListener =
        object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    // Process the accelerometer data (x, y, z)
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Add the data to the data list
                    accelerometerData.add(AccelerometerData(x, y, z))
                }
            }
        }

    private var accelerometerData = mutableListOf<AccelerometerData>()

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MobileComputing)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        mProgressDialog = ProgressDialog(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val symptomsButton = findViewById<Button>(R.id.symptomsButton)

        // Set an OnClickListener for the button
        symptomsButton.setOnClickListener {
            // Create an Intent to navigate to the SymptomsActivity
            val intent = Intent(this@MainActivity, SymptomsActivity::class.java)

            // Start the new activity
            startActivity(intent)
        }

        val uploadSignButton = findViewById<Button>(R.id.uploadSignButton)

        val dbHelper = SQLiteHelper(this, null)

        uploadSignButton.setOnClickListener {
            if (myDB.HEART_RATE == null && myDB.RESPIRATORY_RATE == null) {
                Toast.makeText(baseContext, "Heart rate / Respiratory rate missing", Toast.LENGTH_SHORT).show()
            }
            else if (myDB.HEART_RATE == null) {
                dbHelper.getLastSymptom()?.takeIf { it.moveToLast() }?.run {
                    Log.d("Inside first", "Inside first")
                    val isNulllastRespiratoryRate = isNull(getColumnIndex(SQLiteHelper.RESPIRATORY_RATE_COL))
                    Log.d("Last Respiratory Rate", isNulllastRespiratoryRate.toString())
                    if (isNulllastRespiratoryRate) {
                        Log.d("lastRespiratoryRate null", "lastRespiratoryRate null")
                        dbHelper.updateLastSign(SQLiteHelper.RESPIRATORY_RATE_COL, myDB.RESPIRATORY_RATE)
                    } else {
                        Log.d("lastRespiratoryRate present", "lastRespiratoryRate present")
                        dbHelper.addSign(myDB)
                    }
                } ?: dbHelper.addSign(myDB)
            }

            else if (myDB.RESPIRATORY_RATE == null) {
                dbHelper.getLastSymptom()?.takeIf { it.moveToLast() }?.run {
                    Log.d("Inside second", "Inside second")
                    val isNulllastHeartRate = isNull(getColumnIndex(SQLiteHelper.HEART_RATE_COL))
                    Log.d("Last Heart Rate", isNulllastHeartRate.toString())
                    if (isNulllastHeartRate) {
                        Log.d("lastHeartRate null", "lastHeartRate null")
                        dbHelper.updateLastSign(SQLiteHelper.HEART_RATE_COL, myDB.HEART_RATE)
                    } else {
                        Log.d("lastHeartRate present", "lastHeartRate present")
                        dbHelper.addSign(myDB)
                    }
                } ?: dbHelper.addSign(myDB)
            }
            else {
                dbHelper.addSign(myDB)
            }
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false) permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    // Button click handler to start recording accelerometer data
    fun startRecording(view: View) {
        accelerometerData.clear() // Clear previous data
        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        Toast.makeText(baseContext, "Getting accelerometer data...", Toast.LENGTH_SHORT).show()

        mProgressDialog.setMessage("Capturing accelerometer data..")
        mProgressDialog.setCancelable(false)
        mProgressDialog.show()
        // Stop recording after 45 seconds
        Handler(Looper.getMainLooper())
            .postDelayed(
                {
                    sensorManager.unregisterListener(sensorListener)
                    Toast.makeText(
                        baseContext,
                        "Collected accelerometer data...",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    val respiratoryRate = callRespiratoryCalculator(accelerometerData)
                    myDB.RESPIRATORY_RATE = respiratoryRate.toFloat()
                    Log.d(
                        "RespiratoryRate",
                        "Respiratory Rate: $respiratoryRate breaths per minute"
                    )
                    mProgressDialog.dismiss()
                },
                45000
            ) // 45 seconds in milliseconds
    }

    private fun callRespiratoryCalculator(data: List<AccelerometerData>): Int {
        var previousValue = 10f
        var k = 0
        for (item in data) {
            Log.d("AccelerometerData", "X: ${item.x}, Y: ${item.y}, Z: ${item.z}")
        }
        for (i in 11 until data.size) {
            val accelX = data[i].x
            val accelY = data[i].y
            val accelZ = data[i].z

            var currentValue =
                sqrt(
                    accelZ.toDouble().pow(2.0) +
                            accelX.toDouble().pow(2.0) +
                            accelY.toDouble().pow(2.0)
                )
                    .toFloat()

            if (abs(previousValue - currentValue) > 0.15) {
                k++
            }

            previousValue = currentValue
        }

        val result = ((k / 45.00) * 30).toInt()

        val rValueTextView = findViewById<TextView>(R.id.rValue)

        rValueTextView.text = result.toString()

        return result
    }

    @SuppressLint("RestrictedApi")
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        Toast.makeText(baseContext, "Recording video...", Toast.LENGTH_SHORT).show()

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
                }
            }

        val mediaStoreOutputOptions =
            MediaStoreOutputOptions.Builder(
                contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

        videoCapture.getCamera()?.getCameraControl()?.enableTorch(true)

        recording =
            videoCapture.output
                .prepareRecording(this, mediaStoreOutputOptions)
                .apply {
                    if (
                        PermissionChecker.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PermissionChecker.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            viewBinding.videoCaptureButton.apply {
                                //                            text =
                                // getString(R.string.stop_capture)

                                mProgressDialog.setMessage("Capturing video..")
                                mProgressDialog.setCancelable(false)
                                mProgressDialog.show()
                                isEnabled = false
                            }
                            Handler(Looper.getMainLooper())
                                .postDelayed(
                                    {
                                        recording?.stop()
                                        recording?.close()
                                        recording = null
                                    },
                                    45000
                                )
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                videoCapture.getCamera()?.getCameraControl()?.enableTorch(false)
                                val msg =
                                    "Video capture succeeded: " +
                                            "${recordEvent.outputResults.outputUri}"

                                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                                Log.d(TAG, msg)
                                val output_uri = recordEvent.outputResults.outputUri
                                val hrValueTextView =
                                    findViewById<TextView>(R.id.hrValue)
                                Log.d("OutputURI", convertMediaUriToPath(output_uri))
//                                hrValueTextView.text = "Computing Heart rate.."
                                val videoFilePath = convertMediaUriToPath(output_uri)
                                val slowTask = SlowTask(hrValueTextView, videoFilePath, mProgressDialog)
                                slowTask.execute()
                            } else {
                                recording?.close()
                                recording = null
                                Log.e(
                                    TAG,
                                    "Video capture ends with error: " + "${recordEvent.error}"
                                )
                            }
                            viewBinding.videoCaptureButton.apply {
//                                text = getString(com.example.mc_project_1.R.string.start_capture)
                                isEnabled = true
                            }
                        }
                    }
                }
    }

    fun convertMediaUriToPath(uri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, proj, null, null, null)
        val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val path = cursor.getString(column_index)
        cursor.close()
        return path
    }

    open class SlowTask(private val hrValueTextView: TextView, private val videoFilePath: String, private val mProgressDialog: ProgressDialog) :
        AsyncTask<String, String, String?>() {

        override fun onPreExecute() {
            super.onPreExecute()
            // This method is executed on the UI thread before the background task starts

            // Initialize the ProgressDialog
            mProgressDialog.setMessage("Computing heart rate..")
            mProgressDialog.setCancelable(false)
        }
        override fun doInBackground(vararg params: String?): String? {
            var retriever = MediaMetadataRetriever()
            var frameList = ArrayList<Bitmap>()
            Log.d("videoFilePath", videoFilePath.toString())
            try {
                retriever.setDataSource(videoFilePath.toString())
                var duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)

                Log.d("Frame count", duration.toString())
                var aduration = duration!!.toInt()

                //                Log.d("aduration", aduration.toString())
                var i = 10
                while (i < aduration) {
                    val bitmap = retriever.getFrameAtIndex(i)
                    Log.i("ImageProgress", ((i.toFloat()/duration.toFloat()) * 100).toString() + "%")
//                    hrValueTextView.text = "Computing Heart rate.." +
                    frameList.add(bitmap!!)
                    i += 5
                }
                Log.d("ivalue", i.toString())
            } catch (m_e: Exception) {
                Log.d("Exceptioninsideheart", m_e.toString())
            } finally {
                Log.println(Log.ASSERT, "Inside finally", "Inside finally")
                retriever?.release()
                var redBucket: Long = 0
                var pixelCount: Long = 0
                val a = mutableListOf<Long>()
                for (i in frameList) {
                    redBucket = 0
                    for (y in 550 until 650) {
                        for (x in 550 until 650) {
                            val c: Int = i.getPixel(x, y)
                            pixelCount++
                            redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                        }
                    }
                    a.add(redBucket)
                }
                if (a.isNotEmpty()) {
                    val b = mutableListOf<Long>()
                    Log.d("A values", a.toString())
                    for (i in 0 until a.lastIndex - 5) {
                        var temp =
                            (a.elementAt(i) +
                                    a.elementAt(i + 1) +
                                    a.elementAt(i + 2) +
                                    a.elementAt(i + 3) +
                                    a.elementAt(i + 4)) / 4
                        b.add(temp)
                    }
                    Log.d("B values", b.toString())
                    var x = b.elementAt(0)
                    if (b.isNotEmpty()) {
                        var count = 0
                        for (i in 1 until b.lastIndex) {
                            var p = b.elementAt(i.toInt())
                            if ((p - x) > 3500) {
                                count = count + 1
                            }
                            x = b.elementAt(i.toInt())
                        }
                        var rate = ((count.toFloat() / 45) * 60).toInt()
                        heartRate = (rate / 2).toString()
                        Log.d("Heart rate", heartRate)
                        return heartRate
                    }
                }
                return "-1"
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != "-1") {
                hrValueTextView.text = result
                if (result != null) {
                    myDB.HEART_RATE = result.toFloat()
                }
            }

            mProgressDialog.dismiss()

            // Delete the video file
            videoFilePath?.let { filePath ->
                val videoFile = File(filePath)
                if (videoFile.exists()) {
                    val deleted = videoFile.delete()
                    if (deleted) {
                        // Show a toast indicating that the video was deleted
                        Toast.makeText(
                            hrValueTextView.context,
                            "Video deleted successfully",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
            {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
                    }

                val recorder =
                    Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                videoCapture = VideoCapture.withOutput(recorder)

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "MC_APP_MAIN_ACTIVITY"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                .toTypedArray()
    }
}

data class AccelerometerData(val x: Float, val y: Float, val z: Float)
