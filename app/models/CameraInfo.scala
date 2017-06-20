package models

import models.database.Camera

case class CameraInfo(camera: Camera,
                      shutter: Option[String],
                      aperture: Option[String],
                      iso: Option[String],
                      focalLength: Option[String])
