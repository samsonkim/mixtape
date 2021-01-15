package com.mixtape.domain

/**
 * User's playlist of songs domain model
 *
 * @param id
 * @param `user_id`
 * @param `song_ids`
 */
case class Playlist(id: String,
                   `user_id` : String,
                   `song_ids`: Set[String])
