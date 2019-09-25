
package helpers.servicemocks

import java.time.LocalDate

object AccountingPeriodStub extends WireMockMethods{

  def stubEligibleAccountingPeriodSuccess(startDate: LocalDate, endDate: LocalDate)(eligible: Boolean): Unit =
    stubEligibleAccountingPeriodSuccess(startDate, endDate)(eligible)

}
