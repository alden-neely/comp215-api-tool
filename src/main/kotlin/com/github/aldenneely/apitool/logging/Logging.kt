package com.github.aldenneely.apitool.logging

import kotlin.system.exitProcess

interface Logger {
    fun print(out: Any)
    fun fatal(out: Any)

    companion object {
        val DEFAULT: Logger = DefaultLogger()
    }
}

private class DefaultLogger: Logger {

    override fun print(out: Any) {
        println(out)
    }

    override fun fatal(out: Any) {
        println("FATAL ERROR: $out")
        exitProcess(1)
    }
}