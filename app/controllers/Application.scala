package controllers

import models._
import models.database._

import play.api._
import play.api.mvc._

import play.api.Play.current
import play.api.db.slick._
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.Json

object Application extends Controller {
  def index = Action {
    Redirect(routes.Application.photo)
  }

  def newSession = Action {
    Redirect(routes.Application.index).withNewSession
  }

  def photo = DBAction {
    implicit session => {
      val correct = session.session.get("correct") match {
        case Some(correct) => correct.toInt
        case None          => 0
      }
      val guesses = session.session.get("guesses") match {
        case Some(guesses) => guesses.toInt
        case None          => 0
      }

      val randomFormat = Format.random(Config.excludeFormats)
      val p = PhotoController.fetchRandomDisplayPhoto(dayRange = Config.photoLookBackDays, randomFormat)
      p match {
        case Some(photo) => {
          val camera = Cameras.findById(photo.cameraId).getOrElse(throw new RuntimeException("missing camera"))

          // generate two format options: one correct, and one random incorrect
          val incorrectFormat = Format.random(Config.excludeFormats + photo.format)
          val r = new scala.util.Random(scala.compat.Platform.currentTime)
          val shuffledFormats = r.shuffle(Set(photo.format, incorrectFormat)).toList

          // record the impression
          Stats.recordImpression(new LocalDate(), photo.format, incorrectFormat)

          Ok(views.html.photo(photo, camera, shuffledFormats(0), shuffledFormats(1), (correct, guesses)))
        }
        case None => Ok(views.html.error("Something went wrong fetching a photo...", true))
      }
    }
  }

  def guess(photoId: String, formatGuess: String, otherFormat: String) = DBAction {
    implicit session => {
      val result = PhotoController.makeGuess(photoId.toLong, Format.withName(formatGuess), Format.withName(otherFormat))

      val correct = session.session.get("correct") match {
        case Some(correct) => if(result) correct.toInt + 1 else correct.toInt
        case None          => if(result) 1 else 0
      }
      val guesses = session.session.get("guesses") match {
        case Some(guesses) => guesses.toInt + 1
        case None          => 1
      }

      val photo = Photos.findById(photoId.toLong).getOrElse(throw new RuntimeException("invalid photo id: " + photoId))
      val camera = Cameras.findById(photo.cameraId).getOrElse(throw new RuntimeException("invalid camera id: " + photo.id.toString()))

      val jsonResult = Json.obj(
        "result"      -> result.toString(),
        "correct"     -> correct.toString(),
        "guesses"     -> guesses.toString(),
        "photoUrl"    -> photo.url,
        "format"      -> Format.displayLabelFor(photo.format),
        "camera"      -> camera.normalizedDisplayText,
        "cameraAdUrl" -> camera.adUrl.getOrElse[String](""))

      Ok(Json.stringify(jsonResult)).withSession(session.session + ("correct" -> correct.toString) + ("guesses" -> guesses.toString))
    }
  }

  def about() = DBAction {
    implicit session => {
      Ok(views.html.about())
    }
  }

  def skip(photoId: String, format1: String, format2: String) = DBAction {
    implicit session => {
      PhotoController.skipPhoto(photoId.toLong, Format.withName(format1), Format.withName(format2))

      val photo = Photos.findById(photoId.toLong).getOrElse(throw new RuntimeException("invalid photo id: " + photoId))
      val camera = Cameras.findById(photo.cameraId).getOrElse(throw new RuntimeException("invalid camera id: " + photo.id.toString()))

      val jsonResult = Json.obj(
        "photoUrl"    -> photo.url,
        "format"      -> Format.displayLabelFor(photo.format),
        "camera"      -> camera.normalizedDisplayText,
        "cameraAdUrl" -> camera.adUrl.getOrElse[String](""))

      Ok(Json.stringify(jsonResult))
    }
  }

  def stats = DBAction {
    implicit session => {
      val usableFormats = Format.values.filterNot(Config.excludeFormats).toList
      val date = new DateTime().minusDays(Config.photoLookBackDays).toLocalDate
      val cameraCounts = usableFormats.map(format => (format, Cameras.countByFormat(format))).toMap
      val photoCounts = usableFormats.map(format => (format, Photos.countByFormatSinceDate(format, date))).toMap
      val guessStats = usableFormats.map(format => (format, StatsSummary.findByCorrectFormat(format).map(otherFormat => (otherFormat.incorrectFormat, otherFormat)).toMap)).toMap
      val identifiability = usableFormats.map(format => {
        val summaries = StatsSummary.findByCorrectFormat(format)
        val correct = summaries.map(_.correct).sum
        val guesses = summaries.map(_.guesses).sum
        (format, correct.toDouble/guesses.toDouble)
      }).toMap
      Ok(views.html.stats.stats(usableFormats, cameraCounts, photoCounts, guessStats, identifiability))
    }
  }

  def error(message: String = "Something went wrong...") = DBAction {
    implicit session => {
      Ok(views.html.error(message))
    }
  }

  // js Routing
  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Application.guess,
        routes.javascript.Application.skip
      )
    ).as("text/javascript")
  }
}
