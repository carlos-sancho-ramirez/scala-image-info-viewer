name := "Image Info Viewer"

version := "0.1"

scalaVersion := "2.12.6"

lazy val root = project
  .in(file("."))
  .aggregate(
    common,
    dumper,
    restService
  )

lazy val common = project

lazy val dumper = project
  .dependsOn(common)

lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion = "2.5.11"

lazy val restService = project
  .dependsOn(common)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion
    )
  )
