package com.github.aldenneely.apitool

import com.xenomachina.argparser.ArgParser

import com.github.kittinunf.fuel.*
import com.github.kittinunf.result.Result

import kotlin.system.exitProcess

import com.jayway.jsonpath.*
import com.xenomachina.argparser.default
import java.text.ParseException

import java.text.SimpleDateFormat
import java.util.Date

import com.github.salomonbrys.kotson.*
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider

object GithubDateParser {
    private val DATE_FORMAT_STRING = "2011-09-06T17:26:27Z"
    private val format = SimpleDateFormat(DATE_FORMAT_STRING)

    fun parse(date: String): Date? {
        return try {
            format.parse(date)
        } catch (e: ParseException) {
            null
        }
    }
}

fun main(args: Array<String>) {
    val parser = ArgParser(args)
    val parsedArgs = AppArgs(parser)

    val app = App(parsedArgs, AppOutput())
    app.run()
}

class AppOutput {

    fun print(out: Any) {
        println(out)
    }

    fun fatal(out: Any) {
        println("FATAL ERROR: $out")
        exitProcess(1)
    }
}

class App(val args: AppArgs, val output: AppOutput) {

    val GITHUB_URL_HEAD = "https://api.github.com"

    init {
        setupJsonPathDefaults()
    }

    fun run() {
        makeApiRequest(args.request)
    }

    private fun setupJsonPathDefaults(){
        Configuration.setDefaults(object: Configuration.Defaults {

            val jsonProvider = GsonJsonProvider()
            override fun jsonProvider(): JsonProvider {
                return jsonProvider
            }

            val mappingProvider = GsonMappingProvider()
            override fun mappingProvider(): MappingProvider {
                return mappingProvider
            }

            override fun options(): MutableSet<Option> {
                return mutableSetOf()
            }

        })

    }

    private fun processApiOutput(jsonSrc: String) {
        val filtered: com.google.gson.JsonArray = if(args.filter != null) {
            JsonPath.read(jsonSrc, args.filter)
        } else {
            JsonPath.read(jsonSrc, "$")
        }

        val results = filtered

        val dateFilteredResults = if(args.deadline != null) {
            results.filter {
                val dateCreated = GithubDateParser.parse(it["created_at"].string)

                dateCreated?.after(args.deadline) ?: false
            }
        } else {
            results
        }

        /* show output */
        dateFilteredResults.forEach { output.print(it["repo"]["url"].string) }
    }

    private fun makeApiRequest(requestUrl: String) {

        val url = GITHUB_URL_HEAD + requestUrl

        url.httpGet().responseString { _, _, result ->

            when (result) {
                is Result.Failure -> {
                    output.fatal("Could not perform request: ${result}")
                }

                is Result.Success -> {
                    val resp = result.get()
                    processApiOutput(resp)
                }
            }

        }
    }
}


class AppArgs(parser: ArgParser) {

    val request by parser.storing("-r", "--request",
            help = "specify the GitHub api request URL to use")

    val filter by parser.storing("-f", "--filter",
            help = "filter the API results by a manual JsonPath query").default(null)

    val deadline by parser.storing("-d", "--deadline",
            help = "set the deadline for the events, and highlight late events in red") { GithubDateParser.parse(this) }
            .default(null)

}
