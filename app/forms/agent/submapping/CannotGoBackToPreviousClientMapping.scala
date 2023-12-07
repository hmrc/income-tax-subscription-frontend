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

package forms.agent.submapping

import models.CannotGoBack
import models.CannotGoBack._
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.{FormError, Mapping}

object CannotGoBackToPreviousClientMapping {

  val agentServiceAccount = AgentServiceAccount.toString
  val reenterClientDetails = ReenterClientDetails.toString
  val signUpAnotherClient = SignUpAnotherClient.toString

  def apply(cannotGoBackToPreviousClientEmpty: Invalid): Mapping[CannotGoBack] = of(new Formatter[CannotGoBack] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], CannotGoBack] = {
      data.get(key) match {
        case Some(`agentServiceAccount`) => Right(AgentServiceAccount)
        case Some(`reenterClientDetails`) => Right(ReenterClientDetails)
        case Some(`signUpAnotherClient`) => Right(SignUpAnotherClient)
        case _ =>
          Left(cannotGoBackToPreviousClientEmpty.errors.map(e => FormError(key, e.message, e.args)))
      }
    }

    override def unbind(key: String, value: CannotGoBack): Map[String, String] = {
      val stringValue = value match {
        case AgentServiceAccount => AgentServiceAccount.toString
        case ReenterClientDetails => ReenterClientDetails.toString
        case SignUpAnotherClient => SignUpAnotherClient.toString
      }

      Map(key -> stringValue)
    }
  })

}
