package com.corrot.tcp_test

class Constants {
    companion object {
        const val IP = "192.168.137.1"//"192.168.2.31"
        const val PORT = 8000
        const val TIMEOUT_TIME = 3000

        const val SAMPLE_TIME = 100L
        const val INPUT_FRAME_SIZE = 28
        const val MAX_SPEED = 128

        const val CONNECTION_STATUS_CONNECTED = 11
        const val CONNECTION_STATUS_CONNECTING = 12
        const val CONNECTION_STATUS_FAILED = 13

        const val KEY_CONNECTION_STATUS = "connectionStatus"
        const val KEY_LED = "led"
        const val KEY_BATTERY = "battery"
        const val KEY_SENSORS = "sensors"
    }
}