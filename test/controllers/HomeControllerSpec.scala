package controllers

import auth.authenticatedFakeRequest
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._


class HomeControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "HomeControllerSpec"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "index" -> TestHomeController.index()
  )

  object TestHomeController extends HomeController(
    MockBaseControllerConfig,
    messagesApi
  )

  "Calling the index action of the Home controller with an authorised user" should {

    lazy val result = TestHomeController.index()(authenticatedFakeRequest())

    s"get a redirection (303) to ${controllers.routes.IncomeSourceController.showIncomeSource().url}" in {

      status(result) must be(Status.SEE_OTHER)

      redirectLocation(result).get mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }
  }

  authorisationTests

}
