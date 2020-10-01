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


import models.common.{AccountingMethodModel, IncomeSourceModel}
import models.individual.business.{OverseasPropertyCommencementDateModel, PropertyCommencementDateModel}
import play.api.libs.functional.~
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.SubscriptionDetailsService
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

object Answers {

  import utilities.SubscriptionDataUtil._

  val incomeSourceModelAnswer: Answer[IncomeSourceModel] = SingleAnswer[IncomeSourceModel](
    retrieveAnswer = _.getIncomeSource,
    ifEmpty = Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
  )

  val optAccountingMethodAnswer: Answer[Option[AccountingMethodModel]] = OptionalAnswer(
    retrieveAnswer = _.getAccountingMethod
  )

  val optPropertyCommencementDateAnswer: Answer[Option[PropertyCommencementDateModel]] = OptionalAnswer(
    retrieveAnswer = _.getPropertyCommencementDate
  )

  val optForeignPropertyCommencementDateAnswer: Answer[Option[OverseasPropertyCommencementDateModel]] = OptionalAnswer(
    retrieveAnswer = _.getOverseasPropertyCommencementDate
  )

}

trait RequireAnswer {

  val subscriptionDetailsService: SubscriptionDetailsService

  def require[A](answer: Answer[A])(f: A => Future[Result])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Result] = {
    subscriptionDetailsService.fetchAll() flatMap {
      cacheMap =>
        answer(cacheMap) match {
          case Right(answers) => f(answers)
          case Left(result) => Future.successful(result)
        }
    }
  }

}
