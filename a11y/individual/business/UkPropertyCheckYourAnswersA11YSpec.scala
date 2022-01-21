
package individual.business

import config.AppConfig
import models.common.PropertyModel
import models.{Cash, DateModel}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.individual.incometax.business.PropertyCheckYourAnswers

class UkPropertyCheckYourAnswersA11YSpec extends AnyWordSpecLike
  with Matchers
  with GuiceOneAppPerSuite
  with AccessibilityMatchers {

  implicit val request: Request[_] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "the page" must {
    val page: PropertyCheckYourAnswers = app.injector.instanceOf[PropertyCheckYourAnswers]
    val content = page(
      viewModel = PropertyModel(
        accountingMethod = Some(Cash),
        startDate = Some(DateModel(
          day = "20",
          month = "03",
          year = "1990"
        ))),
      postAction = controllers.individual.business.routes.PropertyCheckYourAnswersController.submit(),
      backUrl = "/test-back-url"
    )

    "pass accessibility checks" in {
      content.toString() must passAccessibilityChecks
    }
  }

}
