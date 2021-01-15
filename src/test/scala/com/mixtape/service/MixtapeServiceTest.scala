package com.mixtape.service

import java.io.{BufferedWriter, File, FileWriter}

import com.mixtape.domain.{ChangeAction, Playlist, Song, User}
import com.mixtape.utils.Control.using
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

/**
 * Unit tests for MixtapeService
 */
class MixtapeServiceTest extends AnyFlatSpec with Matchers {
  val instance = new MixtapeService()

  "process" should "work end to end" in {
    val inputFile = File.createTempFile("mixtapeservicetest-", "-InputFile")
    inputFile.deleteOnExit()
    using(new BufferedWriter(new FileWriter(inputFile))) { bw =>
      bw.write(getMixtapeJson())
    }

    val changesFile = File.createTempFile("mixtapeservicetest-", "-ChangesFile")
    changesFile.deleteOnExit()
    using(new BufferedWriter(new FileWriter(changesFile))) { bw =>
      bw.write(getChangesCsv())
    }

    val outputFile = File.createTempFile("mixtapeservicetest-", "-OutputFile")
    outputFile.deleteOnExit()

    val result = instance.process(inputFile.getAbsolutePath,
    changesFile.getAbsolutePath,
    outputFile.getAbsolutePath)

    result.isSuccess shouldBe (true)

    val actualJson = using(io.Source.fromFile(outputFile.getAbsoluteFile)) { source =>
      source.getLines().mkString
    }
    val expectedJson =
      """|{"users":[{"id":"user1","name":"testuser"}],"playlists":[{"id":"playlistA","user_id":"user1","song_ids":["song1","song2"]},{"id":"playlist2","user_id":"user1","song_ids":["song2","song3"]}],"songs":[{"id":"song1","artist":"artist1","title":"title1"},{"id":"song2","artist":"artist2","title":"title2"},{"id":"song3","artist":"artist3","title":"title3"}]}
         |""".stripMargin

    actualJson shouldEqual expectedJson.trim
  }

  "ingestInputFile" should "have 1 user, 2 playlists, 3 songs for test.json" in {
    val tempFile = File.createTempFile("mixtapeservicetest-", "-ingestInputFile")
    tempFile.deleteOnExit()
    using(new BufferedWriter(new FileWriter(tempFile))) { bw =>
      bw.write(getMixtapeJson())
    }

    val result = instance.ingestInputFile(tempFile.getAbsolutePath)
    result.isSuccess shouldBe (true)

    val mixtapeData = result.get
    mixtapeData._1 shouldEqual Set(User("user1", "testuser"))
    mixtapeData._2 shouldEqual Set(Playlist("playlist1", "user1", Set("song1")),
      Playlist("playlist2", "user1", Set("song2")))
    mixtapeData._3 shouldEqual Set(Song("song1", "artist1", "title1"),
      Song("song2", "artist2", "title2"),
      Song("song3", "artist3", "title3"))

    mixtapeData shouldEqual getMixtapeData()
  }

  "ingestChangesFile" should "have 3 ChangeAction commands" in {
    val tempFile = File.createTempFile("mixtapeservicetest-", "-ingestChangesFile")
    tempFile.deleteOnExit()
    using(new BufferedWriter(new FileWriter(tempFile))) { bw =>
      bw.write(getChangesCsv())
    }

    val result = instance.ingestChangesFile(tempFile.getAbsolutePath)
    result.isSuccess shouldEqual true

    val changeActions = result.get
    changeActions.size shouldEqual 3

    val changeAction1 = changeActions(0)
    changeAction1.command shouldEqual "playlist-add"
    changeAction1.data.sameElements(Array("playlistA", "user1", "song1,song2")) shouldEqual true

    val changeAction2 = changeActions(1)
    changeAction2.command shouldEqual "playlist-remove"
    changeAction2.data.sameElements(Array("playlist1")) shouldEqual true

    val changeAction3 = changeActions(2)
    changeAction3.command shouldEqual "playlist-addSong"
    changeAction3.data.sameElements(Array("playlist2", "song3")) shouldEqual true
  }

