package org.bibliarij.chat

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
