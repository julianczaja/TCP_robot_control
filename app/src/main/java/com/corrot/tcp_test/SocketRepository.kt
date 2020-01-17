package com.corrot.tcp_test

import android.os.NetworkOnMainThreadException
import android.util.Log
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTED
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_CONNECTING
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_DISCONNECTED
import com.corrot.tcp_test.Constants.Companion.CONNECTION_STATUS_NOT_CONNECTED
import com.corrot.tcp_test.Constants.Companion.DISCONNECT_MODE_0
import com.corrot.tcp_test.Constants.Companion.DISCONNECT_MODE_1
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

    private var connectionThread: Thread
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

        // Initiate connection thread
        connectionThread = Thread()

        // Initiate looper thread
        looperThread = LooperThread()
        looperThread.start()

        // Initiate tasking thread
        taskerThread = thread(start = true) { startTasking() }
    }


    /**
     * Function that returns runnable, that creates and connect TCP socket
     * @param address socket connection IP address
     * @return Runnable, that creates and connect TCP socket
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
     * Function that returns runnable, that disconnects TCP socket and close streams
     * @return Runnable, that disconnects TCP socket and close streams
     */
    private fun getDisconnectRunnable(mode: Int): Runnable {
        return Runnable {
            Log.d(TAG, "Disconnecting socket (mode $mode)...")
            if (socket.isConnected && !socket.isClosed) {
                thread(start = true) {
                    run {
                        try {
                            inputStream.close()
                            writer.close()
                            socket.close()

                            when (mode) {
                                DISCONNECT_MODE_0 -> setConnectionStatus(
                                    CONNECTION_STATUS_DISCONNECTED
                                )
                                DISCONNECT_MODE_1 -> setConnectionStatus(
                                    CONNECTION_STATUS_NOT_CONNECTED
                                )
                            }

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
    }


    /**
     * Function that starts connect runnable on connection thread
     */
    fun connect(address: String) {
        Log.e(TAG, "STATE: ${connectionThread.state}")
        if (connectionThread.state != Thread.State.RUNNABLE) {
            try {
                connectionThread = Thread(getConnectRunnable(address))
                connectionThread.start()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "connect(): ${e.message}")
            }
        } else {
            Log.e(TAG, "connect(): Connection thread busy!")
        }
    }


    /**
     * Function that disconnects socket and closes streams on connection thread
     */
    fun disconnect(mode: Int) {
        if (connectionThread.state != Thread.State.RUNNABLE) {
            try {
                connectionThread = Thread(getDisconnectRunnable(mode))
                connectionThread.start()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "connect(): ${e.message}")
            }
        } else {
            Log.e(TAG, "disconnect(): Connection thread busy!")
        }
    }

    fun registerListener(listener: InputListener) {
        this.listener = listener
    }

    private fun setConnectionStatus(status: Int) {
//        Log.d(TAG, "NEW CONNECTION STATUS = $status")
        connectionStatus = status
        listener?.onConnectionStatusChange(status)
    }


    /**
     * Function that posts runnables to looperThread handler every _ millis
     */
    private fun startTasking() {
        Log.d(TAG, "Starting tasking")
        while (true) {

            // Check for connection state error
            if (socket.isClosed && connectionStatus == CONNECTION_STATUS_CONNECTED) {
                setConnectionStatus(CONNECTION_STATUS_DISCONNECTED)
            }

            if (connectionStatus == CONNECTION_STATUS_CONNECTED) {
                // Check if socket connection failed
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
