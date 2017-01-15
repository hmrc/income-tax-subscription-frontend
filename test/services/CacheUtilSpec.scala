package services

import models._
import org.scalatest.Matchers._
import uk.gov.hmrc.http.cache.client.CacheMap
import util.UnitTestTrait

class CacheUtilSpec extends UnitTestTrait {

  import CacheUtil._
  import CacheUtilSpec._

  "CacheUtil" should {

    "In the respective get calls, return None if they are not in the cachemap" in {
      emptyCacheMap.getBusinessName() shouldBe None
      emptyCacheMap.getAccountingPeriod() shouldBe None
      emptyCacheMap.getContactEmail() shouldBe None
      emptyCacheMap.getIncomeType() shouldBe None
      emptyCacheMap.getTerms() shouldBe None
    }

    "In the respective get calls, return the models if they are in the cachemap" in {
      testCacheMap.getBusinessName() shouldBe Some(testBusinessName)
      testCacheMap.getAccountingPeriod() shouldBe Some(testAccountingPeriod)
      testCacheMap.getContactEmail() shouldBe Some(testContactEmail)
      testCacheMap.getIncomeType() shouldBe Some(testIncomeType)
      testCacheMap.getTerms() shouldBe Some(testTerms)
    }

    "The getSummary should populate the Summary model corectly" in {
      testCacheMap.getSummary() shouldBe
        SummaryModel(
          testAccountingPeriod,
          testBusinessName,
          testIncomeType,
          testContactEmail,
          testTerms
        )

      emptyCacheMap.getSummary() shouldBe SummaryModel()
    }

  }
}

object CacheUtilSpec {

  import CacheConstants._

  val testAccountingPeriod = AccountingPeriodModel(DateModel("01", "04", "2018"), DateModel("01", "04", "2018"))
  val testBusinessName = BusinessNameModel("test business")
  val testContactEmail = EmailModel("test@example.com")
  val testIncomeType = IncomeTypeModel("Cash")
  val testTerms = TermModel(true)

  val emptyCacheMap = CacheMap("", Map())

  val testCacheMap = CacheMap(
    "", Map(
      AccountingPeriod -> AccountingPeriodModel.format.writes(testAccountingPeriod),
      BusinessName -> BusinessNameModel.format.writes(testBusinessName),
      ContactEmail -> EmailModel.format.writes(testContactEmail),
      IncomeType -> IncomeTypeModel.format.writes(testIncomeType),
      Terms -> TermModel.format.writes(testTerms)
    )
  )

}