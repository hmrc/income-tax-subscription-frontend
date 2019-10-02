
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
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.{ConfirmAgentModel, StoredSubscription}
import incometax.util.AccountingPeriodUtil
import play.api.libs.json.{JsValue, Json}
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
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testBusinessPhoneNumber = BusinessPhoneNumberModel("01234567890")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)

  val testTerms = true

  val testBusinessStartDate = BusinessStartDateModel(testStartDate)

  lazy val testAddress = Address(Some(List("line1", "line2")), Some("zz111zz"), Some(Country("GB", "United Kingdom")))

  lazy val fullKeystoreData: Map[String, JsValue] =
    keystoreData(
      incomeSource = Some(Both),
      rentUkProperty = Some(testRentUkProperty_property_and_other),
      areYouSelfEmployed = Some(testAreYouSelfEmployed_yes),
      otherIncome = Some(testOtherIncomeNo),
      matchTaxYear = Some(testMatchTaxYearNo),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      businessPhoneNumber = Some(testBusinessPhoneNumber),
      businessAddress = Some(testAddress),
      accountingMethod = Some(testAccountingMethod),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      terms = Some(testTerms)
    )

  lazy val fullKeystoreDataBothV2: Map[String, JsValue] =
    keystoreDataV2(
      incomeSource = Some(Both),
      rentUkProperty = Some(testRentUkProperty_property_and_other),
      areYouSelfEmployed = Some(testAreYouSelfEmployed_yes),
      otherIncome = Some(testOtherIncomeNo),
      matchTaxYear = Some(testMatchTaxYearNo),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      businessPhoneNumber = Some(testBusinessPhoneNumber),
      businessAddress = Some(testAddress),
      accountingMethod = Some(testAccountingMethod),
      accountingMethodProperty = Some(testAccountingMethodProperty),
      terms = Some(testTerms)
    )

  lazy val fullKeystoreDataPropertyV2: Map[String, JsValue] =
    keystoreDataV2(
      incomeSource = Some(Property),
      rentUkProperty = Some(testRentUkProperty_property_only),
      areYouSelfEmployed = Some(testAreYouSelfEmployed_no),
      otherIncome = Some(testOtherIncomeNo),
      accountingMethodProperty = Some(testAccountingMethodProperty),
      terms = Some(testTerms)
    )


  def keystoreData(incomeSource: Option[IncomeSourceType] = None,
                   rentUkProperty: Option[RentUkPropertyModel] = None,
                   areYouSelfEmployed: Option[AreYouSelfEmployedModel] = None,
                   otherIncome: Option[YesNo] = None,
                   matchTaxYear: Option[MatchTaxYearModel] = None,
                   accountingPeriodDate: Option[AccountingPeriodModel] = None,
                   businessName: Option[BusinessNameModel] = None,
                   businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                   businessAddress: Option[Address] = None,
                   accountingMethod: Option[AccountingMethodModel] = None,
                   propertyAccountingMethod: Option[AccountingMethodPropertyModel] = None,
                   terms: Option[Boolean] = None): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceType.format.writes(model)) ++
      rentUkProperty.map(model => RentUkProperty -> RentUkPropertyModel.format.writes(model)) ++
      areYouSelfEmployed.map(model => AreYouSelfEmployed -> AreYouSelfEmployedModel.format.writes(model)) ++ //
      otherIncome.map(model => OtherIncome -> YesNo.format.writes(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      businessPhoneNumber.map(model => BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model)) ++
      businessAddress.map(model => BusinessAddress -> Address.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyAccountingMethod.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      terms.map(model => Terms -> Json.toJson(model))
  }

  def keystoreDataV2(incomeSource: Option[IncomeSourceType] = None,
                     rentUkProperty: Option[RentUkPropertyModel] = None,
                     areYouSelfEmployed: Option[AreYouSelfEmployedModel] = None,
                     otherIncome: Option[YesNo] = None,
                     matchTaxYear: Option[MatchTaxYearModel] = None,
                     accountingPeriodDate: Option[AccountingPeriodModel] = None,
                     businessName: Option[BusinessNameModel] = None,
                     businessPhoneNumber: Option[BusinessPhoneNumberModel] = None,
                     businessAddress: Option[Address] = None,
                     accountingMethod: Option[AccountingMethodModel] = None,
                     accountingMethodProperty: Option[AccountingMethodPropertyModel] = None,
                     terms: Option[Boolean] = None): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceType.format.writes(model)) ++
      rentUkProperty.map(model => RentUkProperty -> RentUkPropertyModel.format.writes(model)) ++
      areYouSelfEmployed.map(model => AreYouSelfEmployed -> AreYouSelfEmployedModel.format.writes(model)) ++ //
      otherIncome.map(model => OtherIncome -> YesNo.format.writes(model)) ++
      matchTaxYear.map(model => MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      accountingPeriodDate.map(model => AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      businessPhoneNumber.map(model => BusinessPhoneNumber -> BusinessPhoneNumberModel.format.writes(model)) ++
      businessAddress.map(model => BusinessAddress -> Address.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      terms.map(model => Terms -> Json.toJson(model)) ++
      accountingMethodProperty.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness = Business

  lazy val testIncomeSourceProperty = Property

  lazy val testIncomeSourceBoth = Both

  lazy val testRentUkProperty_no_property = RentUkPropertyModel(No, None)
  lazy val testRentUkProperty_property_only = RentUkPropertyModel(Yes, Some(Yes))
  lazy val testRentUkProperty_property_and_other = RentUkPropertyModel(Yes, Some(No))

  lazy val testAreYouSelfEmployed_yes = AreYouSelfEmployedModel(Yes)
  lazy val testAreYouSelfEmployed_no = AreYouSelfEmployedModel(No)

  lazy val testOtherIncomeNo = No

  lazy val testOtherIncomeYes = Yes

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testStartDate)

  lazy val testReturnedAddress = ReturnedAddress("ref", Some("id"), testAddress)

  lazy val testEnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMTDID)

  val testStoredSubscription = StoredSubscription(
    arn = testArn,
    incomeSource = Both,
    otherIncome = false,
    currentPeriodIsPrior = Some(true),
    accountingPeriodStart = Some(testAccountingPeriod.startDate),
    accountingPeriodEnd = Some(testAccountingPeriod.endDate),
    tradingName = Some(testBusinessName.businessName),
    cashOrAccruals = Some(testAccountingMethod.accountingMethod),
    cashOrAccrualsProperty = Some(testAccountingMethodProperty.propertyAccountingMethod)
  )

  val testConfirmAgentYes = ConfirmAgentModel(ConfirmAgentForm.option_yes)
  val testConfirmAgentNo = ConfirmAgentModel(ConfirmAgentForm.option_no)
}
