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

import models.agent.AccountingMethodModel
import models.individual.business.MatchTaxYearModel
import models.individual.subscription.IncomeSourceType
import play.api.libs.functional.~
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.agent.KeystoreService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}

case class CompositeAnswer[A, B](answerA: Answer[A], answerB: Answer[B]) extends Answer[A ~ B] {
  def apply(cacheMap: CacheMap): Either[Result, A ~ B] = answerA(cacheMap).right.flatMap { answerA =>
    answerB(cacheMap).right.map { answerB =>
      new ~(answerA, answerB)
    }
  }
}

case class SingleAnswer[A](retrieveAnswer: CacheMap => Option[A], ifEmpty: Result) extends Answer[A] {
  def apply(cacheMap: CacheMap): Either[Result, A] = retrieveAnswer(cacheMap).toRight(ifEmpty)
}

case class OptionalAnswer[A](retrieveAnswer: CacheMap => A) extends Answer[A] {
  def apply(cacheMap: CacheMap): Either[Result, A] = Right(retrieveAnswer(cacheMap))
}

trait Answer[A] {
  def apply(cacheMap: CacheMap): Either[Result, A]

  def and[B](other: Answer[B]): Answer[A ~ B] = CompositeAnswer(this, other)
}

// If both keystore services, cacheutils and models were combined, that would be simpler as we wouldn't need seperate agent and individual answers
object AgentAnswers {

  import agent.services.CacheUtil._

  val incomeSourceTypeAnswer: Answer[IncomeSourceType] = SingleAnswer(
    retrieveAnswer = _.getIncomeSource(),
    ifEmpty = Redirect(controllers.agent.routes.IncomeSourceController.show().url)
  )

  val matchTaxYearAnswer: Answer[MatchTaxYearModel] = SingleAnswer(
    retrieveAnswer = _.getMatchTaxYear(),
    ifEmpty = Redirect(controllers.agent.business.routes.MatchTaxYearController.show())
  )

  val optAccountingMethodAnswer: Answer[Option[AccountingMethodModel]] = OptionalAnswer(
    retrieveAnswer = _.agentGetAccountingMethod()
  )

}

// Requires the merging of individual and agent keystore services
object IndividualAnswers {

  //individual specific requirements if required

}

// Requires the merging of individual and agent keystore services
trait IndividualRequireAnswer extends BaseRequireAnswer {

  val noDataRedirectLocation: Call = controllers.usermatching.routes.UserDetailsController.show()

}

trait AgentRequireAnswer extends BaseRequireAnswer {

  val noDataRedirectLocation: Call = controllers.agent.matching.routes.ClientDetailsController.show()

}

trait BaseRequireAnswer {

  val keystoreService: KeystoreService
  val noDataRedirectLocation: Call

  def require[A](answer: Answer[A])(f: A => Future[Result])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Result] = {
    keystoreService.fetchAll() flatMap {
      case None => Future.successful(Redirect(noDataRedirectLocation))
      case Some(cacheMap) => answer(cacheMap) match {
        case Right(answers) => f(answers)
        case Left(result) => Future.successful(result)
      }
    }
  }

}
