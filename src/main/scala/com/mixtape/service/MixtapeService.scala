package com.mixtape.service

import java.io.{BufferedWriter, File, FileWriter}

import com.mixtape.domain._
import com.mixtape.utils.Control.using
import net.liftweb.json.JsonParser
import net.liftweb.json.Serialization.write

import scala.collection.mutable
import scala.util.Try

/**
 * Service that manages Mixtape application functionality
 */
class MixtapeService {
  implicit val formats = net.liftweb.json.DefaultFormats

  /**
   * Main entry point for the mixtape service.
   * Ingests input and changes file and applies changes to the dataset
   * and serializes the updated state to output json file
   *
   * @param inputFileName
   * @param changesFileName
   * @param outputFileName
   * @return
   */
  def process(inputFileName: String,
              changesFileName: String,
              outputFileName: String) = {

    val outputMixtapeData = for {
      mixtapeData <- ingestInputFile(inputFileName)
      changeActions <- ingestChangesFile(changesFileName)
      updatedMixtapeData <- applyChanges(mixtapeData._1,
        mixtapeData._2,
        mixtapeData._3,
        changeActions)
    } yield updatedMixtapeData

    outputMixtapeData.map(d => {
      generateOutputFile(outputFileName, d._1, d._2, d._3)
    }).flatten
  }

  /**
   * Apply changes from mixtape data with changes that need to be applied
   *
   * @param users
   * @param playlists
   * @param songs
   * @param changeActions
   * @return
   */
  def applyChanges(users: Set[User],
                   playlists: Set[Playlist],
                   songs: Set[Song],
                   changeActions: Seq[ChangeAction]) = {

    val mutablePlaylists = playlists.foldLeft(mutable.Map.empty[String, Playlist]) {
      (m, p) => m += (p.id -> p)
    }

    Try {
      changeActions.foreach(changeAction => {
        changeAction.command.trim.toLowerCase match {
          case "playlist-add" => {
            val playlistId = changeAction.data(0)
            val userId = changeAction.data(1)
            val songIds = changeAction.data(2).split(",")
            mutablePlaylists += (playlistId -> Playlist(playlistId,
              userId,
              songIds.toSet
            ))
          }
          case "playlist-remove" => {
            val playlistId = changeAction.data(0)
            mutablePlaylists -= playlistId
          }
          case "playlist-addsong" => {
            val playlistId = changeAction.data(0)
            val songId = changeAction.data(1)

            mutablePlaylists.get(playlistId)
              .map { p =>
                val songIds = p.`song_ids` ++ Set(songId)
                p.copy(`song_ids` = songIds)
              }.foreach { p =>
              mutablePlaylists(playlistId) = p
            }
          }
          case _ => {
            throw new RuntimeException(s"Unknown command. changeAction=$changeAction")
          }
        }
      })
      (users, mutablePlaylists.values.toSet, songs)
    }
  }

  /**
   * Ingests inputfile to produce users, playlists, and songs
   *
   * @param fileName
   * @return
   */
  def ingestInputFile(fileName: String): Try[(Set[User], Set[Playlist], Set[Song])] = {
    Try {
      val content = using(io.Source.fromFile(fileName)) { source =>
        source.getLines().mkString
      }
      val mixtapeFile = JsonParser.parse(content).extract[MixtapeFile]
      Tuple3(mixtapeFile.users, mixtapeFile.playlists, mixtapeFile.songs)
    }
  }

  /**
   * Ingests changesFile to produce sequences of changes to perform
   *
   * @param fileName
   * @return
   */
  def ingestChangesFile(fileName: String): Try[Seq[ChangeAction]] = {
    Try {
      val changeActions = using(io.Source.fromFile(fileName)) { source =>
        source.getLines()
          .map(_.trim)
          .filter(_.length > 0)
          .map { line =>
            val cols = line.split(";").map(_.trim)
            ChangeAction(cols(0), cols.drop(1))
          }.toSeq
      }
      changeActions
    }
  }

  /**
   * Generates output file of mixtape data in json format
   *
   * @param fileName
   * @param users
   * @param playlists
   * @param songs
   * @return
   */
  def generateOutputFile(fileName: String,
                         users: Set[User],
                         playlists: Set[Playlist],
                         songs: Set[Song]
                        ): Try[Unit] = {
    Try {
      val mixtapeFile = MixtapeFile(users, playlists, songs)
      val json = write(mixtapeFile)
      val file = new File(fileName)
      using(new BufferedWriter(new FileWriter(file))) { bw =>
        bw.write(json)
      }
    }
  }
}