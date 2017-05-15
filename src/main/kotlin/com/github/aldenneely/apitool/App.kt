package com.github.aldenneely.apitool

import com.xenomachina.argparser.ArgParser

fun main(args: Array<String>) {
    val parser = ArgParser(args)
    val parsedArgs = AppArgs(parser)

}


class AppArgs(parser: ArgParser){

    val request by parser.storing("-r", "--request",
            help="specify the GitHub api request URL to use")

    val query by parser.storing("-q", "--query",
            help="filter the API results by a manual JsonPath query")

}