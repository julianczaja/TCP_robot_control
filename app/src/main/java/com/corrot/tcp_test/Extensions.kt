package com.corrot.tcp_test

import androidx.lifecycle.MutableLiveData

/**
 * Helps to notify observer after setting MutableLiveData value
 * https://stackoverflow.com/a/52075248/10559761
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}


/**
 * @param x value to map
 * @param fromMin input value minimum
 * @param fromMax input value maximum
 * @param toMin output value minimum
 * @param toMax output value maximum
 * @return
 */
fun map(x: Int, fromMin: Int, fromMax: Int, toMin: Int, toMax: Int): Int {
    return (x - fromMin) * (toMax - toMin) / (fromMax - fromMin) + toMin
}