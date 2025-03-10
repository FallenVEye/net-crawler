package com.fallenveye

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.*
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.jsoup.Jsoup

import scala.util.{Failure, Success}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object PageCrawler {

    given system: ActorSystem[?] = ActorSystem(Behaviors.empty, "Scrapper")
    given executionContext: ExecutionContext = system.executionContext

    final case class Request(urls: List[String])
    final case class Response(urls: Map[String, String])

    given requestFormat: RootJsonFormat[Request] = jsonFormat1(Request.apply)
    given responseFormat: RootJsonFormat[Response] = jsonFormat1(Response.apply)

    private val route = path("process") {
        post {
            entity(as[Request]) { request =>
                onSuccess(processUrls(request.urls)) { urls =>
                    complete(Response(urls))
                }
            }
        }
    }

    def main(args: Array[String]): Unit = {
        val server = Http().newServerAt("localhost", 8080).bind(route)
    }

    private def processUrls(urls: List[String]): Future[Map[String, String]] = {
        Source(urls)
            .via(Flow[String].mapAsync(10)(retrieveTitle))
            .filter((url, title) => title != null)
            .toMat(
                Sink.fold[Map[String, String], (String, String)](Map.empty.withDefaultValue("")) { (map, pair) => map + (pair._1 -> pair._2) }
            )(Keep.right)
            .run()
    }

    private def retrieveTitle(url: String): Future[(String, String)] = {
        Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = url))
            .flatMap(handleRedirects)
            .flatMap(response => response.entity.toStrict(2.seconds))
            .map(entity => entity.data.utf8String)
            .map(page => (url, Jsoup.parse(page).title()))
            .transform {
                case Success((url, title)) => Try((url, title))
                case Failure(ex) => Try((url, null))
            }
    }

    private def handleRedirects(response: HttpResponse): Future[HttpResponse] = {
        response match
            case r @ HttpResponse(StatusCodes.Found, _, _, _) => followRedirect(response.getHeader("location").get().value())
            case r @ HttpResponse(StatusCodes.MovedPermanently, _, _, _) => followRedirect(response.getHeader("location").get().value())
            case r @ HttpResponse(_,_,_,_) => Future.successful(r)
    }

    private def followRedirect(url: String): Future[HttpResponse] = {
        Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = url))
            .flatMap(handleRedirects)
    }
}