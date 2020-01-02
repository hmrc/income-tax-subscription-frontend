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

package testonly.forms

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

import _root_.testonly.models.UnauthorisedAgentSubmissionModel
import core.forms.prevalidation.PreprocessedForm
import core.forms.validation.Constraints._
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.utils.ConstraintUtil._
import core.forms.validation.utils.MappingUtil._
import core.forms.validation.utils.Patterns
import incometax.subscription.models.IncomeSourceType
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Valid}

object UnauthorisedAgentSubmissionForm {

  val agentArn = "agentArn"
  val clientNino = "clientNino"
  val incomeSource = "incomeSource"

  val option_business = IncomeSourceType.business
  val option_property = IncomeSourceType.property
  val option_both = IncomeSourceType.both

  val emptyArn: Constraint[String] = constraint[String](
    arn => {
      lazy val emptyArn = ErrorMessageFactory.error("Enter the agent's ARN")
      if (arn.isEmpty) emptyArn else Valid
    }
  )

  val invalidArn: Constraint[String] = constraint[String](
    arn => {
      lazy val invalidArn = ErrorMessageFactory.error("Enter a valid ARN")
      if (Patterns.validText(arn.trim)) Valid else invalidArn
    }
  )

  val sourceEmpty: Constraint[String] = constraint[String](
    source => {
      lazy val emptySource = ErrorMessageFactory.error("error.income_source.empty")
      if (source.isEmpty) emptySource else Valid
    }
  )

  val sourceInvalid: Constraint[String] = constraint[String](
    source => {
      lazy val invalidSource = ErrorMessageFactory.error("error.income_source.invalid")
      source match {
        case `option_business` | `option_property` | `option_both` => Valid
        case _ => invalidSource
      }
    }
  )

  val unauthorisedAgentSubmissionValidationForm = Form(
    mapping(
      agentArn -> oText.toText.verifying(emptyArn andThen invalidArn),
      clientNino -> oText.toText.verifying(emptyNino andThen validateNino),
      incomeSource -> oText.toText.verifying(sourceEmpty andThen sourceInvalid)
    )(UnauthorisedAgentSubmissionModel.apply)(UnauthorisedAgentSubmissionModel.unapply)
  )

  import core.forms.prevalidation.CaseOption._
  import core.forms.prevalidation.TrimOption._

  val unauthorisedAgentSubmissionForm = PreprocessedForm(
    validation = unauthorisedAgentSubmissionValidationForm,
    trimRules = Map(agentArn -> bothAndCompress, clientNino -> bothAndCompress),
    caseRules = Map(agentArn -> upper, clientNino -> upper)
  )

}

// $COVERAGE-ON$
