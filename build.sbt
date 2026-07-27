version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  "dev.zio"                     %% "zio"          % "2.1.26",
  "com.beachape"                %% "enumeratum"   % "1.9.8",
  "com.chuusai"                 %% "shapeless"    % "2.3.13",
  "com.softwaremill.sttp.tapir" %% "tapir-core"   % "1.13.27",
  "dev.zio"                     %% "zio-prelude"  % "1.0.0-RC47",
  "dev.zio"                     %% "zio-test-sbt" % "2.1.26" % Test
)
