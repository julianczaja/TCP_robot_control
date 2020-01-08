package com.corrot.tcp_test

import android.os.NetworkOnMainThreadException
import android.util.Log
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTED
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTING
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_DISCONNECTED
import com.corrot.tcp_test.Constants.Companion.DEFAULT_IP
import com.corrot.tcp_test.Constants.Companion.PORT
import com.corrot.tcp_test.Constants.Companion.TIMEOUT_TIME
import java.io.IOException
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
        Log.d(TAG, "Instantiating SocketRepository")
        // Initialize socket
        socket = Socket()

        // Create connection thread with 'connect' runnable
        connectThread = Thread(getConnectRunnable(DEFAULT_IP))

        // Start looper thread
        looperThread = LooperThread()
        looperThread.start()

        // Start tasking thread
        taskerThread = thread(start = true) { startTasking() }
    }


    /**
     * Function that creates TCP socket, starts looperThread and taskerThread
     */
    private fun getConnectRunnable(address: String): Runnable {
        return Runnable {
            setConnectionStatus(CONNECTION_STATUS_CONNECTING)
            try {
                Log.d(TAG, "Connecting to: $address (port $PORT)...")
                // Initialize socket
                socket = Socket()
                socket.connect(InetSocketAddress(address, PORT), TIMEOUT_TIME)

                if (socket.isConnected) {
                    setConnectionStatus(CONNECTION_STATUS_CONNECTED)
                    writer = PrintWriter(socket.getOutputStream())
                    inputStream = socket.getInputStream()
                    Log.d(TAG, "Connected successfully!")
                } else {
                    setConnectionStatus(CONNECTION_STATUS_DISCONNECTED)
                    Log.e(TAG, "Connection failed!")
                }
            } catch (e: Exception) {
                setConnectionStatus(CONNECTION_STATUS_DISCONNECTED)
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
     * Function that starts connect runnable on new thread
     */
    fun connect(address: String) {
        if (!connectThread.isAlive) {
            thread(start = true) {
                run {
                    connectThread = Thread(getConnectRunnable(address))
                    connectThread.run()
                }
            }
        } else {
            Log.e(TAG, "Already connecting to socket!")
        }
    }


    /**
     * Function that close streams and socket on new thread
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting socket...")
        if (socket.isConnected && !socket.isClosed && !connectThread.isAlive) {
            thread(start = true) {
                run {
                    try {
                        inputStream.close()
                        writer.close()
                        socket.close()
                        setConnectionStatus(CONNECTION_STATUS_DISCONNECTED)
                        Log.d(TAG, "Socket disconnected successfully!")
                    } catch (e: IOException) {
                        Log.e(TAG, "Error during closing socket: ${e.message}")
                    }
                }
            }
        } else {
            Log.d(TAG, "Socket already disconnected")
        }
    }

    fun registerListener(listener: InputListener) {
        this.listener = listener
    }

    private fun setConnectionStatus(status: Int) {
        Log.d(TAG, "NEW CONNECTION STATUS = $status")
        connectionStatus = status
        listener?.onConnectionStatusChange(status)
    }


    /**
     * Function that posts runnables to looperThread handler every _ millis
     */
    private fun startTasking() {
        Log.d(TAG, "Starting tasking")
        while (true) {

            if (socket.isClosed && connectionStatus == CONNECTION_STATUS_CONNECTED) {
                setConnectionStatus(CONNECTION_STATUS_DISCONNECTED)
            }

            if (connectionStatus == CONNECTION_STATUS_CONNECTED) {
                // Check if connection failed
                looperThread.mHandler?.post {
                    if (writer.checkError())
                        setConnectionStatus(CONNECTION_STATUS_DISCONNECTED)
                }
                // Write output frame
                looperThread.mHandler?.post(WriteRunnable(writer, sb.toString()))
                // Read input frame
                looperThread.mHandler?.post(ReadRunnable(inputStream, listener))
            }
            Thread.sleep(Constants.SAMPLE_TIME)
        }
    }
}
