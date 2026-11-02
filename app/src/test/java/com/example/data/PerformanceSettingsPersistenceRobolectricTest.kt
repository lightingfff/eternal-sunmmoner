package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestSaveManager
import java.io.File
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceSettingsPersistenceRobolectricTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    File(context.filesDir, "eternal_summoner_saves").deleteRecursively()
  }

  @Test
  fun `render mode and opt-in probe survive restart without changing guest identity`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val original = manager.startOrResumeGuestSession("Performance")
    manager.updateSettings(manager.settings.value.copy(performanceMode = PerformanceMode.HIGH, performanceOverlay = true))

    val restarted = LocalGuestSaveManager(context)
    assertEquals(original.guestPlayerId, restarted.guestSnapshot.value!!.guestPlayerId)
    assertEquals(PerformanceMode.HIGH, restarted.settings.value.performanceMode)
    assertEquals(true, restarted.settings.value.performanceOverlay)
  }

  @Test
  fun `old session with unknown render mode defaults to balanced and disabled probe`() = runTest {
    val manager = LocalGuestSaveManager(context)
    val original = manager.startOrResumeGuestSession("Legacy")
    // Session settings are loaded separately from the guest progression snapshot.
    val directory = File(context.filesDir, "eternal_summoner_saves")
    val sessionFile = directory.listFiles()!!.first { file ->
      file.extension == "json" && JSONObject(file.readText()).has("sessionMode")
    }
    val session = JSONObject(sessionFile.readText())
    session.getJSONObject("settings").put("performanceMode", "FUTURE_UNSUPPORTED_MODE").remove("performanceOverlay")
    sessionFile.writeText(session.toString())

    val restarted = LocalGuestSaveManager(context)
    assertEquals(original.guestPlayerId, restarted.guestSnapshot.value!!.guestPlayerId)
    assertEquals(PerformanceMode.BALANCED, restarted.settings.value.performanceMode)
    assertEquals(false, restarted.settings.value.performanceOverlay)
  }
}
