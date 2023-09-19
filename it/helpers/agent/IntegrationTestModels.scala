
package helpers.agent

import helpers.IntegrationTestModels.crypto
import models._
import models.common._
import models.common.business._
import models.usermatching.UserDetailsModel
import utilities.AccountingPeriodUtil

import java.time.LocalDate

object IntegrationTestModels {
  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  val testEndDate: DateModel = helpers.IntegrationTestModels.testEndDate
  val testEndDateNext: DateModel = AccountingPeriodUtil.getCurrentTaxYear.endDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y: DateModel = AccountingPeriodUtil.getCurrentTaxYear.endDate.plusYears(1)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingYearCurrentConfirmed: AccountingYearModel = AccountingYearModel(Current, confirmed = true)
  val testAccountingYearNextConfirmed: AccountingYearModel = AccountingYearModel(Next, confirmed = true)
  private val testPropertyStartDateModel: PropertyStartDateModel = PropertyStartDateModel(DateModel("05", "04", "2017"))
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName: BusinessNameModel = BusinessNameModel("test business")
  val testBusinessTrade: BusinessTradeNameModel = BusinessTradeNameModel("test trade")
  val testBusinessAddress: BusinessAddressModel = BusinessAddressModel(
    Address(
      lines = Seq(
        "1 long road",
        "lonely town",
        "quiet county"
      ),
      postcode = Some("ZZ1 1ZZ")
    )
  )
  val testAccountingMethod: AccountingMethodModel = AccountingMethodModel(Cash)
  val testAccountingMethodProperty: AccountingMethodPropertyModel = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodForeignProperty: OverseasAccountingMethodPropertyModel = OverseasAccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testValidStartDate)
  val testOverseasPropertyStartDate: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testValidStartDate)
  val testInvalidOverseasPropertyStartDate: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testInvalidStartDate)
  val testInvalidPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testInvalidStartDate)
  val testBusinesses: Seq[SelfEmploymentData] = Seq(SelfEmploymentData(
    id = "12345",
    businessStartDate = Some(BusinessStartDate(DateModel("5", "4", "2017"))),
    businessName = Some(testBusinessName.encrypt(crypto.QueryParameterCrypto)),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(BusinessAddressModel(Address(Seq("1 long road", "lonely town", "quiet county"), Some("ZZ1 1ZZ"))).encrypt(crypto.QueryParameterCrypto))
  ))

  val testBusinessTradeName: BusinessTradeNameModel = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate: BusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel(Address(Seq("line 1", "line 2"), Some("TF2 1PF")))
  val testId = "testId"

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  lazy val testClientDetails: UserDetailsModel = helpers.IntegrationTestModels.testUserDetails

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData(
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1", "line 2"), Some("TF2 1PF"))))
    ))

  val testFullOverseasPropertyModel: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    count = Some(1),
    startDate = Some(testPropertyStartDateModel.startDate),
    confirmed = true
  )

  val testFullPropertyModel: PropertyModel = PropertyModel(
    accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
    count = Some(1),
    startDate = Some(testPropertyStartDate.startDate),
    confirmed = true
  )
}
