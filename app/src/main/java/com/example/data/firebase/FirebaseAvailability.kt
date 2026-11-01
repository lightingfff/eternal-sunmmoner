package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Central safety gate for Firebase runtime availability.
 * APK can build with google-services.json missing (WARN+passthrough), but at runtime
 * FirebaseApp.getInstance() will throw IllegalStateException and cause immediate crash
 * if we call Firebase.auth / FirebaseFirestore.getInstance without check.
 *
 * This helper isolates that check so MainActivity can fall back to 100% offline guest mode.
 */
object FirebaseAvailability {
  private const val TAG = "FirebaseAvail"
  @Volatile private var cached: Boolean? = null

  fun isFirebaseReady(context: Context): Boolean {
    cached?.let { return it }
    val ready = try {
      // FirebaseApp.getApps returns empty if google-services.json never configured
      // FirebaseApp.getInstance() throws IllegalStateException if not initialized
      val apps = FirebaseApp.getApps(context.applicationContext)
      if (apps.isEmpty()) {
        Log.w(TAG, "FirebaseApp not initialized — no google-services.json config found. Running in offline guest mode.")
        false
      } else {
        // Also verify default app can be retrieved
        FirebaseApp.getInstance()
        true
      }
    } catch (e: Exception) {
      Log.w(TAG, "Firebase unavailable: ${e.message} — offline guest mode", e)
      false
    }
    cached = ready
    return ready
  }

  fun resetForTest() { cached = null }
}
