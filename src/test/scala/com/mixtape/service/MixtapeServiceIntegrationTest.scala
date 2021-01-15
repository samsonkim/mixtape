package com.mixtape.service

import java.io.File

import com.mixtape.domain.{MixtapeFile, Playlist}
import com.mixtape.utils.Control.using
import net.liftweb.json.JsonParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

/**
 * Tests to validate functionality via supplied mixtape-data.json and changes-data.csv
 */
class MixtapeServiceIntegrationTest extends AnyFlatSpec with Matchers {
  implicit val formats = net.liftweb.json.DefaultFormats

  val currentDirectory = new java.io.File(".").getCanonicalPath
  val dataDirectory = s"$currentDirectory/data"
  val inputFileName = s"$dataDirectory/mixtape-data.json"
  val changesFileName = s"$dataDirectory/changes-data.csv"
  val outputFileName = s"$currentDirectory/target/output.json"

  val instance = new MixtapeService()

  "ingestInputFile" should "have 7 users, 3 playlists, 40 songs for mixtape-data.json" in {
    val result = instance.ingestInputFile(inputFileName)
    result.isSuccess should be(true)

    val mixtapeData = result.get
    mixtapeData._1.size shouldEqual (7)
    mixtapeData._2.size shouldEqual (3)
    mixtapeData._3.size shouldEqual (40)
  }

  "ingestChangesFile" should "have 3 ChangeAction commands" in {
    val result = instance.ingestChangesFile(changesFileName)
    result.isSuccess shouldEqual true

    val changeActions = result.get
    changeActions.size shouldEqual 3

    val changeAction1 = changeActions(0)
    changeAction1.command shouldEqual "playlist-add"
    changeAction1.data.sameElements(Array("9", "1", "20")) shouldEqual true

    val changeAction2 = changeActions(1)
    changeAction2.command shouldEqual "playlist-remove"
    changeAction2.data.sameElements(Array("1")) shouldEqual true

    val changeAction3 = changeActions(2)
    changeAction3.command shouldEqual "playlist-addSong"
    changeAction3.data.sameElements(Array("2", "37")) shouldEqual true
  }

  "process" should "work end to end" in {
    val outputFile = File.createTempFile("mixtapeservicetest-", "-OutputFile")
    outputFile.deleteOnExit()

    val result = instance.process(inputFileName,
      changesFileName,
      outputFile.getAbsolutePath)

    result.isSuccess shouldBe (true)

    val actualJson = using(io.Source.fromFile(outputFile.getAbsoluteFile)) { source =>
      source.getLines().mkString
    }

    actualJson.length should be > 0

    val mixtapeFile = JsonParser.parse(actualJson).extract[MixtapeFile]

    mixtapeFile.users.size shouldEqual (7)
    mixtapeFile.playlists.size shouldEqual (3)
    mixtapeFile.songs.size shouldEqual (40)

    val playlists = mixtapeFile.playlists

    playlists.map(_.id) should contain only ("2", "3", "9")
    playlists.map(_.id) should contain noneOf ("1", "-1")

    playlists.filter(_.id == "9").head shouldEqual Playlist("9", "1", Set("20"))
    playlists.filter(_.id == "2").head shouldEqual Playlist("2", "3", Set("6", "8", "11", "37"))
  }
}
