package com.corrot.tcp_test

class Constants {
    companion object {
        const val DEFAULT_IP = "192.168.137.1"
        const val PORT = 8000
        const val TIMEOUT_TIME = 2000
        const val SAMPLE_TIME = 100L
        const val INPUT_FRAME_SIZE = 28

        const val DRIVE_MODE_NORMAL = "Normal"
        const val DRIVE_MODE_SLOW = "Slow"
        const val DRIVE_MODE_RACE = "Race"

        const val MODE_SLOW_MULTIPLIER = 0.5
        const val MODE_NORMAL_MULTIPLIER = 1
        const val MODE_RACE_MULTIPLIER = 1.5

        const val CONNECTION_STATUS_CONNECTED = 11      // while connected to socket
        const val CONNECTION_STATUS_CONNECTING = 12     // while connecting to socket
        const val CONNECTION_STATUS_DISCONNECTED = 13   // while socket connection failed
        const val CONNECTION_STATUS_NOT_CONNECTED = 14  // while user disconnected socket

        const val DISCONNECT_MODE_0 = 0  // 0 - application stops
        const val DISCONNECT_MODE_1 = 1  // 1 - user want to disconnect

        const val KEY_CONNECTION_ADDRESS = "connectionAddress"
        const val KEY_CONNECTION_STATUS = "connectionStatus"
        const val KEY_RED_LED = "redLed"
        const val KEY_GREEN_LED = "greenLed"
        const val KEY_BATTERY = "battery"
        const val KEY_SENSORS = "sensors"
        const val KEY_ROBOT_STATUS = "robotStatus"
        const val KEY_DRIVE_MODE = "driveMode"
    }
}