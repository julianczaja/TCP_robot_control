package com.corrot.tcp_test

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTED
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTING
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_FAILED
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var mainViewModel: MainViewModel
    private var dialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val factory = SavedStateViewModelFactory(application, this)
        mainViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        val connectionStatusTextVIew = tv_connection
        val progressBar = pb_main
        val shadowView = v_shadow
        val retryButton = ib_retry
        val ledButton = btn_led

        // Observers
        mainViewModel.connectionStatus.observe(this, Observer {
            Log.d(TAG, "Connection status changed: $it")
            when (it) {
                CONNECTION_STATUS_CONNECTED -> {
                    connectionStatusTextVIew.setText(R.string.connection_status_connected)
                    progressBar.visibility = View.GONE
                    shadowView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    dialog?.dismiss()
                    Snackbar
                        .make(ledButton, "Connected successfully!", Snackbar.LENGTH_SHORT)
                        .show()
                }
                CONNECTION_STATUS_CONNECTING -> {
                    connectionStatusTextVIew.setText(R.string.connection_status_connecting)
                    progressBar.visibility = View.VISIBLE
                    shadowView.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                }
                CONNECTION_STATUS_FAILED -> {
                    connectionStatusTextVIew.setText(R.string.connection_status_failed)
                    progressBar.visibility = View.GONE
                    shadowView.visibility = View.VISIBLE

                    if (window.decorView.isShown && !isFinishing)
                        dialog = MaterialDialog(this).show {
                            title(text = "Connection lost")
                            message(text = "Retry?")
                            positiveButton(text = "Retry") {
                                mainViewModel.connect()
                            }
                            negativeButton(text = "Cancel") {
                                retryButton.visibility = View.VISIBLE
                                shadowView.visibility = View.GONE
                                dismiss()
                            }
                        }
                }
            }
        })

        mainViewModel.sensors.observe(this, Observer {
            rcpb_1.progress = it[0].toFloat()
            rcpb_2.progress = it[1].toFloat()
            rcpb_3.progress = it[2].toFloat()
            rcpb_4.progress = it[3].toFloat()
            rcpb_5.progress = it[4].toFloat()
        })

        mainViewModel.led.observe(this, Observer {
            // TODO: show led indicator
        })

        mainViewModel.battery.observe(this, Observer {
            // TODO: show battery status
        })


        // Listeners
        ib_retry.setOnClickListener {
            mainViewModel.connect()
        }

        ledButton.setOnClickListener {
            mainViewModel.ledClicked()
        }

        joystick.setOnMoveListener({ _, _ ->
            mainViewModel.setJoystickValue(joystick.normalizedX, joystick.normalizedY)
        }, (1000 / Constants.SAMPLE_TIME).toInt())
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.connect()
    }

    override fun onPause() {
        super.onPause()
        dialog?.dismiss()
        mainViewModel.disconnect()
    }
}
