package models.database

import scala.slick.driver.MySQLDriver.simple._
import org.joda.time.LocalDate
import com.github.tototoshi.slick.JodaSupport._
import models.Format.Format

case class Photo(id: Option[Long],
                 flickrPhotoId: String,
                 url: String,
                 imageUrlMd: String,
                 imageUrlLg: String,
                 title: Option[String],
                 username: String,
                 userId: String,
                 fullName: Option[String],
                 cameraId: Long,
                 format: Format,
                 shutter: Option[String],
                 aperture: Option[String],
                 iso: Option[String],
                 focalLength: Option[String],
                 exploreDate: LocalDate,
                 exploreNumber: Int) {

  def date = {
    exploreDate.toDateTimeAtStartOfDay().toDate()
  }
}

trait PhotosComponent {
  val Photos: Photos

  class Photos extends Table[Photo]("photos") {
    def id            = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def flickrPhotoId = column[String]("flickrPhotoId", O.NotNull)
    def url           = column[String]("url", O.NotNull)
    def imageUrlMd    = column[String]("imageUrlMd", O.NotNull)
    def imageUrlLg    = column[String]("imageUrlLg", O.NotNull)
    def title         = column[String]("title", O.Nullable)
    def username      = column[String]("username", O.NotNull)
    def userId        = column[String]("userId", O.NotNull)
    def fullName      = column[String]("fullName", O.Nullable)
    def cameraId      = column[Long]("cameraId", O.NotNull)
    def format        = column[Format]("format", O.NotNull)
    def shutter       = column[String]("shutter", O.Nullable)
    def aperture      = column[String]("aperture", O.Nullable)
    def iso           = column[String]("iso", O.Nullable)
    def focalLength   = column[String]("focalLength", O.Nullable)
    def exploreDate   = column[LocalDate]("exploreDate", O.NotNull)
    def exploreNumber = column[Int]("exploreNumber", O.NotNull)

    def * = id.? ~ flickrPhotoId ~ url ~ imageUrlMd ~ imageUrlLg ~ title.? ~ username ~ userId ~ fullName.? ~ cameraId ~ format ~ shutter.? ~ aperture.? ~ iso.? ~ focalLength.? ~ exploreDate ~ exploreNumber <> (Photo.apply _, Photo.unapply _)

    val byId            = createFinderBy(_.id)
    val byFlickrPhotoId = createFinderBy(_.flickrPhotoId)
  }
}

object Photos extends DAO {
  def insert(photo: Photo)(implicit s: Session) { Photos.insert(photo) }

  def findById(id: Long)(implicit s: Session): Option[Photo] = Photos.byId(id).firstOption

  def findByFlickrPhotoId(flickrPhotoId: String)(implicit s: Session): Option[Photo] = Photos.byFlickrPhotoId(flickrPhotoId).firstOption

  def upsert(photo: Photo)(implicit s: Session) {
    this.findByFlickrPhotoId(photo.flickrPhotoId) match {
      case Some(existingPhoto) => {
        val updatedPhoto = new Photo(
          existingPhoto.id,
          existingPhoto.flickrPhotoId,
          photo.url,
          photo.imageUrlMd,
          photo.imageUrlLg,
          photo.title,
          photo.username,
          photo.userId,
          photo.fullName,
          photo.cameraId,
          photo.format,
          photo.shutter,
          photo.aperture,
          photo.iso,
          photo.focalLength,
          photo.exploreDate,
          photo.exploreNumber
        )
        Photos.where(_.id === existingPhoto.id).update(updatedPhoto)
      }
      case None => Photos.insert(photo)
    }
  }

  def randomByFormatSinceDate(format: Format, newerThan: LocalDate)(implicit s: Session): Option[Photo] = {
    val photos = Query(Photos).where(_.format === format).where(_.exploreDate > newerThan).run
    val r = new scala.util.Random(scala.compat.Platform.currentTime)
    r.shuffle(photos).headOption
  }

  def countByDate(date: LocalDate)(implicit s: Session): Int = {
    Query(Photos).where(_.exploreDate === date).length.run
  }

  def countByFormatSinceDate(format: Format, newerThan: LocalDate)(implicit s: Session): Int = {
    Query(Photos).where(_.format === format).where(_.exploreDate > newerThan).length.run
  }
}

