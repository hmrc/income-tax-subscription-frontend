/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.utils

import agent.services.CacheConstants
import models.agent.AccountingMethodModel
import models.individual.business.MatchTaxYearModel
import models.individual.subscription.Both
import models.{Cash, Yes}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.functional.~
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import services.agent.KeystoreService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RequirementsSpec extends WordSpecLike with MustMatchers with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val testResult: Result = Redirect("/custom-result")
  val testResult2: Result = Redirect("/custom-result2")

  val testSingleAnswer: Answer[AccountingMethodModel] = SingleAnswer[AccountingMethodModel](
    retrieveAnswer = _.getEntry[AccountingMethodModel]("testKey"),
    ifEmpty = testResult
  )
  val testOptionalAnswer: Answer[Option[AccountingMethodModel]] = OptionalAnswer[Option[AccountingMethodModel]](
    retrieveAnswer = _.getEntry[AccountingMethodModel]("testKey2")
  )
  val testCompositeAnswer: Answer[AccountingMethodModel ~ AccountingMethodModel] = {
    SingleAnswer(_.getEntry[AccountingMethodModel]("testKey"), testResult) and SingleAnswer(_.getEntry[AccountingMethodModel]("testKey2"), testResult2)
  }
  val testModel: AccountingMethodModel = AccountingMethodModel(Cash)

  class Setup extends BaseRequireAnswer {
    override val keystoreService: KeystoreService = mock[KeystoreService]
    override val noDataRedirectLocation: Call = controllers.agent.business.routes.BusinessAccountingMethodController.show()
  }

  "SingleAnswer" must {
    "return the data type of the answer" when {
      "the data exists in the provided CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey" -> Json.toJson(testModel)))
        testSingleAnswer(cacheMap) mustBe Right(testModel)
      }
    }
    "return a result" when {
      "the data does not exist in the CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey2" -> Json.toJson(testModel)))
        testSingleAnswer(cacheMap) mustBe Left(testResult)
      }
    }
  }

  "OptionalAnswer" must {
    "return some data" when {
      "the data exists in the provided CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey2" -> Json.toJson(testModel)))
        testOptionalAnswer(cacheMap) mustBe Right(Some(testModel))
      }
    }
    "return none" when {
      "the data exists in the provided CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey" -> Json.toJson(testModel)))
        testOptionalAnswer(cacheMap) mustBe Right(None)
      }
    }
  }

  "CompositeAnswer" must {
    "return both pieces of data" when {
      "the data exists in the CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey" -> Json.toJson(testModel), "testKey2" -> Json.toJson(testModel)))
        testCompositeAnswer(cacheMap) mustBe Right(new ~(testModel, testModel))
      }
    }
    "return the first answers result" when {
      "the data for the first answer doesn't exist in the CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey2" -> Json.toJson(testModel)))
        testCompositeAnswer(cacheMap) mustBe Left(testResult)
      }
      "both pieces of data are not present in the CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map())
        testCompositeAnswer(cacheMap) mustBe Left(testResult)
      }
    }
    "return the second answers result" when {
      "the data for the second answer doesn't exist in the CacheMap" in {
        val cacheMap: CacheMap = CacheMap("testId", Map("testKey" -> Json.toJson(testModel)))
        testCompositeAnswer(cacheMap) mustBe Left(testResult2)
      }
    }
  }

  "require" must {
    "redirect to the no data redirect location" when {
      "there is no data in keystore" in new Setup {
        when(keystoreService.fetchAll()(any())) thenReturn Future.successful(None)
        val result: Future[Result] = require(testSingleAnswer) { answer =>
          Future.successful(Ok(Json.toJson(answer)))
        }

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(noDataRedirectLocation.url)
      }
    }
    "return a result" when {
      "the required data is available" in new Setup {
        when(keystoreService.fetchAll()(any()))
          .thenReturn(Future.successful(Some(CacheMap("testId", Map("testKey" -> Json.toJson(testModel))))))
        val result: Future[Result] = require(testSingleAnswer) { answer =>
          Future.successful(Ok(Json.toJson(answer)))
        }

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testModel)
      }
      "the required data is not available in the returned CacheMap" in new Setup {
        when(keystoreService.fetchAll()(any()))
          .thenReturn(Future.successful(Some(CacheMap("testId", Map("testKey2" -> Json.toJson(testModel))))))
        val result: Future[Result] = require(testSingleAnswer) { answer =>
          Future.successful(Ok(Json.toJson(answer)))
        }

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/custom-result")
      }
    }
  }

  "Agent - incomeSourceTypeAnswer" must {
    "return an income source type" when {
      "present in the cache map" in {
        val cacheMap: CacheMap = CacheMap("testId", Map(CacheConstants.IncomeSource -> Json.toJson(Both)))
        AgentAnswers.incomeSourceTypeAnswer(cacheMap) mustBe Right(Both)
      }
    }
    "return a redirect to the income source controller" when {
      "not present in the cache map" in {
        val cacheMap: CacheMap = CacheMap("testId", Map())
        AgentAnswers.incomeSourceTypeAnswer(cacheMap) mustBe Left(Redirect(controllers.agent.routes.IncomeSourceController.show()))
      }
    }
  }

  "Agent - matchTaxYearAnswer" must {
    "return a match tax year model" when {
      "present in the cache map" in {
        val cacheMap: CacheMap = CacheMap("testId", Map(CacheConstants.MatchTaxYear -> Json.toJson(MatchTaxYearModel(Yes))))
        AgentAnswers.matchTaxYearAnswer(cacheMap) mustBe Right(MatchTaxYearModel(Yes))
      }
    }
    "return a redirect to the match tax year controller" when {
      "not present in the cache map" in {
        val cacheMap: CacheMap = CacheMap("testId", Map())
        AgentAnswers.matchTaxYearAnswer(cacheMap) mustBe Left(Redirect(controllers.agent.business.routes.MatchTaxYearController.show()))
      }
    }
  }

  "Agent - optAccountingMethodAnswer" must {
    "return some accounting method model" when {
      "present in the cache map" in {
        val cacheMap: CacheMap = CacheMap("testId", Map(CacheConstants.AccountingMethod -> Json.toJson(AccountingMethodModel(Cash))))
        AgentAnswers.optAccountingMethodAnswer(cacheMap) mustBe Right(Some(AccountingMethodModel(Cash)))
      }
    }
    "return none" when {
      "not present in the cache map" in {
        val cacheMap: CacheMap = CacheMap("testId", Map())
        AgentAnswers.optAccountingMethodAnswer(cacheMap) mustBe Right(None)
      }
    }
  }

}
