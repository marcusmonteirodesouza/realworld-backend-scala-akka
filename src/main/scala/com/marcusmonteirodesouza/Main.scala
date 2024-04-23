package com.marcusmonteirodesouza

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends App {
  private def startApp(): Unit = {
    implicit val system: ActorSystem[Any] = ActorSystem(Behaviors.empty, "realworld-backend-scala-akka")

    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val route = path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to Akka HTTP</h1>"))
      }
    }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  startApp()
}
