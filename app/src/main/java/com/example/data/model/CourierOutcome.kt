package com.example.data.model

/** Terminal result of the authored Lost Courier escort; absent for ordinary expeditions. */
enum class CourierOutcome {
  RESCUED,
  SUPPLIES_RECOVERED,
  RESCUED_WITHOUT_SUPPLIES,
  FAILED,
}
