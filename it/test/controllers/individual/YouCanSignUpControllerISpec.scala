package controllers.individual

import helpers.ComponentSpecBase
import play.api.test.Helpers._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.WsTestClient
import scala.concurrent.{ExecutionContext, Future}

import scala.concurrent.ExecutionContext.Implicits.global

class YouCanSignUpControllerISpec extends ComponentSpecBase {

  s"GET ${routes.YouCanSignUpController.show.url}" must {
    "return OK with the page content" in {

      WsTestClient.withClient { client =>
        val fullUrl = s"http://localhost:9000${routes.YouCanSignUpController.show.url}"

        val response: Future[WSResponse] = client.url(fullUrl)
          .withHttpHeaders("Cookie" -> "session-id=fake-session-id")
          .get()

        response.map { wsResponse =>
          wsResponse.status mustBe OK
        }
      }
    }
  }
}
