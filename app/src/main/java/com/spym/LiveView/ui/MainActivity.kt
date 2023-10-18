package com.spym.LiveView.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Camera
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.spym.LiveView.R

import com.spym.LiveView.adapters.MainRecyclerViewAdapter
import com.spym.LiveView.databinding.ActivityMainBinding
import com.spym.LiveView.repository.MainRepository
import com.spym.LiveView.service.MainService
import com.spym.LiveView.service.MainServiceRepository
import com.spym.LiveView.utils.DataModel
import com.spym.LiveView.utils.DataModelType
import com.spym.LiveView.utils.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener {
    private val TAG = "MainActivity"
    private val mCamera: Camera? =null

    private lateinit var views: ActivityMainBinding
    var username: String? = null


    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    private var mainAdapter: MainRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        init()
    }

    private fun init() {
        username = intent.getStringExtra("username")
        if (username == null) finish()
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }

    private fun subscribeObservers() {
        setupRecyclerView()
        MainService.listener = this
        mainRepository.observeUsersStatus {
            Log.d(TAG, "subscribeObservers: $it")
            mainAdapter?.updateList(it)
        }
    }

    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        views.mainRecyclerView.apply {
            setLayoutManager(layoutManager)
            adapter = mainAdapter
        }
    }

    private fun startMyService() {
        mainServiceRepository.startService(username!!)
    }

    override fun onVideoCallClicked(username: String) {


        if (!isCameraReleased()){
            showCustomToast(this, "Camera in Use", R.drawable.baseline_camera_24)
        }


        showCameraSelectionDialog(username)


    }

    fun StartCallActivity(face: Boolean, username: String) {
    

        Log.d("MokardderHossain", "Check isFront = $face")
        
        
        if (username == ""){
            Log.d(TAG, "StartCallActivity: Mismatch Username")
        }


        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, face,true) {
                if (it) {
                    //we have to start video call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra("target", username)
                        putExtra("isVideoCall", true)
//                        putExtra("isFront", isFront)
                        putExtra("isCaller", true)
                    })

                }
            }

        }

    }


    override fun onAudioCallClicked(username: String) {
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, false, false) {
                if (it) {

                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra("target", username)
                        putExtra("isVideoCall", false)
                        putExtra("isCaller", true)
                    })
                }
            }
        }
    }

    private fun showCameraSelectionDialog(username: String) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Select Camera")
            .setMessage("Choose the camera you want to use:")
            .setPositiveButton("Front") { dialog, which ->

                StartCallActivity(true, username)
                Log.d(TAG, "showCameraSelectionDialog: sdadw")

            }
            .setNegativeButton("Back") { dialog, which ->

                StartCallActivity(false, username)

            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCallReceived(model: DataModel) {
        runOnUiThread {



            Toast.makeText(this, "Called", Toast.LENGTH_SHORT)

            views.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true



//                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        //create an intent to go to video call activity
                        startActivity(Intent(this@MainActivity, CallActivity::class.java).apply {
                            putExtra("target", model.sender)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("isCaller", false)
                        })


                    }

                    declineButton.setOnClickListener {
                        incomingCallLayout.isVisible = false
                    }

//                }
            }
        }


    }
    fun isCameraOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
    fun showCustomToast(context: Context, message: String, iconResId: Int) {
        // Inflate the custom layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.toast_layout, null)

        // Customize the toast layout components
        val icon = layout.findViewById<ImageView>(R.id.custom_toast_icon)
        icon.setImageResource(iconResId)

        val text = layout.findViewById<TextView>(R.id.custom_toast_text)
        text.text = message

        // Create and show the custom toast
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM, 0, 0)
        toast.show()
    }
    fun isCameraReleased(): Boolean {
            return mCamera == null;
    }
}