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

package core

import models.DateModel

object Constants {

  val mtdItsaEnrolmentName = "HMRC-MTD-IT"
  val mtdItsaEnrolmentIdentifierKey = "MTDITID"
  val ninoEnrolmentName = "HMRC-NI"
  val ninoEnrolmentIdentifierKey = "NINO"
  val utrEnrolmentName = "IR-SA"
  val utrEnrolmentIdentifierKey = "UTR"

  object GovernmentGateway {
    val GGProviderId = "GovernmentGateway"
    val MTDITID = "MTDITID"
    val NINO = "NINO"
    val ggPortalId = "Default"
    val ggServiceName = "HMRC-MTD-IT"
    val ggFriendlyName = "Making Tax Digital Income Tax Self-Assessment enrolment"
  }

  val preferencesServiceKey = "mtdfbit"

  val crystallisationTaxYearStart = DateModel("6", "4", "2018")

}
