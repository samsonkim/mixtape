package com.mixtape.domain

/**
 * Domain model for representing changes that need to be applied to mixtape application
 *
 * @param command
 * @param data
 */
case class ChangeAction(command: String,
                       data: Array[String])
