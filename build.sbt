name := "GTF"

version := "1.05"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "joda-time" % "joda-time" % "2.3",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick" % "0.5.0.9",
  "com.github.tototoshi" %% "slick-joda-mapper" % "0.4.1",
  "com.flickr4java" % "flickr4java" % "2.11"
)

play.Project.playScalaSettings
