package com.corrot.tcp_test

class Constants {
    companion object {
        const val DEFAULT_IP = "192.168.137.1" //"192.168.2.36"
        const val PORT = 8000
        const val TIMEOUT_TIME = 2000

        const val SAMPLE_TIME = 100L
        const val INPUT_FRAME_SIZE = 28

        const val CONNECTION_STATUS_CONNECTED = 11
        const val CONNECTION_STATUS_CONNECTING = 12
        const val CONNECTION_STATUS_DISCONNECTED = 13


        const val KEY_CONNECTION_ADDRESS = "connectionAddress"
        const val KEY_CONNECTION_STATUS = "connectionStatus"
        const val KEY_RED_LED = "redLed"
        const val KEY_GREEN_LED = "greenLed"
        const val KEY_BATTERY = "battery"
        const val KEY_SENSORS = "sensors"
        const val KEY_ROBOT_STATUS = "robotStatus"
    }
}