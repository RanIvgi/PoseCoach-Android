# 🔍 Performance Debugging Guide for PoseCoach

## Overview

This guide explains how to use the performance debugging tools added to identify and fix the FPS bottleneck in your live session.

**Current Issue**: Live session running at ~1 FPS (previously 30 FPS)

---

## 📊 Frame Processing Pipeline

Every camera frame goes through these steps:

```
1. YUV → Bitmap Conversion (imageProxyToBitmap)     ~30-50ms  ⚠️ HIGH
2. Bitmap Rotation (rotateBitmap)                   ~10-20ms  ⚠️ MEDIUM
3. MediaPipe ML Inference (detectAsync)             ~50-100ms ⚠️ VERY HIGH
4. Pose Evaluation (angle calculations)             ~5-15ms   ⚠️ LOW
```

**Total per frame**: ~95-185ms = theoretical max of 5-10 FPS
**Your actual**: 1 FPS = something is taking ~1000ms per frame!

---

## 🛠️ Debugging Tools Installed

### Tool 1: Timing Measurements (ENABLED by default)

**What it does**: Logs precise timing for each operation in milliseconds

**Location**: `PoseEngine.kt` and `CameraViewModel.kt`

**How to view**:
1. Open Android Studio Logcat
2. Filter by tag: `PoseEngine-Timing` or `CameraViewModel-Timing`
3. You'll see output like:
   ```
   ⏱️ Frame processing: 45.23ms TOTAL | Bitmap: 28.50ms | Rotation: 8.20ms | Inference Setup: 8.53ms
   📥 MediaPipe callback: 12.45ms (landmark extraction + StateFlow emit)
   🧮 Pose evaluation: 7.82ms (angle calculations + rep counting + feedback)
   ```

**To disable timing logs**:
- In `PoseEngine.kt`, set `ENABLE_TIMING_LOGS = false`
- In `CameraViewModel.kt`, set `ENABLE_EVALUATION_TIMING = false`

---

### Tool 2: Skip-Test Flags

**What they do**: Let you skip expensive operations to isolate which one is causing the slowdown

**Available flags**:

#### In `PoseEngine.kt`:

```kotlin
companion object PerformanceFlags {
    const val SKIP_BITMAP_CONVERSION = false      // Skip YUV→Bitmap
    const val SKIP_BITMAP_ROTATION = false        // Skip Matrix rotation
    const val SKIP_MEDIAPIPE_INFERENCE = false    // Skip ML model
    const val ENABLE_TIMING_LOGS = true           // Show timing logs
    const val USE_FAKE_POSE_RESULTS = false       // Generate fake data
}
```

#### In `CameraViewModel.kt`:

```kotlin
companion object {
    const val SKIP_POSE_EVALUATION = false        // Skip angle calculations
    const val ENABLE_EVALUATION_TIMING = true     // Show timing logs
}
```

---

## 📋 Step-by-Step Debugging Process

### Step 1: Establish Baseline (CURRENT STATE)

1. **Check all flags are false** (except timing logs = true)
2. **Run the app** and start a live session
3. **Watch logcat** filtered by `PoseEngine-Timing`
4. **Record the timing** for each operation

**Expected output** (if working normally):
```
⏱️ Frame processing: 40-60ms TOTAL
📥 MediaPipe callback: 10-15ms
🧮 Pose evaluation: 5-10ms
```

**Your current output** (if broken):
```
⏱️ Frame processing: ???ms TOTAL  ← Record this!
📥 MediaPipe callback: ???ms       ← Record this!
🧮 Pose evaluation: ???ms          ← Record this!
```

---

### Step 2: Test MediaPipe Inference (MOST LIKELY CULPRIT)

MediaPipe is the heaviest operation and most likely to cause issues.

**Test instructions**:
1. Open `PoseEngine.kt`
2. Find the `PerformanceFlags` companion object
3. Set: `const val SKIP_MEDIAPIPE_INFERENCE = true`
4. **Also set**: `const val USE_FAKE_POSE_RESULTS = true` (to keep UI working)
5. Rebuild and run the app
6. Start a live session

**What to expect**:
- ✅ **If FPS jumps to 20-30**: MediaPipe is the bottleneck!
  - **Solution**: Reduce camera resolution (see Step 6)
- ❌ **If FPS stays at 1**: MediaPipe is NOT the bottleneck, continue to Step 3

---

### Step 3: Test Bitmap Conversion

**Test instructions**:
1. Open `PoseEngine.kt`
2. Set: `const val SKIP_BITMAP_CONVERSION = true`
3. **Revert**: `const val SKIP_MEDIAPIPE_INFERENCE = false`
4. Rebuild and run

**What to expect**:
- ✅ **If FPS improves significantly**: YUV→Bitmap conversion is the bottleneck
  - **Solution**: Optimize conversion method or reduce resolution
- ❌ **If FPS stays low**: Continue to Step 4

---

### Step 4: Test Bitmap Rotation

**Test instructions**:
1. Open `PoseEngine.kt`
2. Set: `const val SKIP_BITMAP_ROTATION = true`
3. **Revert**: `const val SKIP_BITMAP_CONVERSION = false`
4. Rebuild and run

**What to expect**:
- ✅ **If FPS improves**: Rotation is slow (unusual but possible)
- ❌ **If FPS stays low**: Continue to Step 5

