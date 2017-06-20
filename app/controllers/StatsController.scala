package controllers

import play.api.mvc._

import play.api.db.slick.Session
import play.api.Logger
import org.joda.time.{DateTime, LocalDate}
import models.database.{StatsSummary, SummarizedStat, Stats}

object StatsController extends Controller {
  def statsRollup(sinceDaysAgo: Int)(implicit s: Session) {
    Logger.info("rolling up stats since " + sinceDaysAgo.toString() + " days ago")
    val start: Long = System.currentTimeMillis() / 1000
    val date = new LocalDate(new DateTime().minusDays(sinceDaysAgo).toDate)
    Stats.summarizeSince(date).map(
      x => new SummarizedStat(None, x.correctFormat, x.incorrectFormat, x.impressions, x.skipped, x.guesses, x.correct)
    ).map(StatsSummary.upsert _)
    val end: Long = System.currentTimeMillis() / 1000
    val diff = end - start
    Logger.info("done rolling up stats: " + diff.toString() + " seconds")
  }
}
