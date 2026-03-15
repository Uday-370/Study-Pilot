package com.example.studysmart.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

object AppReviewManager {
    fun askForReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the Review Info object
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not tell us if they actually
                    // reviewed or not (for privacy), so we just assume success!
                }
            } else {
                // There was some problem, log or handle the error code.
                task.exception?.printStackTrace()
            }
        }
    }
}