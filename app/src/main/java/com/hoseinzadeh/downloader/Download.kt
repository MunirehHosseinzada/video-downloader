package com.hoseinzadeh.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException

suspend fun main() {

    val file = File("youtubeLinks.csv")
    val fileLines = file.readLines()

    var line = 0
    while (line < fileLines.count()){
        val parts = fileLines[line].split(",")
        //Create directory in specific path
        val path = FilePath(
            language = parts[0],
            grade = parts[1],
            subject = parts[2],
            lesson = parts[3],
            lessonName = parts[4],
            videoLink = parts[5]
        )

        println(path)
        val directory = File("${path.language}/${path.grade}/${path.subject}/${path.lesson}")
        if (directory.mkdirs()) {
            println("Directory created successfully")
        }

        val video = File("$directory/${path.lessonName}.mp4")

        if(!video.exists()){
            //Executing youtube-dl command and retrying if needed
            doLongRunningTask(path = path)
                .flowOn(Dispatchers.Default)
                .retry(retries = 2) { cause ->
                    if (cause is IOException) {
                        println("delay 30 seconds ...")
                        delay(30000)
                        return@retry true
                    } else {
                        return@retry false
                    }
                }
                .catch {
                    // error
                    println("failed :(")
                }
                .collect {
                    // success
                    println("success :)")
                    val tempVideo = File("$directory/${path.lessonName}.mp4.tmp")
                    if (tempVideo.exists()){
                        tempVideo.renameTo(video)
                    }
                }
        }
        line += 2
    }
}

private fun doLongRunningTask(path: FilePath): Flow<Int> {
    return flow {

        val destination = "${path.language}/${path.grade}/${path.subject}/${path.lesson}/${path.lessonName}.mp4.tmp"

        val commandParts = listOf("youtube-dl", "-o", destination, "--all-subs", path.videoLink)
        println(commandParts)

//        val process = ProcessBuilder(commandParts)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start()
//
//        process.waitFor(60, TimeUnit.MINUTES)
//
//        val status = process.exitValue()
//
//        println(process.toString())
//
//        if (status != 0) {
//            throw IOException()
//        }

        emit(0)
    }
        .flowOn(Dispatchers.IO)
}
