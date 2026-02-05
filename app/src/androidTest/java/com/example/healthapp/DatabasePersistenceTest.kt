package com.example.healthapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {

    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        firestore = Firebase.firestore
    }

    @Test
    fun verifyDatabasePersistence() = runTest {
        val testCollection = firestore.collection("debug_test_data")
        val testId = UUID.randomUUID().toString()
        val testData = hashMapOf(
            "message" to "Hello Database",
            "timestamp" to System.currentTimeMillis()
        )

        // 1. Write
        try {
            testCollection.document(testId).set(testData).await()
        } catch (e: Exception) {
            fail("Failed to write to database: ${e.message}")
        }

        // 2. Read
        try {
            val snapshot = testCollection.document(testId).get().await()
            assertTrue("Document should exist", snapshot.exists())
            assertEquals("Message should match", "Hello Database", snapshot.getString("message"))
            
            // Cleanup
            testCollection.document(testId).delete().await()
        } catch (e: Exception) {
            fail("Failed to read from database: ${e.message}")
        }
    }
}
