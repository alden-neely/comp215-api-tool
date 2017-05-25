package com.github.aldenneely.apitool

import com.xenomachina.argparser.ArgParser

import com.github.kittinunf.fuel.*
import com.github.kittinunf.result.Result


import com.jayway.jsonpath.*
import com.xenomachina.argparser.default
import java.text.ParseException

import java.util.Date
import com.strategicgains.util.date.DateAdapter

import com.github.salomonbrys.kotson.*
import com.jayway.jsonpath.spi.json.GsonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.ShowHelpException

import java.io.StringWriter

import com.github.aldenneely.apitool.logging.*

/**
 * parses strings in the date format used by github
 */
object GithubDateParser {
    private val DATE_FORMAT_STRING = "2011-09-06T17:26:27Z"
    private val parser = DateAdapter()

    /**
     * parses strings in the date format used by github
     */
    fun parse(date: String): Date? {
        return try {
            parser.parse(date)
        } catch (e: ParseException) {
            null
        }
    }
}

fun main(args: Array<String>) {
    val parser = ArgParser(args, helpFormatter=DefaultHelpFormatter())

    val parsedArgs = AppArgs(parser)

    val app = App(parsedArgs, Logger.DEFAULT)
    app.run()
}

/**
 * wrapper around the Github REST API.
 */
object GithubApi {

    val GITHUB_URL_HEAD = "https://api.github.com"

    /**
     * make a GET reqeust to github, and run callback on
     * the results.
     */
    fun request(requestUrl: String, callback: (String)->Unit) {

        val url = GITHUB_URL_HEAD + requestUrl

        url.httpGet().responseString { _, _, result ->

            when (result) {
                is Result.Failure -> {
                    Logger.DEFAULT.fatal("Could not perform request: $result")
                }

                is Result.Success -> {
                    val resp = result.get()
                    callback(resp)
                }
            }

        }
    }
}

/**
 * the main application functionality
 */
class App(val args: AppArgs, val output: Logger){

    init {
        setupJsonPathDefaults()
    }

    /**
     * run the application (main entry point).
     */
    fun run() {
        try {
            GithubApi.request(args.request, this::processApiOutput)
        } catch (e: ShowHelpException){
            val writer = StringWriter()

            e.printUserMessage(
                    writer,
                    progName = "apitool",
                    columns = 70)

            output.print(writer.buffer)
        }

    }

    /**
     * Initialize the JSONPath library to use GSON-style json representation,
     * as this is the representation other parts of our app uses.
     */
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

    /**
     * filter and show the json loaded from GitHub.
     */
    private fun processApiOutput(jsonSrc: String) {

        // JSONPath query which matches any json
        // without filtering or modification
        val ALLPASS_QUERY = "$"

        // filter the results by the user-supplied JSONPath query
        val filteredByQuery: com.google.gson.JsonArray = when (args.filter) {
            null -> JsonPath.read(jsonSrc, ALLPASS_QUERY)
            else -> JsonPath.read(jsonSrc, args.filter)
        }

        // filter by the user-supplied time threshold
        val filteredByQueryAndTime = when (args.deadline) {
            null -> filteredByQuery
            else -> filteredByQuery.filter {
                val dateCreated = GithubDateParser.parse(it["created_at"].string)

                dateCreated?.after(args.deadline) ?: false
            }
        }

        // log output to the console
        filteredByQueryAndTime.forEach { output.print(it["repo"]["url"].string) }
    }


}

/**
 * CLI argument parser.
 */
class AppArgs(parser: ArgParser) {

    /**
     * the API request URL
     */
    val request by parser.storing("-r", "--request",
            help = "specify the GitHub api request URL to use")

    /**
     * the JSONPath expression to filter the JSON
     */
    val filter by parser.storing("-f", "--filter",
            help = "filter the API results by a manual JsonPath query").default(null)

    /**
     * the time threshold for events
     */
    val deadline by parser.storing("-d", "--deadline",
            help = "set the deadline for the events, and highlight late events in red") { GithubDateParser.parse(this) }
            .default(null)

}
