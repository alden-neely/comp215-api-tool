package com.github.aldenneely.apitool

import com.xenomachina.argparser.ArgParser

import com.github.kittinunf.fuel.*
import com.github.kittinunf.result.Result

import kotlin.system.exitProcess
import com.jayway.jsonpath.*

fun main(args: Array<String>) {
    val parser = ArgParser(args)
    val parsedArgs = AppArgs(parser)

    val app = App(parsedArgs, AppOutput())
    app.run()
}

class AppOutput{

    fun print(out: Any){
        println(out)
    }

    fun fatal(out: Any){
        println("FATAL ERROR: $out")
        exitProcess(1)
    }
}

class App(val args: AppArgs, val output: AppOutput) {

    val GITHUB_URL_HEAD = "http://api.github.com"

    fun run(){
        makeApiRequest(args.request)
    }

    private fun parseJson(jsonSrc: String) {
        val json: List<String> = JsonPath.read(jsonSrc, args.query)
        output.print(json)
    }

    private fun makeApiRequest(requestUrl: String) {

        val url = GITHUB_URL_HEAD + requestUrl

        url.httpGet().responseString { _, _, result ->

            when(result){
                is Result.Failure -> {
                    output.fatal("Could not perform request")
                }

                is Result.Success -> {
                    val resp = result.get()
                    parseJson(resp)
                }
            }

        }
    }
}


class AppArgs(parser: ArgParser){

    val request by parser.storing("-r", "--request",
            help="specify the GitHub api request URL to use")

    val query by parser.storing("-q", "--query",
            help="filter the API results by a manual JsonPath query")

}