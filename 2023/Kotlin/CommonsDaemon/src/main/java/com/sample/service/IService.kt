package com.sample.service

interface IService : Runnable {
    fun stop()
    val isStopped: Boolean?
}
