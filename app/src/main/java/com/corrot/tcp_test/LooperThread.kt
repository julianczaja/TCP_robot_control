package com.corrot.tcp_test

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.corrot.tcp_test.Constants.Companion.INPUT_FRAME_SIZE
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter

class LooperThread : Thread() {
    companion object {
        const val TAG = "LooperThread"
    }

    var mHandler: Handler? = null

    override fun run() {
        Looper.prepare()
        mHandler = Handler()
        Looper.loop()

        Log.d(TAG, "Closing LooperThread")
    }
}

class WriteRunnable(
    private var writer: PrintWriter,
    private var message: String
) : Runnable {
    companion object {
        const val TAG = "WriteRunnable"
    }

    override fun run() {
        try {
            writer.write(message)
            Log.d(TAG, "OUTPUT FRAME: \t\t$message")
            writer.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Connection lost. ${e.message.toString()}")
        }
    }
}

/**
 * reads input stream when there is complete frame of data
 * similar to -> https://stackoverflow.com/a/16313762/10559761
 */
class ReadRunnable(
    private val inputStream: InputStream,
    private var listener: SocketRepository.InputListener?
) : Runnable {
    companion object {
        const val TAG = "ReadRunnable"
    }

    override fun run() {
        val inputData = ByteArray(1024)
        val result = inputStream.available()

        if (result >= INPUT_FRAME_SIZE) {
            inputStream.read(inputData, 0, result)
            val s = String(inputData, 0, INPUT_FRAME_SIZE)
            Log.d(TAG, "INPUT FRAME: \t\t$s")

            if (listener != null) {
                listener!!.onDataLoaded(s)
            }
        }
    }
}
