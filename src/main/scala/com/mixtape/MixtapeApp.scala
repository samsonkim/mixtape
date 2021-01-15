package com.mixtape

import com.mixtape.service.MixtapeService
import wvlet.airframe.launcher.{Launcher, command, option}

import scala.util.{Failure, Success}

class MixtapeApp(
                  @option(prefix = "-i,--input", description = "Input filename(json)")
                  inputFilename: Option[String],
                  @option(prefix = "-c,--changes", description = "Changes filename(csv)")
                  changesFilename: Option[String],
                  @option(prefix = "-o,--output", description = "[Optional] Output filename(json). Default is output.json")
                  outputFilename: Option[String],
                  @option(prefix = "-h,--help", description = "show help", isHelp = true)
                  displayHelp: Boolean
                ) {

  val currentDirectory = new java.io.File(".").getCanonicalPath
  val defaultOutputFileName = s"$currentDirectory/output.json"

  @command(isDefault = true)
  def default(): Unit = {
    val newOutputFile = outputFilename.getOrElse(defaultOutputFileName)
    println(s"input=$inputFilename\nchanges=$changesFilename\noutput=$newOutputFile")

    val service = new MixtapeService

    val checkFilename = (filename: Option[String], field: String) => {
      filename match {
        case Some(x) => x
        case None => println(s"$field is required")
      }
    }

    checkFilename(inputFilename, "Input file")
    checkFilename(changesFilename, "Changes file")

    for {
      input <- inputFilename
      changes <- changesFilename
    } yield {
      val result = service.process(input, changes, newOutputFile)
      result match {
        case Success(_) => println("Success!")
        case Failure(f) => println(s"Failure:\n$f")
      }
    }
    println("\nComplete")
  }
}

object MixtapeApp extends App {
  Launcher.execute[MixtapeApp](args)
}
