package com.corrot.tcp_test

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.corrot.tcp_test.Constants.Companion.KEY_BATTERY
import com.corrot.tcp_test.Constants.Companion.KEY_CONNECTION_STATUS
import com.corrot.tcp_test.Constants.Companion.KEY_LED
import com.corrot.tcp_test.Constants.Companion.KEY_SENSORS
import com.corrot.tcp_test.Constants.Companion.MAX_SPEED
import java.text.ParseException
import kotlin.concurrent.thread

class MainViewModel(handle: SavedStateHandle) : ViewModel() {
    companion object {
        const val TAG = "MainViewModel"
    }

    private var socketRepository: SocketRepository

    private var _connectionStatus = handle.getLiveData<Int>(KEY_CONNECTION_STATUS)
    private var _led = handle.getLiveData<Boolean>(KEY_LED)
    private var _battery = handle.getLiveData<Int>(KEY_BATTERY)
    private var _sensors = handle.getLiveData<MutableList<Int>>(KEY_SENSORS)

    var connectionStatus: LiveData<Int> = _connectionStatus
    var led: LiveData<Boolean> = _led
    var battery: LiveData<Int> = _battery
    var sensors: LiveData<MutableList<Int>> = _sensors

    init {
        _led.value = false
        _battery.value = 0
        _sensors.value = mutableListOf(0, 0, 0, 0, 0)

        socketRepository = SocketRepository
        socketRepository.registerListener(object : SocketRepository.InputListener {
            override fun onDataLoaded(data: String) {
                parseInputData(data)
            }

            override fun onConnectionStatusChange(connectionStatus: Int) {
                _connectionStatus.postValue(connectionStatus)
            }
        })

        // TODO: select robot number first, then connect
        connect()
    }

    fun connect() {
        thread(start = true) {
            run {
                socketRepository.connect()
            }
        }
    }

    fun disconnect() {
        thread(start = true) {
            run {
                socketRepository.disconnect()
            }
        }
    }

    fun ledClicked() {
        when (_led.value) {
            true -> {
                socketRepository.sb.setCharAt(2, '0')
                _led.value = false
            }
            false -> {
                socketRepository.sb.setCharAt(2, '1')
                _led.value = true
            }
        }
    }

    fun setJoystickValue(_x: Int, _y: Int) {
        val x = ((_x - 50) * 2) / 100f
        val y = ((-_y + 50) * 2) / 100f

        x.coerceIn(-0.8f, 0.8f)
        y.coerceIn(-0.8f, 0.8f)

        val leftMotor: Int
        val rightMotor: Int

        // TODO: find proper formula
        if (y >= 0) {
            leftMotor = (y * MAX_SPEED * (x)).toInt()
            rightMotor = (y * MAX_SPEED * (0.8 - (x))).toInt()
        } else {
            leftMotor = (y * MAX_SPEED * (x)).toInt()
            rightMotor = (y * MAX_SPEED * (0.8 - (x))).toInt()
        }

        val hexValLeft = String.format("%02X", leftMotor)
        val hexValRight = String.format("%02X", rightMotor)

        socketRepository.sb.setCharAt(3, hexValLeft[0])
        socketRepository.sb.setCharAt(4, hexValLeft[1])
        socketRepository.sb.setCharAt(5, hexValRight[0])
        socketRepository.sb.setCharAt(6, hexValRight[1])

//        Log.d(TAG, "X=$x\tY=$y\tleftMotor=${leftMotor}\trightMotor=${rightMotor}")
    }

    private fun parseInputData(data: String) {
        try {
            val battery = Integer.parseInt(data.substring(3, 7), 16)
            val mappedBattery = map(battery, 0, 65535, 0, 100)
            _battery.postValue(mappedBattery)

            val sensorsArr = arrayListOf(0, 0, 0, 0, 0)
            for (i in 0..4) {
                val sensor = Integer.parseInt(data.substring(7 + i * 4, 11 + i * 4), 16)
                val mappedSensor = map(sensor, 0, 65535, 0, 100)
                sensorsArr[i] = mappedSensor
            }
            _sensors.postValue(sensorsArr)
        } catch (e: ParseException) {
            Log.e(TAG, e.message.toString())
        }

//        Log.d(TAG, "UPDATING SENSORS: ${_sensors.value}")
    }
}
