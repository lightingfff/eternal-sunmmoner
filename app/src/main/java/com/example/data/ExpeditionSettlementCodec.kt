package com.example.data

import com.example.data.firebase.ExpeditionOutcomeType
import com.example.data.firebase.ExpeditionSettlementResult
import com.example.data.model.CourierOutcome
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** One format for atomic guest receipts and immutable cloud receipts.
 * Guest replay protection covers only the last [MAX_RECORDS] settlements. Once an ID is evicted,
 * submitting it is treated as a new settlement. Cloud receipts remain until explicitly removed.
 */
object ExpeditionSettlementCodec {
  const val MAX_RECORDS = 32
  const val MAX_JSON_LENGTH = 8192

  fun validateAttemptId(value: String): String {
    val uuid = UUID.fromString(value)
    require(uuid.version() == 4 && uuid.variant() == 2 && uuid.toString() == value) {
      "Mã lần thám hiểm phải là UUID v4 chuẩn."
    }
    return value
  }

  fun encode(result: ExpeditionSettlementResult): String = JSONObject().apply {
    put("version", 1)
    put("victory", result.victory)
    put("floorNumber", result.floorNumber)
    put("goldEarned", result.goldEarned)
    put("manaEarned", result.manaEarned)
    put("shardsEarned", result.shardsEarned)
    put("oreEarned", result.oreEarned)
    put("essenceEarned", result.essenceEarned)
    put("injuredHeroNames", JSONArray(result.injuredHeroNames))
    put("leveledUpHeroNames", JSONArray(result.leveledUpHeroNames))
    put("lootedItemName", result.lootedItemName ?: JSONObject.NULL)
    put("outcomeType", result.outcomeType.name)
    put("bonusDiscoveryGold", result.bonusDiscoveryGold)
    put("bonusDiscoveryMana", result.bonusDiscoveryMana)
    put("discoveredNodesCount", result.discoveredNodesCount)
    put("courierOutcome", result.courierOutcome?.name ?: JSONObject.NULL)
    put("courierForgeUnlocked", result.courierForgeUnlocked)
    put("courierMemoryHeroName", result.courierMemoryHeroName ?: JSONObject.NULL)
  }.toString().also { require(it.length <= MAX_JSON_LENGTH) { "Biên nhận thám hiểm quá lớn." } }

  fun decode(json: String): ExpeditionSettlementResult {
    require(json.length <= MAX_JSON_LENGTH)
    val obj = JSONObject(json)
    require(obj.getInt("version") == 1)
    fun names(key: String): List<String> = obj.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
    fun nullable(key: String): String? = if (obj.isNull(key)) null else obj.getString(key)
    return ExpeditionSettlementResult(
      victory = obj.getBoolean("victory"), floorNumber = obj.getInt("floorNumber"),
      goldEarned = obj.getLong("goldEarned"), manaEarned = obj.getLong("manaEarned"),
      shardsEarned = obj.getLong("shardsEarned"), oreEarned = obj.getLong("oreEarned"),
      essenceEarned = obj.getLong("essenceEarned"), injuredHeroNames = names("injuredHeroNames"),
      leveledUpHeroNames = names("leveledUpHeroNames"), lootedItemName = nullable("lootedItemName"),
      outcomeType = ExpeditionOutcomeType.valueOf(obj.getString("outcomeType")),
      bonusDiscoveryGold = obj.getLong("bonusDiscoveryGold"), bonusDiscoveryMana = obj.getLong("bonusDiscoveryMana"),
      discoveredNodesCount = obj.getInt("discoveredNodesCount"),
      courierOutcome = nullable("courierOutcome")?.let(CourierOutcome::valueOf),
      courierForgeUnlocked = obj.getBoolean("courierForgeUnlocked"), courierMemoryHeroName = nullable("courierMemoryHeroName"),
    )
  }

  fun append(ledger: Map<String, ExpeditionSettlementResult>, id: String, result: ExpeditionSettlementResult): Map<String, ExpeditionSettlementResult> {
    validateAttemptId(id)
    require(id !in ledger)
    return (ledger + (id to result)).entries.toList().takeLast(MAX_RECORDS).associate { it.toPair() }
  }
}