---

### Step 5: Test Pose Evaluation

**Test instructions**:
1. Open `CameraViewModel.kt`
2. Set: `const val SKIP_POSE_EVALUATION = true`
3. Rebuild and run

**What to expect**:
- ✅ **If FPS improves**: Angle calculations are slow
  - **Solution**: Optimize DefaultPoseEvaluator
- ❌ **If FPS stays low**: Problem is elsewhere (UI rendering, device performance)

---

### Step 6: Solutions Based on Findings

#### If MediaPipe is the bottleneck (most likely):

**Solution A: Reduce Camera Resolution** (RECOMMENDED - gives 4-6x speedup)
```kotlin
// In CameraViewModel.kt, find imageAnalysis builder:
val imageAnalysis = ImageAnalysis.Builder()
    .setTargetResolution(Size(640, 480))  // Add this line
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
```

**Solution B: Frame Skipping** (process every 2nd or 3rd frame)
```kotlin
// In CameraViewModel.kt, in the analyzer:
private var frameCounter = 0
it.setAnalyzer(cameraExecutor) { imageProxy ->
    frameCounter++
    if (frameCounter % 2 == 0) {  // Process every 2nd frame
        poseEngine.detectPose(imageProxy, _cameraState.value.isFront())
    }
    imageProxy.close()
}
```

#### If Bitmap Conversion is the bottleneck:

**Solution: Already optimized to 75% JPEG quality**
- Could try 50% quality (faster but less accurate)
- Or implement native YUV→RGB conversion (complex)

#### If Pose Evaluation is the bottleneck:

**Solution: Optimize angle calculation code**
- Review `DefaultPoseEvaluator.kt`
- Cache repeated calculations
- Reduce trigonometric operations

---

## 📱 Logcat Filtering Tips

**Best filters to use**:

1. **See all timing logs**:
   - Tag: `Timing`
   - This shows both `PoseEngine-Timing` and `CameraViewModel-Timing`

2. **See only PoseEngine timing**:
   - Tag: `PoseEngine-Timing`

3. **See warnings about skipped operations**:
   - Level: `Warn`
   - Will show messages like "⚠️ SKIP_MEDIAPIPE_INFERENCE=true"

4. **See FPS updates**:
   - Tag: `PoseEngine`
   - Look for device detection and initialization logs

---

## 🎯 Quick Test Checklist

Run through this checklist quickly to find the bottleneck:

- [ ] **Baseline**: Check timing logs with all flags = false
- [ ] **Test 1**: Set `SKIP_MEDIAPIPE_INFERENCE = true` + `USE_FAKE_POSE_RESULTS = true`
  - [ ] FPS improved? → MediaPipe is the bottleneck
  - [ ] FPS same? → Continue
- [ ] **Test 2**: Set `SKIP_BITMAP_CONVERSION = true` (revert Test 1)
  - [ ] FPS improved? → Bitmap conversion is the bottleneck
  - [ ] FPS same? → Continue
- [ ] **Test 3**: Set `SKIP_BITMAP_ROTATION = true` (revert Test 2)
  - [ ] FPS improved? → Rotation is the bottleneck
  - [ ] FPS same? → Continue
- [ ] **Test 4**: Set `SKIP_POSE_EVALUATION = true` in CameraViewModel.kt
  - [ ] FPS improved? → Evaluation is the bottleneck
  - [ ] FPS same? → Check UI rendering or device performance

---

## 🚨 Important Notes

1. **Always rebuild after changing flags** - Kotlin constants are inlined at compile time
2. **Test on the same device** - Different devices have different performance
3. **Watch the timing logs** - They'll tell you exactly which operation is slow
4. **One flag at a time** - Don't enable multiple skip flags simultaneously
5. **Revert flags when done** - Set everything back to false before committing

---

## 📞 Need Help?

If you're still stuck after going through this guide:

1. **Share the timing log output** from Step 1 (baseline measurements)
2. **Share which skip-test improved FPS** (if any)
3. **Check if this happens on emulator or real device** (emulators are much slower)
4. **Check device temperature** (thermal throttling can cause slowdowns)

---

## 🎓 Understanding the Results

**Normal performance** (what you should see):
- Bitmap conversion: 10-20ms
- Rotation: 5-10ms
- MediaPipe inference: 30-80ms
- Pose evaluation: 3-8ms
- **Total**: 48-118ms per frame = **8-20 FPS**

**Your issue** (1 FPS = 1000ms per frame):
- Something is taking 10-50x longer than it should
- Use the skip tests to find which operation is the culprit
- Most likely: MediaPipe is stuck or receiving huge images

**After optimization** (target):
- With 640x480 resolution: 15-30ms for MediaPipe
- With frame skipping: Effective 30-60 FPS
- **Target**: 20-30 FPS smooth performance

---

## 🔧 Quick Fix Reference

**If MediaPipe is slow** → Reduce resolution to 640x480
**If Bitmap conversion is slow** → Already at 75% JPEG, try 50%
**If Rotation is slow** → Skip rotation or optimize matrix operations
**If Evaluation is slow** → Optimize DefaultPoseEvaluator calculations
**If nothing helps** → Check device specs, thermal throttling, or background processes

Good luck debugging! 🚀
