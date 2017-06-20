package models.database

private[models] trait DAO
  extends CamerasComponent
  with UnknownCamerasComponent
  with PhotosComponent
  with StatsComponent
  with StatsSummaryComponent
{
  val Cameras = new Cameras
  val UnknownCameras = new UnknownCameras
  val Photos = new Photos
  val Stats = new Stats
  val StatsSummary = new StatsSummary
}
