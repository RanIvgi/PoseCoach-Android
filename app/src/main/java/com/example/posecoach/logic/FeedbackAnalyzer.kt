package com.example.posecoach.logic

import com.example.posecoach.data.FeedbackMessage
import com.example.posecoach.data.FeedbackSeverity

object FeedbackAnalyzer {

    fun analyze(
        rawFeedback: List<FeedbackMessage>,
        exerciseType: String,
        totalReps: Int
    ): List<FeedbackMessage> {
        val summarizedFeedback = mutableListOf<FeedbackMessage>()
        val feedbackTexts = rawFeedback.map { it.text }

        // 1. Analyze Depth (Squat & Pushup)
        // "Good depth!" is emitted continuously but de-duplicated in history, so roughly 1 per good rep.
        // "Go deeper on the next rep!" is emitted once per bad rep.
        val goodDepthCount = feedbackTexts.count { it == "Good depth!" }
        val badDepthCount = feedbackTexts.count { it == "Go deeper on the next rep!" }
        
        // Logic: If we have reps, check depth consistency
        if (totalReps > 0) {
            if (badDepthCount == 0 && goodDepthCount > 0) {
                summarizedFeedback.add(FeedbackMessage("Perfect! You went down deep enough on all reps.", FeedbackSeverity.INFO))
            } else if (badDepthCount > 0 && goodDepthCount > 0) {
                summarizedFeedback.add(FeedbackMessage("Inconsistent depth. You hit good depth on some reps, but missed others.", FeedbackSeverity.WARNING))
            } else if (badDepthCount > 0 && goodDepthCount == 0) {
                summarizedFeedback.add(FeedbackMessage("Depth was insufficient. Try to go lower on every rep.", FeedbackSeverity.WARNING))
            }
        }

        // 2. Analyze Specific Form Errors
        // We check for specific error strings emitted by DefaultPoseEvaluator
        analyzeError(feedbackTexts, "Knees over toes! Push hips back.", "Knees consistently extended past toes. Try sitting back more.", summarizedFeedback)
        analyzeError(feedbackTexts, "Keep your chest up and back straight.", "Tendency to lean forward. Keep chest lifted.", summarizedFeedback)
        analyzeError(feedbackTexts, "Hips sagging! Engage your core and keep body straight.", "Core stability issue: Hips were sagging. Engage your abs.", summarizedFeedback)
        analyzeError(feedbackTexts, "Hips too high! Lower them to form a straight line.", "Hips were too high. Try to keep a straight body line.", summarizedFeedback)
        
        // 3. Camera/Detection Issues
        val cameraIssues = feedbackTexts.count { it.contains("Adjust camera") || it.contains("No pose detected") }
        if (cameraIssues > 5) { 
             summarizedFeedback.add(FeedbackMessage("Camera positioning needs adjustment for better detection.", FeedbackSeverity.WARNING))
        }

        // 4. If no specific feedback was generated but reps were done
        if (summarizedFeedback.isEmpty() && totalReps > 0) {
             summarizedFeedback.add(FeedbackMessage("Great form! No major issues detected.", FeedbackSeverity.INFO))
        }
        
        return summarizedFeedback
    }

    private fun analyzeError(
        allTexts: List<String>, 
        rawErrorText: String, 
        summaryText: String, 
        outputList: MutableList<FeedbackMessage>
    ) {
        val count = allTexts.count { it == rawErrorText }
        if (count > 2) { // If it happened more than a few times (considering de-duplication)
            outputList.add(FeedbackMessage(summaryText, FeedbackSeverity.WARNING))
        } else if (count > 0) {
             // Optional: Report occasional errors? 
             // For now, let's only report if it's somewhat frequent or at least happened once clearly
             outputList.add(FeedbackMessage("Occasional issue: $rawErrorText", FeedbackSeverity.INFO))
        }
    }
}
