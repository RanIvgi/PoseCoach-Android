# Recomposition Tracking Guide

## 🎯 Purpose
This guide explains how to capture and analyze recomposition logs to identify UI performance bottlenecks.

## 📱 How to Run the Test

1. **Build and run the app** in Android Studio
2. **Open the Camera screen** and grant camera permission
3. **Wait ~5 seconds** for camera to initialize
4. **Start a session** (press Play button)
5. **Let it run for 30 seconds** while moving in front of the camera
6. **Stop the session** (press Stop button)

## 🔍 How to Capture Logs

### In Android Studio:

1. Open **Logcat** panel (bottom of screen)
2. In the filter box, type: `Recomposition-Track`
3. Click the **pause** button to stop log updates
4. Select all logs (Cmd+A / Ctrl+A)
5. Copy (Cmd+C / Ctrl+C)
6. Paste the logs back here

## 📊 What to Look For

### Expected Recomposition Counts (30-second session)

| Component | Good (✅) | Bad (❌) | Why |
|-----------|----------|---------|-----|
| **CameraScreen** | 5-15 times | 100+ times | Should only recompose on state changes |
| **CameraControls** | 5-15 times | 500+ times | Should NOT recompose on every FPS update |
| **PoseOverlay** | 5-15 times | 500+ times | Should be isolated via StateFlow |
| **FeedbackDisplay** | 20-50 times | N/A | Expected - feedback changes during exercise |
| **ExerciseSelector** | 1-3 times | 50+ times | Only when user changes exercise |
| **TargetRepsSelector** | 1-3 times | 50+ times | Only when user changes target reps |
| **CameraPreview** | 2-5 times | 20+ times | Only on camera state changes |

### 🚨 Red Flags to Watch For

**CRITICAL ISSUES:**
- Any component showing **500+ recompositions** → MAJOR PERFORMANCE PROBLEM
- `CameraControls` with **300+ recompositions** → FPS updates causing cascade
- `PoseOverlay` with **300+ recompositions** → StateFlow isolation failed

**MODERATE ISSUES:**
- Any component showing **50-200 recompositions** → Needs optimization
- Components recomposing more than once per second

**NORMAL BEHAVIOR:**
- `FeedbackDisplay` recomposing frequently (feedback text changes)
- Single-digit recompositions for most components

## 📋 Example Good Logs

```
Recomposition-Track: CameraScreen: recomposed 8 times
Recomposition-Track: CameraControls: recomposed 12 times
Recomposition-Track: PoseOverlay: recomposed 6 times
Recomposition-Track: FeedbackDisplay: recomposed 34 times
Recomposition-Track: ExerciseSelector: recomposed 2 times
```

## 📋 Example Bad Logs (PROBLEM!)

```
Recomposition-Track: CameraScreen: recomposed 847 times  ❌ WAY TOO HIGH!
Recomposition-Track: CameraControls: recomposed 845 times ❌ FPS UPDATES!
Recomposition-Track: PoseOverlay: recomposed 723 times   ❌ NOT ISOLATED!
```

## 🎯 What to Copy

After running the test, copy **ALL** the `Recomposition-Track` logs and paste them here. The logs will look like:

```
2025-11-29 12:30:15.123  1234-1234  Recomposition-Track  com.example.posecoach  D  CameraScreen: recomposed 1 times
2025-11-29 12:30:15.456  1234-1234  Recomposition-Track  com.example.posecoach  D  CameraControls: recomposed 1 times
...
```

## 🔧 What Happens Next

Once you paste the logs, I will:
1. **Analyze** which components are recomposing too frequently
2. **Identify** the root cause (FPS updates, state changes, etc.)
3. **Implement** targeted fixes for the problematic components
4. **Verify** the fixes resolved the 2-4 FPS issue

## ⚡ Quick Reference

**How many pose results in 30 seconds?** ~900 (at 30 FPS)

**Good recomposition count:** < 20 times
**Bad recomposition count:** > 100 times
**CRITICAL:** > 500 times (means component is recomposing on EVERY frame!)

---

**Ready?** Run the test and paste the `Recomposition-Track` logs here! 🚀
