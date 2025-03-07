package controllers.individual

import controllers.ControllerSpec
import views.html.individual.CaptureConsent

class CaptureConsentControllerSpec extends ControllerSpec {
  with MockSignUpJourneyRefiner,
  with MockSessionDataService,
  with MockCaptureConsent {

  }

  object TestCaptureConsentController extends CaptureConsentController(
    mock[CaptureConsent],
    mockSessionDataService,
    identify = fakeIdentifierAction,
    journeyRefiner = fakeSignUpJourneyRefiner,
    view = mockCaptureConsent
  )

}
