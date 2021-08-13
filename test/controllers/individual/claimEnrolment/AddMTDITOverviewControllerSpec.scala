/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.individual.claimEnrolment

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.ClaimEnrolment
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.NotFoundException
import utilities.ITSASessionKeys
import views.html.individual.incometax.claimEnrolment.AddMTDITOverview
import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}

import scala.concurrent.Future

class AddMTDITOverviewControllerSpec extends ControllerBaseSpec with MockAuditingService
  with FeatureSwitching {

  val addMTDITOverview : AddMTDITOverview = mock[AddMTDITOverview]

  override val controllerName: String = "AddMTDITOverviewController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAddMTDITOverviewController.show,
    "submit" -> TestAddMTDITOverviewController.submit
  )


  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  object TestAddMTDITOverviewController extends AddMTDITOverviewController(

    addMTDITOverview,
    mockAuditingService,
    mockAuthService
  )

  "Calling the show action of the AddMTDITOverviewController" when {
    "claimEnrolement is enabled" should {

      enable(ClaimEnrolment)

      lazy val result = TestAddMTDITOverviewController.show()(claimEnrolmentRequest)

      "return ok (200)" in {

        enable(ClaimEnrolment)

        when(addMTDITOverview(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(HtmlFormat.empty)

        status(result) must be(Status.OK)
        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))
        session(result).get(ITSASessionKeys.JourneyStateKey) must be(Some(ClaimEnrolmentJourney.name))

      }
    }
  }

  "Calling the show action of the AddMTDITOverviewController when ClaimEnrolment is not enabled" should {

    lazy val result = TestAddMTDITOverviewController.show()(claimEnrolmentRequest)

    "return a NotFoundException" in {

    val ex = intercept[NotFoundException](await(result))
    ex.getMessage mustBe ("[AddMTDITOverviewController][show] - ClaimEnrolment Enabled feature switch not enabled")
    }
  }

  "Calling the submit action of the TestAddMTDITOverviewController " should {

    enable(ClaimEnrolment)

    def callSubmit(): Future[Result] = TestAddMTDITOverviewController.submit()(claimEnrolmentRequest)

      s"redirect to '${controllers.individual.claimEnrolment.routes.AddMTDITOverviewController.show().url}'" in {

        enable(ClaimEnrolment)

        status(callSubmit()) must be(Status.SEE_OTHER)
        redirectLocation(callSubmit()) mustBe Some(controllers.individual.claimEnrolment.routes.AddMTDITOverviewController.show().url)


    }
  }
}
