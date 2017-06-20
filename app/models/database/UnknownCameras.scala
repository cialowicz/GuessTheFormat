package models.database

import models.Format.Format
import scala.slick.driver.MySQLDriver.simple._

case class UnknownCamera(id: Option[Long],
                          exifMake: Option[String],
                          exifModel: String,
                          count: Int)

trait UnknownCamerasComponent {
  val UnknownCameras: UnknownCameras

  class UnknownCameras extends Table[UnknownCamera]("unknownCameras") {
    def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def exifMake    = column[String]("exifMake", O.Nullable)
    def exifModel   = column[String]("exifModel", O.NotNull)
    def count       = column[Int]("count", O.NotNull)

    def * = id.? ~ exifMake.? ~ exifModel ~ count <> (UnknownCamera.apply _, UnknownCamera.unapply _)

    val byId = createFinderBy(_.id)
  }
}

object UnknownCameras extends DAO {
  def insert(unknownCamera: UnknownCamera)(implicit s: Session) { UnknownCameras.insert(unknownCamera) }

  def findById(id: Long)(implicit s: Session): Option[UnknownCamera] = UnknownCameras.byId(id).firstOption

  def findByExifMakeAndModel(exifMake: Option[String], exifModel: String)(implicit s: Session): Option[UnknownCamera] = {
    Query(UnknownCameras).where(_.exifMake === exifMake).where(_.exifModel === exifModel).run.headOption
  }

  def upsert(unknownCamera: UnknownCamera)(implicit s: Session) {
    this.findByExifMakeAndModel(unknownCamera.exifMake, unknownCamera.exifModel) match {
      case Some(existing) => {
        val updatedUnknownCamera = new UnknownCamera(
          existing.id,
          existing.exifMake,
          existing.exifModel,
          existing.count + 1
        )
        UnknownCameras.where(_.id === existing.id).update(updatedUnknownCamera)
      }
      case None => this.insert(unknownCamera)
    }
  }
}

