
package helpers

import java.time.LocalDate

import helpers.IntegrationTestConstants._
import models._
import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel, OverseasAccountingMethodPropertyModel}
import models.individual.business._
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription._
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import utilities.individual.Constants
import utilities.individual.Constants.GovernmentGateway._
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}


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
  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(Yes)
  val testMatchTaxYearNo: MatchTaxYearModel = MatchTaxYearModel(No)
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
  val testInvalidCommencementDate = PropertyCommencementDateModel(testInvalidStartDate)
  val testBusinesses: Seq[SelfEmploymentData] = Seq(SelfEmploymentData("businessId", businessName = Some(testBusinessName)))

  lazy val fullIndivSubscriptionDataBothPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(Both),
      individualIncomeSource = Some(IncomeSourceModel(true, true, false)),
      matchTaxYear = Some(testMatchTaxYearNo),
      selectedTaxYear = Some(testAccountingYearCurrent),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyCommencementDate = Some(testPropertyCommencementDate),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )

  lazy val fullIndivSubscriptionDataPropertyPost: Map[String, JsValue] =
    subscriptionData(
      individualIncomeSource = Some(IncomeSourceModel(false, true, false)),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)

    )


  def subscriptionData(incomeSource: Option[IncomeSourceType] = None,
                       individualIncomeSource: Option[IncomeSourceModel] = None,
                       matchTaxYear: Option[MatchTaxYearModel] = None,
                       selectedTaxYear: Option[AccountingYearModel] = None,
                       accountingPeriodDate: Option[AccountingPeriodModel] = None,
                       businessName: Option[BusinessNameModel] = None,
                       accountingMethod: Option[AccountingMethodModel] = None,
                       propertyCommencementDate: Option[PropertyCommencementDateModel] = None,
                       propertyAccountingMethod: Option[AccountingMethodPropertyModel] = None,
                       overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = None
                      ): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceType.format.writes(model)) ++
      individualIncomeSource.map(model => IndividualIncomeSource -> IncomeSourceModel.format.writes(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      selectedTaxYear.map(model => SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyCommencementDate.map(model => PropertyCommencementDate -> PropertyCommencementDateModel.format.writes(model)) ++
      propertyAccountingMethod.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      overseasPropertyAccountingMethod.map(model => OverseasPropertyAccountingMethod -> OverseasAccountingMethodPropertyModel.format.writes(model))

  }

  lazy val testIncomeSourceBusiness: Business.type = Business

  lazy val testIncomeSourceProperty: Property.type = Property

  lazy val testIncomeSourceBoth: Both.type = Both

  lazy val testIncomeSourceIndivProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)


  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMtdId)

}
