package models.database

import scala.slick.driver.MySQLDriver.simple._
import models.Format.Format

case class SummarizedStat(id: Option[Long],
                       correctFormat: Format,
                       incorrectFormat: Format,
                       impressions: Long,
                       skipped: Long,
                       guesses: Long,
                       correct: Long)

trait StatsSummaryComponent {
  val StatsSummary: StatsSummary

  class StatsSummary extends Table[SummarizedStat]("statsSummary") {
    def id              = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def correctFormat   = column[Format]("correctFormat", O.NotNull)
    def incorrectFormat = column[Format]("incorrectFormat", O.NotNull)
    def impressions     = column[Long]("impressions", O.NotNull)
    def skipped         = column[Long]("skipped", O.NotNull)
    def guesses         = column[Long]("guesses", O.NotNull)
    def correct         = column[Long]("correct", O.NotNull)

    def * = id.? ~ correctFormat ~ incorrectFormat ~ impressions ~ skipped ~ guesses ~ correct <> (SummarizedStat.apply _, SummarizedStat.unapply _)

    val byId = createFinderBy(_.id)
  }
}

object StatsSummary extends DAO {
  def insert(summarizedStat: SummarizedStat)(implicit s: Session) { StatsSummary.insert(summarizedStat) }

  def findById(id: Long)(implicit s: Session): Option[SummarizedStat] = StatsSummary.byId(id).firstOption

  def upsert(summarizedStat: SummarizedStat)(implicit s: Session) {
    this.findByFormats(summarizedStat.correctFormat, summarizedStat.incorrectFormat) match {
      case Some(existingStat) => {
        val updatedStat = new SummarizedStat(
          existingStat.id,
          existingStat.correctFormat,
          existingStat.incorrectFormat,
          summarizedStat.impressions,
          summarizedStat.skipped,
          summarizedStat.guesses,
          summarizedStat.correct)
        StatsSummary.where(_.id === existingStat.id).update(updatedStat)
      }
      case None => this.insert(summarizedStat)
    }
  }

  def findByFormats(correctFormat: Format, incorrectFormat: Format)(implicit s: Session): Option[SummarizedStat] = {
    Query(StatsSummary).where(_.correctFormat === correctFormat).where(_.incorrectFormat === incorrectFormat).run.headOption
  }

  def findByCorrectFormat(correctFormat: Format)(implicit s: Session): Seq[SummarizedStat] = {
    Query(StatsSummary).where(_.correctFormat === correctFormat).list
  }
}