  "applyChanges" should "handle playlist-add action" in {
    val changeAction = ChangeAction("playlist-add", Array("playlistA", "user1", "song1,song2"))

    val result = instance.applyChanges(Set.empty[User],
      Set.empty[Playlist],
      Set.empty[Song],
      Seq(changeAction))

    result.isSuccess shouldBe(true)
    result.foreach{ r =>
      r._1.isEmpty shouldBe(true)
      r._2.head shouldEqual Playlist("playlistA", "user1", Set("song1", "song2"))
      r._3.isEmpty shouldBe(true)
    }
  }

  "applyChanges" should "handle playlist-remove action" in {
    val changeAction = ChangeAction("playlist-remove", Array("playlist1"))

    val playlists = Set(Playlist("playlist1", "user1", Set("song1")),
      Playlist("playlist2", "user1", Set("song2")))

    val result = instance.applyChanges(Set.empty[User],
      playlists,
      Set.empty[Song],
      Seq(changeAction))

    result.isSuccess shouldBe(true)
    result.foreach{ r =>
      r._1.isEmpty shouldBe(true)
      r._2.head shouldEqual Playlist("playlist2", "user1", Set("song2"))
      r._3.isEmpty shouldBe(true)
    }
  }

  "applyChanges" should "handle playlist-addSong action" in {
    val changeAction = ChangeAction("playlist-addSong", Array("playlist2", "song3"))

    val playlists = Set(Playlist("playlist2", "user1", Set("song2")))

    val result = instance.applyChanges(Set.empty[User],
      playlists,
      Set.empty[Song],
      Seq(changeAction))

    result.isSuccess shouldBe(true)
    result.foreach{ r =>
      r._1.isEmpty shouldBe(true)
      r._2.head shouldEqual Playlist("playlist2", "user1", Set("song2", "song3"))
      r._3.isEmpty shouldBe(true)
    }
  }

  "applyChanges" should "handle unknown action" in {
    val changeAction = ChangeAction("someotheraction", Array("playlist2", "song3"))

    val result = instance.applyChanges(Set.empty[User],
      Set.empty[Playlist],
      Set.empty[Song],
      Seq(changeAction))

    result.isFailure shouldBe(true)
  }

  "generateOutputFile" should "produce json file" in {
    val tempFile = File.createTempFile("mixtapeservicetest-", "-generateOutputFile")
    tempFile.deleteOnExit()

    val mixtapeData = getMixtapeData()

    instance.generateOutputFile(tempFile.getAbsolutePath,
      mixtapeData._1,
      mixtapeData._2,
      mixtapeData._3
    )

    val actualJson = using(io.Source.fromFile(tempFile.getAbsoluteFile)) { source =>
      source.getLines().mkString
    }

    actualJson shouldEqual getMixtapeJson().trim
  }

  private def getMixtapeData() = {
    val users = Set(User("user1", "testuser"))
    val playlists = Set(Playlist("playlist1", "user1", Set("song1")),
      Playlist("playlist2", "user1", Set("song2")))
    val songs = Set(Song("song1", "artist1", "title1"),
      Song("song2", "artist2", "title2"),
      Song("song3", "artist3", "title3"))

    (users, playlists, songs)
  }

  private def getMixtapeJson() = {
    val json =
      """|{"users":[{"id":"user1","name":"testuser"}],"playlists":[{"id":"playlist1","user_id":"user1","song_ids":["song1"]},{"id":"playlist2","user_id":"user1","song_ids":["song2"]}],"songs":[{"id":"song1","artist":"artist1","title":"title1"},{"id":"song2","artist":"artist2","title":"title2"},{"id":"song3","artist":"artist3","title":"title3"}]}
         |""".stripMargin
    json
  }

  private def getChangesCsv() = {
    val csv =
      """
        |playlist-add;playlistA;user1;song1,song2
        |playlist-remove;playlist1
        |playlist-addSong;playlist2;song3
        |""".stripMargin
    csv
  }
}
