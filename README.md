# PoseCoach Android App - Foundation

A modular Android application for real-time workout form analysis using Google's MediaPipe Pose Landmarker model. Built with Kotlin and Jetpack Compose.

## 🎯 Project Overview

PoseCoach helps users perform exercises correctly by analyzing their body pose in real-time and providing instant feedback on their form.

**Current Status:** Foundation complete with modular architecture ready for 3-person team development.

### Architecture

The app is divided into three main modules, each owned by a different team member:

```
com.example.posecoach/
├── ui/              # Student 1 - UI & Front-End (Jetpack Compose)
├── pose/            # Student 2 - ML Model Integration (MediaPipe + CameraX)
└── logic/           # Student 3 - Pose Evaluation & Feedback Logic
```

---

## 👥 Team Responsibilities

### Student 1 - UI / Front-End Developer

**Package:** `com.example.posecoach.ui`

**Your files:**
- `CameraScreen.kt` - Main UI screen with camera preview
- `PoseOverlay.kt` - Skeleton visualization overlay
- `theme/Theme.kt` - Material Design theme
- `theme/Type.kt` - Typography configuration
- `theme/Shape.kt` - Shape configuration

**What you'll work on:**
1. ✅ Camera preview display (DONE - foundation ready)
2. ✅ Skeleton overlay drawing (DONE - foundation ready)
3. ✅ Feedback message display (DONE - foundation ready)
4. ✅ GPU/CPU toggle button (DONE - allows switching between delegates)
5. ✅ GPU/CPU status indicator (DONE - shows current delegate with colored badge)
6. 🔨 Exercise selection UI (dropdown/buttons)
7. 🔨 Rep counter display
8. 🔨 Start/Stop session controls
9. 🔨 Settings screen
10. 🔨 Session summary screen
11. 🔨 Animations and transitions
12. 🔨 Sound/haptic feedback

**Key TODO items in your code:**
- Search for `// TODO (Student 1):` or `Student 1 TODO` in your files
- Start with `CameraScreen.kt` - add exercise selection
- Enhance `PoseOverlay.kt` - add animations for smooth transitions
- Customize theme colors in `Theme.kt`

**Data you receive:**
- `PoseResult` from Student 2 (contains landmarks and pose data)
- `FeedbackMessage` from Student 3 (contains text and severity)

