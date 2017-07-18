/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import common.Constants._
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.http.NotFoundException

import scala.concurrent.Future

object AuthPredicates extends Results {
  def defaultPredicates(result: Enrolments => Future[Result]): (Enrolments) => Future[Result] = ninoPredicate(mtdidPredicate(result))

  def confirmationPredicate(result: Enrolments => Future[Result]): (Enrolments) => Future[Result] = ninoPredicate(enrolledPredicate(result))

  def ninoPredicate(result: Enrolments => Future[Result]): Enrolments => Future[Result] =
    enrolments =>
      if (enrolments.getEnrolment(ninoEnrolmentName).nonEmpty) result(enrolments)
      else Future.successful(noNino)

  def mtdidPredicate(result: Enrolments => Future[Result]): Enrolments => Future[Result] = {
    enrolments =>
      if (enrolments.getEnrolment(mtdItsaEnrolmentName).isEmpty) result(enrolments)
      else Future.successful(alreadyEnrolled)
  }

  def enrolledPredicate(result: Enrolments => Future[Result]): Enrolments => Future[Result] = {
    enrolments =>
      if (enrolments.getEnrolment(mtdItsaEnrolmentName).nonEmpty) result(enrolments)
      else Future.failed(new NotFoundException("AuthPredicates.enrolledPredicate"))
  }

  private val noNino: Result = Redirect(controllers.routes.NoNinoController.showNoNino())

  private val alreadyEnrolled: Result = Redirect(controllers.routes.AlreadyEnrolledController.enrolled())
}
