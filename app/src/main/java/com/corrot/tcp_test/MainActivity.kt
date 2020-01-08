package com.corrot.tcp_test

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTED
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTING
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_DISCONNECTED
import com.google.android.material.snackbar.Snackbar
import com.skydoves.progressview.ProgressView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var mainViewModel: MainViewModel
    private var dialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "CREATING ACTIVITY")

        val factory = SavedStateViewModelFactory(application, this)
        mainViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        val connectionStatusTextVIew = tv_connection
        val robotStatusTextView = tv_status_code
        val batteryTextView = tv_battery
        val progressBar = pb_main
        val shadowView = v_shadow
        val retryButton = ib_retry

        // Initialize spinner
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            this, R.array.addresses_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_ip.adapter = adapter

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
                    mainViewModel.lightLED(red = false, green = true)

                    Snackbar
                        .make(joystick, "Connected successfully!", Snackbar.LENGTH_SHORT)
                        .show()
                }
                CONNECTION_STATUS_CONNECTING -> {
                    connectionStatusTextVIew.setText(R.string.connection_status_connecting)
                    progressBar.visibility = View.VISIBLE
                    shadowView.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                    dialog?.dismiss()
                    mainViewModel.lightLED(red = true, green = true)
                }
                CONNECTION_STATUS_DISCONNECTED -> {
                    connectionStatusTextVIew.setText(R.string.connection_status_failed)
                    progressBar.visibility = View.GONE
                    shadowView.visibility = View.VISIBLE
                    dialog?.dismiss()
                    mainViewModel.lightLED(red = true, green = false)

                    if (window.decorView.isShown && !isFinishing)
                        dialog = MaterialDialog(this).show {
                            title(text = "Connection lost")
                            message(text = "Retry?")
                            cancelOnTouchOutside(true)
                            positiveButton(text = "Retry") {
                                mainViewModel.connect()
                            }
                            negativeButton(text = "Cancel") {
                                cancel()
                            }
                            onCancel {
                                retryButton.visibility = View.VISIBLE
                                shadowView.visibility = View.GONE
                            }
                        }
                }
            }
        })

        mainViewModel.sensors.observe(this, Observer {
            (rcpb_1 as ProgressView).progress = it[0].toFloat()
            (rcpb_2 as ProgressView).progress = it[1].toFloat()
            (rcpb_3 as ProgressView).progress = it[2].toFloat()
            (rcpb_4 as ProgressView).progress = it[3].toFloat()
            (rcpb_5 as ProgressView).progress = it[4].toFloat()
        })

        mainViewModel.redLed.observe(this, Observer {
            // TODO: show red led indicator
        })

        mainViewModel.greenLed.observe(this, Observer {
            // TODO: show green led indicator
        })

        mainViewModel.robotStatus.observe(this, Observer {
            if (it > 0)
                mainViewModel.lightLED(red = true, green = false)

            val robotStatusText = "Status code: $it"
            robotStatusTextView.text = robotStatusText
        })

        mainViewModel.battery.observe(this, Observer {
            val batteryText = "Battery: $it%"
            batteryTextView.text = batteryText
        })

        // Listeners
        ib_retry.setOnClickListener {
            mainViewModel.connect()
        }

        spinner_ip.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                mainViewModel.setAddress(selectedItem)
            }
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
        mainViewModel.disconnect()
        dialog?.dismiss()
    }
}
