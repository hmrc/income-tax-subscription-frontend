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

import models.IncomeSourcesStatus
import models.common.OverseasProperty.OVERSEAS_PROPERTY
import models.common.SelfEmployed.SELF_EMPLOYED
import models.common.UkProperty.UK_PROPERTY
import models.common.{BusinessIncomeSource, OverseasProperty, SelfEmployed, UkProperty}
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{FormError, Mapping}
import uk.gov.hmrc.http.InternalServerException

object BusinessIncomeSourceMapping {
  def apply(errorKey: String, incomeSourcesStatus: IncomeSourcesStatus): Mapping[BusinessIncomeSource] = of(new Formatter[BusinessIncomeSource] {
    def messageKey: String = {
      (incomeSourcesStatus.selfEmploymentAvailable, incomeSourcesStatus.ukPropertyAvailable, incomeSourcesStatus.overseasPropertyAvailable) match {
        case (false, false, false) =>
          throw new InternalServerException("[BusinessIncomeSourceMapping][apply] - Unexpected state of income sources available")
        case _ => errorKey
      }
    }

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BusinessIncomeSource] = {
      data.get(key) match {
        case Some(SELF_EMPLOYED) if incomeSourcesStatus.selfEmploymentAvailable => Right(SelfEmployed)
        case Some(UK_PROPERTY) if incomeSourcesStatus.ukPropertyAvailable => Right(UkProperty)
        case Some(OVERSEAS_PROPERTY) if incomeSourcesStatus.overseasPropertyAvailable => Right(OverseasProperty)
        case _ => Left(Seq(FormError(key = key, message = messageKey)))
      }
    }

    override def unbind(key: String, value: BusinessIncomeSource): Map[String, String] = {
      val stringValue = value match {
        case SelfEmployed => SELF_EMPLOYED
        case UkProperty => UK_PROPERTY
        case OverseasProperty => OVERSEAS_PROPERTY
      }

      Map(key -> stringValue)
    }
  })
}
