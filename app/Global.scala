import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

import play.api.Play.current
import play.api.db.DB

import controllers.Config

import scala.concurrent.duration._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._

import scala.slick.driver.MySQLDriver.simple._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    Logger.info("START")

    if(play.Play.isProd()) {
      Logger.info("scheduling recurring photo population and stats rollup tasks")
      schedulePhotoPopulateOnStart(app)
      scheduleStatsRollupOnStart(app)
      scheduleRecurringPhotoPopulate(app)
      scheduleRecurringStatsRollup(app)
    }
  }

  override def onStop(app: Application) {
    Logger.info("STOP")
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(
      views.html.error(ex.toString())
    ))
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.error(request.path + " not found...")
    ))
  }

  private def schedulePhotoPopulateOnStart(app: Application) {
    Akka.system.scheduler.scheduleOnce(30.second, new Runnable {
      override def run() {
        val db = Database.forDataSource(DB.getDataSource())
        Logger.info("startup photo populator executing")
        controllers.PhotoController.populateMissing(Config.photoLookBackDays)(db.createSession())
      }
    })
  }

  private def scheduleStatsRollupOnStart(app: Application) {
    Akka.system.scheduler.scheduleOnce(30.second, new Runnable {
      override def run() {
        val db = Database.forDataSource(DB.getDataSource())
        Logger.info("startup stats rollup executing")
        controllers.StatsController.statsRollup(Config.statsRollupDays)(db.createSession())
      }
    })
  }

  private def scheduleRecurringPhotoPopulate(app: Application) {
    Akka.system.scheduler.schedule(24.hour, 24.hour, new Runnable {
      override def run() {
        val db = Database.forDataSource(DB.getDataSource())
        Logger.info("recurring photo populator executing")
        controllers.PhotoController.populatePhotosFrom(7)(db.createSession())
      }
    })
  }

  private def scheduleRecurringStatsRollup(app: Application) {
    Akka.system.scheduler.schedule(3.hour, 3.hour, new Runnable {
      override def run() {
        val db = Database.forDataSource(DB.getDataSource())
        Logger.info("recurring stats rollup executing")
        controllers.StatsController.statsRollup(Config.statsRollupDays)(db.createSession())
      }
    })
  }
}