**Useful Compose resources:**
- [Jetpack Compose basics](https://developer.android.com/jetpack/compose/tutorial)
- [Material Design in Compose](https://developer.android.com/jetpack/compose/themes)
- [Animations](https://developer.android.com/jetpack/compose/animation)

---

### Student 2 - ML Model Integration

**Package:** `com.example.posecoach.pose`

**Your files:**
- `PoseEngine.kt` - MediaPipe integration and camera frame processing

**What you'll work on:**
1. ✅ MediaPipe Pose Landmarker setup (DONE - foundation ready)
2. ✅ CameraX image analysis pipeline (DONE - foundation ready)
3. ✅ ImageProxy to Bitmap conversion (DONE - YUV→RGB with NV21 format)
4. ✅ GPU vs CPU delegate with auto-detection (DONE - detects emulator vs real device)
5. ✅ Automatic CPU fallback on GPU failure (DONE - prevents crashes)
6. ✅ Bitmap rotation handling (DONE - handles device orientation)
7. ✅ Comprehensive logging system (DONE - logs at all critical points)
8. ✅ Enhanced emulator detection (DONE - recognizes all emulator types)
9. 🔨 Confidence threshold tuning
10. 🔨 Landmark smoothing/filtering
11. 🔨 Frame rate optimization
12. 🔨 Multi-person detection (optional)

**CRITICAL TODO - Image Conversion:**
The `imageProxyToBitmap()` function in `PoseEngine.kt` is a placeholder. You MUST implement proper YUV_420_888 to RGB conversion. Options:

```kotlin
// Option 1: Simple approach (if available in your CameraX version)
@androidx.camera.core.ExperimentalGetImage
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    return imageProxy.toBitmap()
}

// Option 2: Manual YUV to RGB conversion
// Search for "YUV_420_888 to Bitmap Android" for implementation examples
```

**Key TODO items:**
- Search for `// TODO (Student 2):` in `PoseEngine.kt`
- Priority 1: Fix `imageProxyToBitmap()` 
- Priority 2: Tune confidence thresholds (lines 83-85)
- Priority 3: Test GPU vs CPU delegate performance

**Performance targets:**
- Target: 15-30 FPS on mid-range devices
- Latency: <100ms per frame
- Use `STRATEGY_KEEP_ONLY_LATEST` to avoid backlog

**MediaPipe resources:**
- [MediaPipe Pose](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
- [Pose landmark indices](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker#pose_landmarker_model)

---

### Student 3 - Pose Evaluation & Feedback Logic

**Package:** `com.example.posecoach.logic`

**Your files:**
- `PoseEvaluator.kt` - Interface defining evaluation contract
- `DefaultPoseEvaluator.kt` - Implementation with exercise-specific logic
- `AngleThresholds` object - Configurable thresholds

**What you'll work on:**
1. ✅ Evaluation interface (DONE - foundation ready)
2. 🔨 Squat form evaluation (example started, needs completion)
3. 🔨 Push-up form evaluation (placeholder, needs implementation)
4. 🔨 Lunge form evaluation (placeholder, needs implementation)
5. 🔨 Rep counting algorithm
6. 🔨 Movement state tracking
7. 🔨 Symmetry checking (left vs right)
8. 🔨 Metrics collection (max depth, speed, etc.)

**Key evaluation techniques:**

```kotlin
// 1. Calculate joint angles
val kneeAngle = poseResult.calculateAngle(
    PoseLandmarkIndex.HIP,
    PoseLandmarkIndex.KNEE,
    PoseLandmarkIndex.ANKLE
)

// 2. Check landmark positions
val knee = poseResult.getLandmark(PoseLandmarkIndex.LEFT_KNEE)
val toe = poseResult.getLandmark(PoseLandmarkIndex.LEFT_FOOT_INDEX)
if (knee.x > toe.x) {
    // Knee past toe - bad form!
}

// 3. Track movement over time
// Store previous poses to detect "up" vs "down" for rep counting
```

**Key TODO items:**
- Search for `// TODO (Student 3):` in your files
- Start with `evaluateSquat()` - complete the example logic
- Implement rep counting in `getRepCount()`
- Add more feedback messages with specific guidance

**Exercise evaluation priorities:**
1. Squats (partially implemented)
2. Push-ups
3. Lunges
4. Add more exercises as needed

**Landmark indices:** See `PoseLandmarkIndex.kt` for all 33 landmarks (0-32)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android device/emulator with API 21+ (Android 5.0+)
- Camera permission required

### Setup
1. Clone this repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Download MediaPipe model (should be in `app/src/main/assets/pose_landmarker_full.task`)
5. Run on device (emulator camera may not work well)

### First Run
1. Grant camera permission when prompted
2. You should see:
   - Live camera preview
   - Skeleton overlay (if pose detected)
   - Feedback message (e.g., "Pose detected!")
   - FPS counter (top-right)
   - GPU/CPU status indicator (top-right, below FPS)
   - Camera switch button (bottom-right)
   - GPU/CPU toggle button (bottom-right, above camera button)

### UI Controls

**Top Right Corner:**
- **FPS Counter** - Shows current frames per second (green text)
- **GPU/CPU Badge** - Indicates current MediaPipe delegate:
  - 🟢 Green "GPU" = Using GPU acceleration (faster on real devices)
  - 🔵 Cyan "CPU" = Using CPU processing (required for emulators)

**Bottom Right Corner:**
- **Memory Icon Button** - Toggle between GPU and CPU delegates
  - Automatically switches to CPU if GPU fails
  - GPU may not work on emulators (auto-detected)
  - Reinitializes MediaPipe engine when toggled
- **Camera Icon Button** - Switch between front and rear cameras

---

## 📁 Project Structure

```
app/src/main/java/com/example/posecoach/
│
├── MainActivity.kt              # App entry point
│
├── data/                        # Shared data models
│   ├── PoseLandmark.kt         # Single landmark (x, y, z, visibility)
│   ├── PoseLandmarkIndex.kt    # Constants for 33 landmark indices
│   ├── PoseResult.kt           # Complete pose detection result
│   ├── FeedbackMessage.kt      # Feedback from evaluator to UI
│   └── CameraState.kt          # Front/Rear camera state
│
├── ui/                         # Student 1 - UI Components
│   ├── CameraScreen.kt         # Main screen with camera + overlay
│   ├── CameraViewModel.kt      # State management
│   ├── PoseOverlay.kt          # Skeleton drawing
│   └── theme/                  # Material theme files
│
├── pose/                       # Student 2 - ML Integration
│   └── PoseEngine.kt           # MediaPipe + CameraX integration
│
└── logic/                      # Student 3 - Evaluation Logic
    ├── PoseEvaluator.kt        # Interface
    ├── DefaultPoseEvaluator.kt # Implementation
    └── AngleThresholds         # Configurable thresholds
```

---

## 🔄 Data Flow

```
Camera Frame (CameraX)
    ↓
[Student 2] PoseEngine.detectPose()
    ↓
PoseResult (33 landmarks)
    ↓
[Student 3] PoseEvaluator.evaluate()
    ↓
FeedbackMessage (text + severity)
    ↓
[Student 1] UI Display (CameraScreen + PoseOverlay)
```

---

## 🛠️ Key Technologies

- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **CameraX** - Camera preview and analysis
- **MediaPipe Tasks Vision** - Pose detection ML model
- **Kotlin Coroutines + Flow** - Asynchronous programming

---

## 📝 Development Guidelines

### Code Style
- Use meaningful variable names
- Add comments for complex logic
- Keep functions focused and small
- Follow Kotlin naming conventions

### Git Workflow
1. Each student works in their package primarily
2. Create feature branches for new features
3. Test before committing
4. Don't break other students' code!

### Communication
- Use TODO comments with your name: `// TODO (Student 1): Add this feature`
- Update this README when adding major features
- Share difficult bugs in team chat

---

## 🔧 Technical Improvements (Completed)

### Image Processing Pipeline
**Problem:** MediaPipe was receiving blank images, resulting in no pose detection.

**Solution Implemented:**
- ✅ Proper YUV_420_888 → RGB conversion using NV21 format
- ✅ Uses `YuvImage` and `BitmapFactory` for reliable conversion
- ✅ Handles all ImageProxy plane data correctly (Y, U, V buffers)
- ✅ Logs bitmap dimensions for debugging: `"Converted ImageProxy to Bitmap: WxH"`

### Rotation Handling
**Problem:** Device orientation caused misaligned pose detection.

**Solution Implemented:**
- ✅ Extracts rotation degrees from ImageProxy metadata
- ✅ Applies Matrix rotation transformation to bitmap
- ✅ Passes rotation info to MediaPipe via `MPImage.Builder`
- ✅ Properly handles 0°, 90°, 180°, 270° rotations

### GPU/CPU Delegate Management
**Problem:** GPU delegate crashes on emulators, causing app to fail.

**Solution Implemented:**
- ✅ Automatic device detection (real device vs emulator)
- ✅ Checks multiple emulator identifiers: `goldfish`, `ranchu`, `sdk_gphone`, etc.
- ✅ Automatic CPU fallback when GPU initialization fails
- ✅ User-controllable toggle button for manual switching
- ✅ Logs device detection results and delegate status

### Overlay Mirroring Fix
**Problem:** Overlay appeared reversed (moving left showed right movement).

**Solution Implemented:**
- ✅ Disabled front camera mirroring in `PoseOverlay`
- ✅ CameraX preview already handles mirroring correctly
- ✅ Overlay now tracks movements accurately

### Comprehensive Logging System
**Logs added for debugging at critical points:**

```
PoseEngine: Device detection - isRealDevice: true/false
PoseEngine: Attempting to initialize with GPU/CPU delegate
PoseEngine: Converted ImageProxy to Bitmap: 640x480
PoseEngine: Sending frame to MediaPipe at time=123456, rotation=90
PoseEngine: Pose detected. Landmarks count: 33
PoseEngine: No pose detected in this frame.
PoseEngine: PoseLandmarker initialized successfully with GPU delegate
CameraViewModel: New PoseResult received: hasPose=true, landmarks=33
PoseOverlay: Drawing overlay with 33 landmarks
```

---

## 🐛 Troubleshooting

### Camera not working
- Check camera permission in app settings
- Try physical device instead of emulator
- Ensure no other app is using the camera

### Pose not detected
- Ensure good lighting
- Stand 2-3 meters from camera
- Show full body in frame
- Check logcat for: `"No pose detected in this frame"`
- Verify bitmap conversion: `"Converted ImageProxy to Bitmap"`

### Build errors
- Sync Gradle files
- Clean and rebuild project
- Check all dependencies are downloaded
- Invalidate caches and restart

### Low FPS / Performance Issues
- Use CPU delegate on emulators (automatically detected)
- Try toggling GPU/CPU with the Memory icon button
- Check FPS counter in top-right (target: 15-30 FPS)
- Reduce camera resolution if needed
- Use `STRATEGY_KEEP_ONLY_LATEST` in ImageAnalysis

### GPU Crashes on Emulator
**Automatic Fix Applied:**
- App now detects emulators and uses CPU delegate by default
- If GPU fails, automatically falls back to CPU
- Check logs for: `"GPU initialization failed, falling back to CPU"`
- Use the toggle button to manually switch if needed

### Overlay Appears Reversed
**Fixed:** Mirroring has been disabled in PoseOverlay. If you still see issues:
- Check that you're using the latest code
- Verify `mirrorForFrontCamera = false` in `PoseOverlay.kt`

### Blank/Black Image to MediaPipe
**Fixed:** YUV→RGB conversion is now properly implemented. If issues persist:
- Check logs for: `"Converted ImageProxy to Bitmap: WxH"`
- Ensure bitmap dimensions are non-zero
- Verify rotation is being applied: `"rotation=90"`

---

## 🎯 Milestone Checklist

### Phase 1: Foundation (COMPLETE ✅)
- [x] Project structure
- [x] Data models
- [x] Basic UI with camera
- [x] MediaPipe integration skeleton
- [x] Evaluation interface

### Phase 2: Core Features (IN PROGRESS 🔨)
- [x] Student 2: Complete image conversion (YUV→RGB with NV21)
- [x] Student 2: Implement rotation handling
- [x] Student 2: Add GPU/CPU delegate switching
- [x] Student 1: Add GPU/CPU toggle button and indicator
- [x] Student 2: Add comprehensive logging system
- [x] Student 1: Fix overlay mirroring issue
- [ ] Student 3: Implement squat evaluation
- [ ] Student 1: Add exercise selection UI
- [ ] Student 3: Implement rep counting
- [ ] Test squat detection end-to-end

### Phase 3: Additional Exercises
- [ ] Push-up evaluation (Student 3)
- [ ] Lunge evaluation (Student 3)
- [ ] Exercise-specific UI (Student 1)

### Phase 4: Polish
- [ ] Improve feedback messages (Student 3)
- [ ] Add animations (Student 1)
- [ ] Performance optimization (Student 2)
- [ ] Session summary (Student 1)
- [ ] Settings screen (Student 1)

---

## 📚 Resources

### Documentation
- [Project Wiki](link-to-wiki) - In-depth guides
- [API Documentation](link-to-docs) - Code reference
- [Design Mockups](link-to-designs) - UI references

### External Resources
- [MediaPipe Pose Guide](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)

---

## 🤝 Contributing

Each student should focus on their assigned module but can help others when needed. When making changes outside your module:
1. Discuss with the module owner first
2. Don't change interfaces without team agreement
3. Test thoroughly

---

## 📄 License

[Add your license here]

---

## 👨‍💻 Team

- **Student 1** - UI/Frontend
- **Student 2** - ML Integration
- **Student 3** - Evaluation Logic

---

**Good luck and have fun building PoseCoach! 💪🏋️‍♀️**
