
package controllers.agent

import helpers.ComponentSpecBase
import play.api.http.Status.SEE_OTHER
import play.api.libs.ws.WSResponse

class LanguageSwitchControllerISpec extends ComponentSpecBase {

  val testRefererRoute: String = "/test/referer/route"

  "GET /client/language/cymraeg" should {
    "update the PLAY_LANG cookie to cy and return the user where they were when a REFERER is in the headers" in {
      lazy val resultCy: WSResponse = getWithHeaders("/client/language/cymraeg", "REFERER" -> testRefererRoute)
      resultCy.headers.isDefinedAt("Set-Cookie") mustBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") mustBe true
      resultCy must have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "update the PLAY_LANG cookie to cy and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultCy: WSResponse = getWithHeaders("/client/language/cymraeg")
      resultCy.headers.isDefinedAt("Set-Cookie") mustBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") mustBe true
      resultCy must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.routes.HomeController.home.url)
      )
    }
  }

  "GET /client/language/english" should {
    "update the PLAY_LANG cookie to en and return the user where they were when a REFERER is in the headers" in {
      lazy val resultEn: WSResponse = getWithHeaders("/client/language/english", "REFERER" -> testRefererRoute)
      resultEn.headers.isDefinedAt("Set-Cookie") mustBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") mustBe true
      resultEn must have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "update the PLAY_LANG cookie to en and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultEn: WSResponse = getWithHeaders("/client/language/english")
      resultEn.headers.isDefinedAt("Set-Cookie") mustBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") mustBe true
      resultEn must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.routes.HomeController.home.url)
      )
    }
  }
}
