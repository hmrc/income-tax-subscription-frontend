/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent

import connectors.httpparser.GetSessionDataHttpParser
import controllers.ControllerSpec
import controllers.agent.actions.mocks.{MockConfirmationJourneyRefiner, MockIdentifierAction}
import models.common.AccountingYearModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.{Current, Next, No, Yes}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks._
import uk.gov.hmrc.http.InternalServerException
import views.agent.mocks.MockSignUpConfirmation

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerSpec
  with MockSignUpConfirmation
  with MockIdentifierAction
  with MockConfirmationJourneyRefiner
  with MockSubscriptionDetailsService
  with MockSessionDataService {

  "show" must {
    "return OK with the page content" when {
      "the user is mandated for the current tax year" in {
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current, confirmed = true, editable = false)))
        mockGetMandationService(Mandated, Voluntary)
        mockFetchSoftwareStatus(Right(Some(No)))
        mockView(
          mandatedCurrentYear = true,
          mandatedNextYear = false,
          taxYearSelectionIsNext = false,
          name = clientDetails.name,
          nino = clientDetails.formattedNino,
          usingSoftware = false
        )

        val result: Future[Result] = TestConfirmationController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user is mandated for the next tax year" in {
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next, confirmed = true, editable = false)))
        mockGetMandationService(Voluntary, Mandated)
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockView(
          mandatedCurrentYear = false,
          mandatedNextYear = true,
          taxYearSelectionIsNext = true,
          name = clientDetails.name,
          nino = clientDetails.formattedNino,
          usingSoftware = true
        )

        val result: Future[Result] = TestConfirmationController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "the user has selected to sign up for the next tax year" in {
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Next, confirmed = true)))
        mockGetMandationService(Voluntary, Voluntary)
        mockFetchSoftwareStatus(Right(Some(Yes)))
        mockView(
          mandatedCurrentYear = false,
          mandatedNextYear = false,
          taxYearSelectionIsNext = true,
          name = clientDetails.name,
          nino = clientDetails.formattedNino,
          usingSoftware = true
        )

        val result: Future[Result] = TestConfirmationController.show(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "throw an internal server exception" when {
      "there was a problem retrieving the software status" in {
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current, confirmed = true)))
        mockGetMandationService(Voluntary, Voluntary)
        mockFetchSoftwareStatus(Left(GetSessionDataHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        intercept[InternalServerException](await(TestConfirmationController.show(request)))
          .message mustBe s"[ConfirmationController][show] - failure retrieving software status - UnexpectedStatusFailure($INTERNAL_SERVER_ERROR)"

      }
    }
  }

  "submit" must {
    "redirect to the add another client route" in {
      mockDeleteAll()

      val result: Future[Result] = TestConfirmationController.submit(request.withMethod("POST"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AddAnotherClientController.addAnother().url)
    }
  }

  object TestConfirmationController extends ConfirmationController(
    mockSignUpConfirmation,
    fakeIdentifierAction,
    fakeConfirmationJourneyRefiner,
    mockSubscriptionDetailsService,
    mockMandationStatusService,
    mockSessionDataService
  )

}
