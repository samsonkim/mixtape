package com.mixtape.domain

/**
 * Represents structure of input and output file of Mixtape Data
 *
 * @param users
 * @param playlists
 * @param songs
 */
case class MixtapeFile(users: Set[User],
                       playlists: Set[Playlist],
                       songs: Set[Song])
