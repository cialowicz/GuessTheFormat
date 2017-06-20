package models.database

import models.Format.Format
import scala.slick.driver.MySQLDriver.simple._

case class Camera(id: Option[Long],
                  exifMake: String,
                  exifModel: String,
                  format: Format,
                  displayText: Option[String],
                  adCode: Option[String]) {

  // Only return the exif camera model if the camera make is already contained in it
  def normalizedDisplayText: String = {
    this.displayText.getOrElse({
      if(this.exifModel.toLowerCase() contains this.exifMake.toLowerCase()) {
        this.exifModel
      } else {
        this.exifMake + " " + this.exifModel
      }
    })
  }

  def adUrl: Option[String] = {
    this.adCode match {
      case Some(ad) => {
        val adCodeXml = scala.xml.XML.loadString("<container>" + ad.replaceAll("&", "&amp;") + "</container>")
        Some((adCodeXml \\ "a" \ "@href").text)
      }
      case None => None
    }
  }
}

trait CamerasComponent {
  val Cameras: Cameras

  class Cameras extends Table[Camera]("cameras") {
    def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def exifMake    = column[String]("exifMake", O.NotNull)
    def exifModel   = column[String]("exifModel", O.NotNull)
    def format      = column[Format]("format", O.NotNull)
    def displayText = column[String]("displayText", O.Nullable)
    def adCode      = column[String]("adCode", O.Nullable)

    def * = id.? ~ exifMake ~ exifModel ~ format ~ displayText.? ~ adCode.? <> (Camera.apply _, Camera.unapply _)

    val byId    = createFinderBy(_.id)
    val byModel = createFinderBy(_.exifModel)
  }
}

object Cameras extends DAO {
  def insert(camera: Camera)(implicit s: Session) { Cameras.insert(camera) }

  def findById(id: Long)(implicit s: Session): Option[Camera] = Cameras.byId(id).firstOption

  def findByModel(model: String)(implicit s: Session): Option[Camera] = Cameras.byModel(model).firstOption

  def countByFormat(format: Format)(implicit s: Session): Int = Query(Cameras).where(_.format === format).length.run
}

