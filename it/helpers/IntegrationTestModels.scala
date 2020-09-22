
package helpers

import java.time.LocalDate

import helpers.IntegrationTestConstants._
import models._
import models.common._
import models.individual.business._
import models.individual.subscription._
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import utilities.AccountingPeriodUtil
import utilities.individual.Constants
import utilities.individual.Constants.GovernmentGateway._


  object IntegrationTestModels {

  import utilities.SubscriptionDataKeys._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  val testEndDate: DateModel = DateModel.dateConvert(LocalDate.now.plusYears(1).plusDays(-1))
  val testEndDateNext: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodForeignProperty = OverseasAccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyCommencementDate = PropertyCommencementDateModel(testValidStartDate)
  val testForeignPropertyCommencementDate = OverseasPropertyCommencementDateModel(testValidStartDate)
  val testInvalidCommencementDate = PropertyCommencementDateModel(testInvalidStartDate)
  val testBusinesses: Seq[SelfEmploymentData] = Seq(SelfEmploymentData("businessId", businessName = Some(testBusinessName)))
  val testInvalidForeignPropertyCommencementDate = OverseasPropertyCommencementDateModel(testInvalidStartDate)

  lazy val fullIndivSubscriptionDataBothPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(true, true, false)),
      individualIncomeSource = Some(IncomeSourceModel(true, true, false)),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyCommencementDate = Some(testPropertyCommencementDate),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )

  lazy val fullIndivSubscriptionDataAllPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(true, true, false)),
      individualIncomeSource = Some(IncomeSourceModel(true, true, true)),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyCommencementDate = Some(testPropertyCommencementDate),
      overseasPropertyCommencementDate = Some(testForeignPropertyCommencementDate),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )

  lazy val fullIndivSubscriptionDataPropertyPost: Map[String, JsValue] =
    subscriptionData(
      individualIncomeSource = Some(IncomeSourceModel(false, true, false)),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)

    )


  def subscriptionData(incomeSource: Option[IncomeSourceModel] = None,
                       individualIncomeSource: Option[IncomeSourceModel] = None,
                       selectedTaxYear: Option[AccountingYearModel] = None,
                       businessName: Option[BusinessNameModel] = None,
                       accountingMethod: Option[AccountingMethodModel] = None,
                       propertyCommencementDate: Option[PropertyCommencementDateModel] = None,
                       propertyAccountingMethod: Option[AccountingMethodPropertyModel] = None,
                       overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = None,
                       overseasPropertyCommencementDate: Option[OverseasPropertyCommencementDateModel] = None
                      ): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      individualIncomeSource.map(model => IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      selectedTaxYear.map(model => SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyCommencementDate.map(model => PropertyCommencementDate -> PropertyCommencementDateModel.format.writes(model)) ++
      propertyAccountingMethod.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      overseasPropertyAccountingMethod.map(model => OverseasPropertyAccountingMethod -> OverseasAccountingMethodPropertyModel.format.writes(model)) ++
      overseasPropertyCommencementDate.map(model => OverseasPropertyCommencementDate -> OverseasPropertyCommencementDateModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(true, false, false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)

  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(true, true, false)

  lazy val testIncomeSourceIndivProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMtdId)

}
