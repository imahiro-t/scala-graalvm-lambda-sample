import sbt._

object Dependencies {
  lazy val circeVersion = "0.13.0"
  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val circeDeps = Seq(circeCore, circeGeneric, circeParser)

  lazy val awssdkVersion = "2.15.7"
  val awssdkUrlConnectionClient = "software.amazon.awssdk" % "url-connection-client" % awssdkVersion
  val awssdkS3 = "software.amazon.awssdk" % "s3" % awssdkVersion
  val awssdkDeps = Seq(awssdkUrlConnectionClient, awssdkS3)
}
