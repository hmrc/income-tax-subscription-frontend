
package helpers

import java.time.LocalDate

import utilities.individual.Constants.GovernmentGateway._
import helpers.IntegrationTestConstants._
import models._
import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business._
import models.individual.business.address.{Address, Country, ReturnedAddress}
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription._
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import utilities.AccountingPeriodUtil
import utilities.individual.Constants


object IntegrationTestModels {

  import utilities.CacheConstants._

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
  val testBusinessPhoneNumber = BusinessPhoneNumberModel("01234567890")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)

  val testBusinessStartDate = BusinessStartDateModel(testStartDate)

  lazy val testAddress = Address(Some(List("line1", "line2")), Some("zz111zz"), Some(Country("GB", "United Kingdom")))

  lazy val fullKeystoreDataBothPost: Map[String, JsValue] =
    keystoreData(
      incomeSource = Some(Both),
      individualIncomeSource = Some(IncomeSourceModel(true, true)),
      matchTaxYear = Some(testMatchTaxYearNo),
      selectedTaxYear = Some(testAccountingYearCurrent),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      businessPhoneNumber = Some(testBusinessPhoneNumber),
      businessAddress = Some(testAddress),
      accountingMethod = Some(testAccountingMethod),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )

  lazy val fullKeystoreDataPropertyPost: Map[String, JsValue] =
    keystoreData(
      individualIncomeSource = Some(IncomeSourceModel(false, true)),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )


  def keystoreData(incomeSource: Option[IncomeSourceType] = None,
                   individualIncomeSource: Option[IncomeSourceModel] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   selectedTaxYear: Option[AccountingYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   propertyAccountingMethod: Option[AccountingMethodPropertyModel] = None
                  ): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceType.format.writes(model)) ++
      individualIncomeSource.map(model => IndividualIncomeSource -> IncomeSourceModel.format.writes(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      selectedTaxYear.map(model => SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      businessPhoneNumber.map(model => BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model)) ++
      businessAddress.map(model => BusinessAddress -> Address.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyAccountingMethod.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: Business.type = Business

  lazy val testIncomeSourceProperty: Property.type = Property

  lazy val testIncomeSourceBoth: Both.type = Both

  lazy val testIncomeSourceIndivProperty: IncomeSourceModel = IncomeSourceModel(false, true)


  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testReturnedAddress = ReturnedAddress("ref", Some("id"), testAddress)

  lazy val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMtdId)

}
