package controllers

import play.api._
import play.api.mvc._
import scala.collection.JavaConversions._

import com.flickr4java.flickr.{Flickr, REST}
import com.flickr4java.flickr.photos.{Extras, Exif}

import java.util.Date
import org.joda.time.{LocalDate, DateTime}

import models._
import models.Format.Format
import models.database._
import play.api.db.slick.Session

object PhotoController extends Controller {
  val flickr = new Flickr(Config.flickrKey, Config.flickrSecret, new REST("api.flickr.com"))

  def populatePhotosFrom(daysAgo: Int)(implicit s: Session) {
    val date = new DateTime().minusDays(daysAgo).toDate
    Logger.info("populating photos from: " + new LocalDate(date).toString())
    val photos = allInterestingPhotosFor(date)
    for((p, i) <- photos.view.zipWithIndex) {
      photoToPhoto(p, new LocalDate(date), i+1) match {
        case Some(photo) => Photos.upsert(photo)
        case None        => ()
      }
    }
  }

  def populateMissing(sinceDaysAgo: Int)(implicit s: Session) {
    Logger.info("populating all missing photos since " + sinceDaysAgo.toString() + " days ago")
    for(i <- sinceDaysAgo to 1 by -1) {
      val date = new LocalDate(new DateTime().minusDays(i).toDate)
      if(Photos.countByDate(date) < 250) {
        this.populatePhotosFrom(i)
      }
    }
    Logger.info("done populating missing photos")
  }

  def fetchRandomDisplayPhoto(dayRange: Int = Config.photoLookBackDays, format: Format)(implicit s: Session): Option[Photo] = {
    Photos.randomByFormatSinceDate(format, new DateTime().minusDays(dayRange).toLocalDate)
  }

  def makeGuess(photoId: Long, formatGuess: Format, otherFormat: Format)(implicit s: Session): Boolean = {
    Photos.findById(photoId) match {
      case Some(photo) => {
        val result = photo.format == formatGuess
        val incorrectFormat = if(result) otherFormat else formatGuess
        Stats.recordGuess(new LocalDate(), photo.format, incorrectFormat, result)
        result
      }
      case None => {
        false
      }
    }
  }

  def skipPhoto(photoId: Long, format1: Format, format2: Format)(implicit s: Session) {
    Photos.findById(photoId) match {
      case Some(photo) => {
        val result = photo.format == format1
        val incorrectFormat = if(result) format2 else format1
        Stats.recordSkip(new LocalDate(), photo.format, incorrectFormat)
        ()
      }
      case None => ()
    }
  }

  // Flickr photo to a local application Photo object
  private def photoToPhoto(photo: com.flickr4java.flickr.photos.Photo, exploreDate: LocalDate, exploreNumber: Int)(implicit s: Session): Option[Photo] = {
    val cameraInfo = exifFor(photo) match {
      case Some(exif) => resolveCameraInfoFrom(exif)
      case None       => None
    }

    cameraInfo match {
      case Some(info) => {
        Some(Photo(
          None,
          photo.getId,
          photo.getUrl,
          photo.getMediumUrl,
          photo.getLargeUrl,
          Option(photo.getTitle).filter(_.trim.nonEmpty),
          photo.getOwner.getUsername,
          photo.getOwner.getId,
          Option(photo.getOwner.getRealName).filter(_.trim.nonEmpty),
          info.camera.id.getOrElse(-1),
          info.camera.format,
          info.shutter,
          info.aperture,
          info.iso,
          info.focalLength,
          exploreDate,
          exploreNumber))
      }
      case None => None
    }
  }

  private def resolveCameraInfoFrom(exif: List[Exif])(implicit s: Session): Option[CameraInfo] = {
    val exifMap = exif.map{ e => (
      Option(e.getLabel).filter(_.trim.nonEmpty).getOrElse(null),
      Option(e.getRaw).filter(_.trim.nonEmpty).getOrElse(null))
    }.toMap

    val make = exifMap.get("Make")
    val model = exifMap.get("Model")
    val shutter = exifMap.get("Exposure")
    val aperture = exifMap.get("Aperture")
    val iso = exifMap.get("ISO Speed")
    val focalLength = exifMap.get("Focal Length")

    val cameraOption: Option[Camera] = model match {
      case Some(exifModel) => Cameras.findByModel(exifModel)
      case _               => None
    }

    cameraOption match {
      case Some(camera) => Some(CameraInfo(camera, shutter, aperture, iso, focalLength))
      case None         => {
        logUnknownCameraType(make, model)
        None
      }
    }
  }

  private def logUnknownCameraType(make: Option[String], model: Option[String])(implicit s: Session) {
    (make, model) match {
      case (Some(exifMake), Some(exifModel)) => UnknownCameras.upsert(new UnknownCamera(None, Some(exifMake), exifModel, 1))
      case (None, Some(exifModel))           => UnknownCameras.upsert(new UnknownCamera(None, None, exifModel, 1))
      case _                                 => ()
    }
  }

  private def exifFor(photo: com.flickr4java.flickr.photos.Photo): Option[List[Exif]] = {
    try {
      flickr.getPhotosInterface.getExif(photo.getId, Config.flickrSecret).toList match {
        case exifData: List[Exif] => Some(exifData)
        case _                    => None
      }
    } catch {
      case _ : Throwable => None
    }
  }

  private def allInterestingPhotosFor(date: Date): List[com.flickr4java.flickr.photos.Photo] = {
    val interestingIface = flickr.getInterestingnessInterface
    val mostInterestingImages = interestingIface.getList(date, Extras.ALL_EXTRAS, 500, 1)
    mostInterestingImages.toList.asInstanceOf[List[com.flickr4java.flickr.photos.Photo]]
  }
}
