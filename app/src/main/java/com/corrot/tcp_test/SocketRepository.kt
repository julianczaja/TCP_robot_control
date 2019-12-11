package com.corrot.tcp_test

import android.os.NetworkOnMainThreadException
import android.util.Log
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTED
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTING
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_FAILED
import com.corrot.tcp_test.Constants.Companion.IP
import com.corrot.tcp_test.Constants.Companion.PORT
import com.corrot.tcp_test.Constants.Companion.TIMEOUT_TIME
import java.io.InputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Singleton socket repository for handling low level stuff.
 */
object SocketRepository {
    private const val TAG = "SocketRepository"

    interface InputListener {
        fun onDataLoaded(data: String)
        fun onConnectionStatusChange(connectionStatus: Int)
    }

    private var listener: InputListener? = null

    private var connectThread: Thread
    private var taskerThread: Thread // thread that gives looperThread runnables
    private var looperThread: LooperThread

    private var socket: Socket
    private lateinit var writer: PrintWriter
    private lateinit var inputStream: InputStream
    private var connectionStatus: Int = 0

    var sb = StringBuilder("[000000]")


    init {
        Log.d(TAG, "Instantiating SocketRepository...")
        // Initialize socket
        socket = Socket()

        // Create connection thread with 'connect' runnable
        connectThread = Thread(getConnectRunnable())

        // Start looper thread
        looperThread = LooperThread()
        looperThread.start()

        // Start tasking thread
        taskerThread = thread(start = true) { startTasking() }
    }

    /**
     * Function that creates TCP socket, starts looperThread and taskerThread
     */
    private fun getConnectRunnable(): Runnable {
        return Runnable {
            setConnectionStatus(CONNECTION_STATUS_CONNECTING)
            try {
                Log.d(TAG, "Connecting to: $IP...")
                // Initialize socket
                socket = Socket()
                socket.connect(InetSocketAddress(IP, PORT), TIMEOUT_TIME)

                if (socket.isConnected) {
                    setConnectionStatus(CONNECTION_STATUS_CONNECTED)
                    writer = PrintWriter(socket.getOutputStream())
                    inputStream = socket.getInputStream()
                    Log.d(TAG, "Connected successfully!")
                } else {
                    setConnectionStatus(CONNECTION_STATUS_FAILED)
                    Log.e(TAG, "Connection failed!")
                }
            } catch (e: Exception) {
                setConnectionStatus(CONNECTION_STATUS_FAILED)
                when (e) {
                    is NetworkOnMainThreadException ->
                        Log.e(TAG, "You can't connect TCP on main thread!")
                    else ->
                        Log.e(TAG, e.toString())
                }
            }
        }
    }

    /**
     * Function that starts connect runnable
     */
    fun connect() {
        if (!connectThread.isAlive) {
            connectThread.run()
        }
    }

    fun disconnect() {
        setConnectionStatus(CONNECTION_STATUS_FAILED)
//        if (socket.isConnected) {
//            socket.close()
//        }
    }

    fun registerListener(listener: InputListener) {
        this.listener = listener
    }

    private fun setConnectionStatus(status: Int) {
        connectionStatus = status
        listener?.onConnectionStatusChange(status)
    }

    /**
     * Function that posts runnables to looperThread handler every _ millis
     */
    private fun startTasking() {
        Log.d(TAG, "Starting writing...")
        while (true) {

            if (socket.isClosed && connectionStatus == CONNECTION_STATUS_CONNECTED) {
                setConnectionStatus(CONNECTION_STATUS_FAILED)
            }

            when (connectionStatus) {
                CONNECTION_STATUS_CONNECTED -> {
                    // Check if connection failed
                    looperThread.mHandler?.post {
                        if (writer.checkError()) {
                            setConnectionStatus(CONNECTION_STATUS_FAILED)
                        }
                    }
                    // Write output frame
                    looperThread.mHandler?.post(WriteRunnable(writer, sb.toString()))
                    // Read input frame
                    looperThread.mHandler?.post(ReadRunnable(inputStream, listener))
                }
//                CONNECTION_STATUS_FAILED -> { // TODO: uncomment to activate auto-connect
//                    thread(start = true) {
//                        run {
//                            connect()
//                        }
//                    }
//                }
            }
            Thread.sleep(Constants.SAMPLE_TIME)
        }
    }
}
