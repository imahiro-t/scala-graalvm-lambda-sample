import Dependencies._
import scala.sys.process._

lazy val lambdaBuild = taskKey[Unit]("Build GraalVM native image")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "erin",
      scalaVersion := "2.13.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "scala-graalvm-lambda-sample",
    libraryDependencies ++= (circeDeps ++ awssdkDeps),
    mainClass in assembly := Some("bootstrap.Main"),
    assemblyJarName in assembly := s"scala-graalvm-lambda-sample_${version.value}.jar",
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("module-info.class") => MergeStrategy.first
      case "codegen-resources/customization.config" => MergeStrategy.concat
      case "codegen-resources/paginators-1.json" => MergeStrategy.concat
      case "codegen-resources/service-2.json" => MergeStrategy.concat
      case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    lambdaBuild := {
      assembly.value
      val jarName = (assemblyJarName in assembly).value
      ("docker run -d -it --name graalvm-builder --rm oracle/graalvm-ce:20.1.0-java11 /bin/bash" #&&
        s"docker cp target/scala-2.13/$jarName graalvm-builder:server.jar" #&&
        "time docker exec graalvm-builder gu install native-image" #&&
        "time docker exec graalvm-builder native-image --verbose --initialize-at-build-time --enable-all-security-services --no-fallback -H:+TraceClassInitialization -H:+ReportExceptionStackTraces -H:EnableURLProtocols=http,https -H:+ReportUnsupportedElementsAtRuntime --allow-incomplete-classpath -cp server.jar --no-server -jar server.jar" #&&
        "docker cp graalvm-builder:server target/bootstrap" #&&
        "docker cp graalvm-builder:/opt/graalvm-ce-java11-20.1.0/lib/libsunec.so target/libsunec.so" #&&
        "docker stop graalvm-builder" #&&
        "zip -j target/bundle.zip target/bootstrap target/libsunec.so").!
    }
  )