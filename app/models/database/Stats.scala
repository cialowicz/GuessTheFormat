package models.database

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.StaticQuery
import StaticQuery.interpolation
import org.joda.time.LocalDate
import com.github.tototoshi.slick.JodaSupport._
import models.Format.Format

case class Stat(id: Option[Long],
                date: LocalDate,
                correctFormat: Format,
                incorrectFormat: Format,
                impressions: Long,
                skipped: Long,
                guesses: Long,
                correct: Long)

case class SummarizedStatVO(
                correctFormat: Format,
                incorrectFormat: Format,
                impressions: Long,
                skipped: Long,
                guesses: Long,
                correct: Long)

trait StatsComponent {
  val Stats: Stats

  class Stats extends Table[Stat]("stats") {
    def id              = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def date            = column[LocalDate]("date", O.NotNull)
    def correctFormat   = column[Format]("correctFormat", O.NotNull)
    def incorrectFormat = column[Format]("incorrectFormat", O.NotNull)
    def impressions     = column[Long]("impressions", O.NotNull)
    def skipped         = column[Long]("skipped", O.NotNull)
    def guesses         = column[Long]("guesses", O.NotNull)
    def correct         = column[Long]("correct", O.NotNull)

    def * = id.? ~ date ~ correctFormat ~ incorrectFormat ~ impressions ~ skipped ~ guesses ~ correct <> (Stat.apply _, Stat.unapply _)

    val byId = createFinderBy(_.id)
  }
}

object Stats extends DAO {
  def insert(stat: Stat)(implicit s: Session) { Stats.insert(stat) }

  def findById(id: Long)(implicit s: Session): Option[Stat] = Stats.byId(id).firstOption

  def findByDateAndFormats(date: LocalDate, correctFormat: Format, incorrectFormat: Format)(implicit s: Session): Option[Stat] = {
    Query(Stats).where(_.date === date).where(_.correctFormat === correctFormat).where(_.incorrectFormat === incorrectFormat).run.headOption
  }

  /**
   * Summarize all statistics objects since a particular date, grouping them into a
   * collection of SummarizedStatVOs
   */
  def summarizeSince(date: LocalDate)(implicit s: Session): Seq[SummarizedStatVO] = {
    Query(Stats).where(_.date > date).groupBy(x => (x.correctFormat, x.incorrectFormat)).map{
      case ((correctFormat, incorrectFormat), stats) => {
        (correctFormat,
          incorrectFormat,
          stats.map(_.impressions).sum.getOrElse(0L),
          stats.map(_.skipped).sum.getOrElse(0L),
          stats.map(_.guesses).sum.getOrElse(0L),
          stats.map(_.correct).sum.getOrElse(0L))
      }
    }.run.map(SummarizedStatVO.tupled)
  }

  // record a "skip", or insert a new stats object if none exists
  def recordSkip(date: LocalDate, correctFormat: Format, incorrectFormat: Format)(implicit s: Session) {
    this.findByDateAndFormats(date, correctFormat, incorrectFormat) match {
      case Some(stat) => {
        val updatedStat = new Stat(stat.id, stat.date, stat.correctFormat, stat.incorrectFormat, stat.impressions, stat.skipped + 1, stat.guesses, stat.correct)
        Stats.where(_.id === stat.id).update(updatedStat)
      }
      case None => this.insert(new Stat(None, date, correctFormat, incorrectFormat, 1L, 1L, 0L, 0L))
    }
  }

  // record an impression, or insert a new stats object if none exists
  def recordImpression(date: LocalDate, correctFormat: Format, incorrectFormat: Format)(implicit s: Session) {
    this.findByDateAndFormats(date, correctFormat, incorrectFormat) match {
      case Some(stat) => {
        val updatedStat = new Stat(stat.id, stat.date, stat.correctFormat, stat.incorrectFormat, stat.impressions + 1, stat.skipped, stat.guesses, stat.correct)
        Stats.where(_.id === stat.id).update(updatedStat)
      }
      case None => this.insert(new Stat(None, date, correctFormat, incorrectFormat, 1L, 0L, 0L, 0L))
    }
  }

  // record a guess, or insert a new stats object if none exists
  def recordGuess(date: LocalDate, correctFormat: Format, incorrectFormat: Format, isCorrect: Boolean)(implicit s: Session) {
    val correctIncrement = if(isCorrect) 1L else 0L
    this.findByDateAndFormats(date, correctFormat, incorrectFormat) match {
      case Some(stat) => {
        val updatedStat = new Stat(stat.id, stat.date, stat.correctFormat, stat.incorrectFormat, stat.impressions, stat.skipped, stat.guesses + 1, stat.correct + correctIncrement)
        Stats.where(_.id === stat.id).update(updatedStat)
      }
      case None => this.insert(new Stat(None, date, correctFormat, incorrectFormat, 1L, 0L, 1L, correctIncrement))
    }
  }
}
