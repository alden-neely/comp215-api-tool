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

object GithubDateParser {
    private val DATE_FORMAT_STRING = "2011-09-06T17:26:27Z"
    private val parser = DateAdapter()

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

class App(val args: AppArgs, val output: Logger) {

    val GITHUB_URL_HEAD = "https://api.github.com"

    init {
        setupJsonPathDefaults()
    }

    fun run() {
        try {
            makeApiRequest(args.request, this::processApiOutput)
        }catch (e: ShowHelpException){
            val writer = StringWriter()

            e.printUserMessage(
                    writer,
                    progName = "apitool",
                    columns = 70)

            output.print(writer.buffer)
        }

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

    private fun makeApiRequest(requestUrl: String, callback: (String)->Unit) {

        val url = GITHUB_URL_HEAD + requestUrl

        url.httpGet().responseString { _, _, result ->

            when (result) {
                is Result.Failure -> {
                    output.fatal("Could not perform request: $result")
                }

                is Result.Success -> {
                    val resp = result.get()
                    callback(resp)
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
