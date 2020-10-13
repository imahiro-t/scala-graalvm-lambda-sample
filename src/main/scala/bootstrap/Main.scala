package bootstrap

import java.net._
import java.net.http._
import java.time.Duration
import java.nio.charset.StandardCharsets.UTF_8

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;

final case class Input(bucket: String)
final case class Output(keys: Seq[String])

object Main {

  def main(args: Array[String]): Unit = {
    val context = System.getenv()
    val runtime = context.get("AWS_LAMBDA_RUNTIME_API")
    val http = HttpClient.newBuilder().build()
    val s3 = S3Client.builder()
      .region(Region.AP_NORTHEAST_1)
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .httpClient(UrlConnectionHttpClient.builder().build())
      .build()

    while (true) {
      try {
        val response = http.send(
          HttpRequest.newBuilder().uri(URI.create(s"http://$runtime/2018-06-01/runtime/invocation/next")).GET().build(),
          HttpResponse.BodyHandlers.ofString(UTF_8)
        )
        val requestId = response.headers().map().get("lambda-runtime-aws-request-id").asScala.head
        Try {
          decode[Input](response.body()) match {
            case Right(Input(bucket)) =>
              val listObjects = ListObjectsRequest
                .builder()
                .bucket(bucket)
                .build()
              Output(s3.listObjects(listObjects).contents().asScala.map(_.key()).toSeq).asJson.noSpaces
            case Left(error) =>
              throw new Exception(error)
          }
        } match {
          case Success(response) =>
            http.send(
              HttpRequest.newBuilder().uri(URI.create(s"http://$runtime/2018-06-01/runtime/invocation/$requestId/response")).POST(HttpRequest.BodyPublishers.ofString(response)).build(),
              HttpResponse.BodyHandlers.ofString(UTF_8)
            )
          case Failure(e) =>
            http.send(
              HttpRequest.newBuilder().uri(URI.create(s"http://$runtime/2018-06-01/runtime/invocation/$requestId/error")).POST(HttpRequest.BodyPublishers.ofString(e.getMessage)).build(),
              HttpResponse.BodyHandlers.ofString(UTF_8)
            )
        }
      } catch {
        case e: Exception =>
          System.err.println(e.getMessage)
      }
    }
  }
}
