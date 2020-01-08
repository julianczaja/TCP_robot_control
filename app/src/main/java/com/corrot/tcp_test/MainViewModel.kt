package com.corrot.tcp_test

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_DISCONNECTED
import com.corrot.tcp_test.Constants.Companion.DEFAULT_IP
import com.corrot.tcp_test.Constants.Companion.KEY_BATTERY
import com.corrot.tcp_test.Constants.Companion.KEY_CONNECTION_ADDRESS
import com.corrot.tcp_test.Constants.Companion.KEY_CONNECTION_STATUS
import com.corrot.tcp_test.Constants.Companion.KEY_GREEN_LED
import com.corrot.tcp_test.Constants.Companion.KEY_RED_LED
import com.corrot.tcp_test.Constants.Companion.KEY_ROBOT_STATUS
import com.corrot.tcp_test.Constants.Companion.KEY_SENSORS
import java.text.ParseException

class MainViewModel(val handle: SavedStateHandle) : ViewModel() {
    companion object {
        const val TAG = "MainViewModel"
    }

    private var socketRepository: SocketRepository
    private var _connectionAddress = handle.get<String>(KEY_CONNECTION_ADDRESS)

    private var _connectionStatus = handle.getLiveData<Int>(KEY_CONNECTION_STATUS)
    private var _robotStatus = handle.getLiveData<Int>(KEY_ROBOT_STATUS)
    private var _greenLed = handle.getLiveData<Boolean>(KEY_GREEN_LED)
    private var _redLed = handle.getLiveData<Boolean>(KEY_RED_LED)
    private var _battery = handle.getLiveData<Int>(KEY_BATTERY)
    private var _sensors = handle.getLiveData<MutableList<Int>>(KEY_SENSORS)


    var connectionStatus: LiveData<Int> = _connectionStatus
    var robotStatus: LiveData<Int> = _robotStatus
    var redLed: LiveData<Boolean> = _redLed
    var greenLed: LiveData<Boolean> = _greenLed
    var battery: LiveData<Int> = _battery
    var sensors: LiveData<MutableList<Int>> = _sensors

    init {
        if (_connectionAddress.isNullOrEmpty())
            _connectionAddress = DEFAULT_IP

        _connectionStatus.value = CONNECTION_STATUS_DISCONNECTED

        _redLed.value = false
        _greenLed.value = false
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
    }

    fun connect() {
        if (connectionStatus.value == CONNECTION_STATUS_DISCONNECTED) {
            socketRepository.connect(_connectionAddress!!)
        } else {
            Log.e(TAG, "Already connected!")
        }
    }

    fun disconnect() {
        handle.set(KEY_CONNECTION_STATUS, CONNECTION_STATUS_DISCONNECTED)
        if (connectionStatus.value != CONNECTION_STATUS_DISCONNECTED) {
            socketRepository.disconnect()
        }
    }

    fun setAddress(address: String) {
        if (address != _connectionAddress) {
            _connectionAddress = address
            disconnect()
            connect()
        }
    }

    fun lightLED(red: Boolean, green: Boolean) {
        if (red) {
            socketRepository.sb.setCharAt(1, '1')
            _redLed.value = true
        } else {
            socketRepository.sb.setCharAt(1, '0')
            _redLed.value = false
        }

        if (green) {
            socketRepository.sb.setCharAt(2, '1')
            _greenLed.value = true
        } else {
            socketRepository.sb.setCharAt(2, '0')
            _greenLed.value = false
        }
    }

    fun setJoystickValue(_x: Int, _y: Int) {
        val x = ((_x - 50)) // TODO: wymyślić funkcję wygładzającą (luźno można x2 to pomnożyć)
        val y = ((-_y + 50))

        x.coerceIn(-80, 80)
        y.coerceIn(-80, 80)

        val leftMotor: Int
        val rightMotor: Int

        // TODO: find proper formula
//        if (y >= 0) {
//            leftMotor = (y * MAX_SPEED * (x)).toInt()
//            rightMotor = (y * MAX_SPEED * (0.8 - (x))).toInt()
//        } else {
//            leftMotor = (y * MAX_SPEED * (x)).toInt()
//            rightMotor = (y * MAX_SPEED * (0.8 - (x))).toInt()
//        }
        leftMotor = y + x
        rightMotor = y - x

        val hexValLeft = if (leftMotor >= 0) {
            String.format("%02X", leftMotor)
        } else {
            String.format("%02X", leftMotor.and(255))
        }

        val hexValRight = if (rightMotor >= 0) {
            String.format("%02X", rightMotor)
        } else {
            String.format("%02X", rightMotor.and(255))
        }

        socketRepository.sb.setCharAt(3, hexValLeft[0])
        socketRepository.sb.setCharAt(4, hexValLeft[1])
        socketRepository.sb.setCharAt(5, hexValRight[0])
        socketRepository.sb.setCharAt(6, hexValRight[1])

//        Log.d(TAG, "X=$x\tY=$y\tleftMotor=${leftMotor}\trightMotor=${rightMotor}")
    }

    private fun parseInputData(data: String) {
        try {
            val status = Integer.parseInt(data.substring(1, 2), 16)
            _robotStatus.postValue(status)

            val battery = Integer.parseInt(data.substring(3, 7), 16)
            val mappedBattery = map(battery, 0, 65535, 0, 100)
            _battery.postValue(mappedBattery)

            val sensorsArr = arrayListOf(0, 0, 0, 0, 0)
            for (i in 0..4) {
                val sensor = Integer.parseInt(data.substring(7 + i * 4, 11 + i * 4), 16)
                val mappedSensor = map(sensor, 0, 53255, 0, 100)
                sensorsArr[i] = mappedSensor
            }
            _sensors.postValue(sensorsArr)
        } catch (e: ParseException) {
            Log.e(TAG, e.message.toString())
        } catch (e: NumberFormatException) {
            Log.e(TAG, e.message.toString())
        }

//        Log.d(TAG, "UPDATING SENSORS: ${_sensors.value}")
    }
}
