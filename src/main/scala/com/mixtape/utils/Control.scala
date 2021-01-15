package com.mixtape.utils

import scala.language.reflectiveCalls

/**
 * Useful utility for managing resources that need to auto-close on completion
 */
object Control {
  def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }
}
