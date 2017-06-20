package controllers

import play.api.Play
import models.Format
import models.Format.Format

object Config {
  def flickrKey: String = {
    Play.current.configuration.getString("flickr.key") getOrElse {
      throw new RuntimeException("application.conf flickr.key not set")
    }
  }

  def flickrSecret: String = {
    Play.current.configuration.getString("flickr.secret") getOrElse {
      throw new RuntimeException("application.conf flickr.secret not set")
    }
  }

  def photoLookBackDays: Int = {
    (Play.current.configuration.getString("app.photoLookBackDays") getOrElse {
      throw new RuntimeException("application.conf app.photoLookBackDays not set")
    }).toInt
  }

  def statsRollupDays: Int = {
    (Play.current.configuration.getString("app.statsRollupDays") getOrElse {
      throw new RuntimeException("application.conf app.statsRollupDays not set")
    }).toInt
  }

  def excludeFormats: Set[Format] = Set(Format.APSH, Format.CameraPhone, Format.OneInch, Format.Unknown)
}
