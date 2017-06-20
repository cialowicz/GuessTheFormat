package models

import scala.slick.lifted.MappedTypeMapper
import scala.util.Random

object Format extends Enumeration {
  type Format = Value
  val FullFrame, APSH, APSC, FourThirds, OneInch, Compact, CameraPhone, Unknown = Value

  def displayLabelFor(value: Format.Value): String = {
    value match {
      case FullFrame   => "Full-Frame"
      case APSH        => "APS-H"
      case APSC        => "APS-C"
      case FourThirds  => "Four-Thirds"
      case OneInch     => "One-Inch"
      case Compact     => "Compact"
      case CameraPhone => "Phone"
      case _           => "unknown"
    }
  }

  def cropFactorFor(value: Format.Value): String = {
    value match {
      case FullFrame   => "1.0x"
      case APSH        => "1.3x"
      case APSC        => "~1.6x"
      case FourThirds  => "2.0x"
      case OneInch     => "2.7x"
      case Compact     => "~5x+"
      case CameraPhone => "~7x+"
      case _           => "?"
    }
  }

  def cropLabelFor(value: Format.Value): String = {
    value match {
      case FullFrame   => "35mm"
      case _           => "crop"
    }
  }

  def lowerDisplayLabelFor(value: Format.Value): String = {
    if(Set(APSC, APSH).contains(value)) {
      this.displayLabelFor(value)
    } else {
      this.displayLabelFor(value).toLowerCase()
    }
  }

  // a vs. an: "taken with a full-frame camera" vs. "taken with an APS-C camera"
  def indefiniteArticleFor(value: Format.Value): String = {
    if(Set(APSC, APSH, Unknown).contains(value)) {
      "an"
    } else {
      "a"
    }
  }

  def random(excludeValues: Set[Format.Value] = Set()): Format.Value = {
    val validValues = Format.values.filterNot(excludeValues).toList
    validValues(Random.nextInt(validValues.size))
  }

  def enumToStringMapper(enum: Enumeration) = MappedTypeMapper.base[enum.Value, String](
    enum   => enum.toString,
    string => enum.withName(string))

  implicit val FormatMapper = enumToStringMapper(Format)
}

