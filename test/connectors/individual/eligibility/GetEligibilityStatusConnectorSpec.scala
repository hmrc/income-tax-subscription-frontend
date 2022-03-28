/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.individual.eligibility

import connectors.individual.eligibility.mocks.TestGetEligibilityStatusConnector
import org.scalatest.matchers.should.Matchers._
import org.scalatest.{EitherValues, OptionValues}
import utilities.individual.TestConstants._

class GetEligibilityStatusConnectorSpec extends TestGetEligibilityStatusConnector with EitherValues with OptionValues {
  "GetEligibilityStatusConnector.getEligibilityStatus" should {
    "GET to the correct url" in {
      TestGetEligibilityStatusConnector.eligibilityUrl(testUtr) should endWith(s"/income-tax-subscription-eligibility/eligibility/$testUtr")
    }
  }

}
