name := "Image Info Viewer"

version := "0.1"

scalaVersion := "2.12.6"

lazy val root = project
  .in(file("."))
  .aggregate(
    common,
    dumper
  )

lazy val common = project

lazy val dumper = project
  .dependsOn(common)
