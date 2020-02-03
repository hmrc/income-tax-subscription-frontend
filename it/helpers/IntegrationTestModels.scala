
package helpers

import java.time.LocalDate

import core.Constants
import core.Constants.GovernmentGateway._
import core.models._
import core.services.CacheConstants
import helpers.IntegrationTestConstants._
import incometax.business.models._
import incometax.business.models.address.{Address, Country, ReturnedAddress}
import incometax.incomesource.models._
import incometax.subscription.models._
import incometax.util.AccountingPeriodUtil
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import usermatching.models.UserDetailsModel


object IntegrationTestModels {

  import CacheConstants._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate = DateModel.dateConvert(LocalDate.now)
  val testEndDate = DateModel.dateConvert(LocalDate.now.plusYears(1).plusDays(-1))
  val testEndDateNext = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1)
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
      rentUkProperty = Some(testRentUkProperty_property_and_other),
      areYouSelfEmployed = Some(testAreYouSelfEmployed_yes),
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
      incomeSource = Some(Property),
      rentUkProperty = Some(testRentUkProperty_property_only),
      areYouSelfEmployed = Some(testAreYouSelfEmployed_no),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )


  def keystoreData(incomeSource: Option[IncomeSourceType] = None,
                   rentUkProperty: Option[RentUkPropertyModel] = None,
                   areYouSelfEmployed: Option[AreYouSelfEmployedModel] = None,
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
      rentUkProperty.map(model => RentUkProperty -> RentUkPropertyModel.format.writes(model)) ++
      areYouSelfEmployed.map(model => AreYouSelfEmployed -> AreYouSelfEmployedModel.format.writes(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      selectedTaxYear.map(model => SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      businessPhoneNumber.map(model => BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model)) ++
      businessAddress.map(model => BusinessAddress -> Address.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyAccountingMethod.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness = Business

  lazy val testIncomeSourceProperty = Property

  lazy val testIncomeSourceBoth = Both

  lazy val testRentUkProperty_no_property = RentUkPropertyModel(No, None)
  lazy val testRentUkProperty_property_only = RentUkPropertyModel(Yes, Some(Yes))
  lazy val testRentUkProperty_property_and_other = RentUkPropertyModel(Yes, Some(No))

  lazy val testAreYouSelfEmployed_yes = AreYouSelfEmployedModel(Yes)
  lazy val testAreYouSelfEmployed_no = AreYouSelfEmployedModel(No)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testReturnedAddress = ReturnedAddress("ref", Some("id"), testAddress)

  lazy val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMTDID)

}
