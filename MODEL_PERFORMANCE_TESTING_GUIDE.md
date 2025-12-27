# MediaPipe Model Performance Testing Guide
## Xiaomi Mi 8 - Model Comparison Study

**Device Under Test:** Xiaomi Mi 8  
**Test Date:** December 27, 2025  
**Models to Compare:** Lite, Full, Heavy  
**Total Test Runs:** 9 (3 models × 3 exercises)

---

## 📋 Pre-Test Checklist

### Device Preparation
- [ ] **Charge device to 100%** (to avoid battery-related throttling)
- [ ] **Close all background apps** (Settings → Apps → Force stop unnecessary apps)
- [ ] **Disable battery saver mode** (if enabled)
- [ ] **Set screen brightness to 75%** (for consistency)
- [ ] **Clear app cache** (Settings → Apps → PoseCoach → Clear Cache)
- [ ] **Restart device** before starting tests
- [ ] **Connect to stable WiFi** (for potential cloud features)
- [ ] **Ensure sufficient storage** (at least 500MB free for logs)

### Environment Setup
- [ ] **Good lighting** - Natural daylight or bright indoor lighting
- [ ] **Clear background** - Plain wall, minimal visual clutter
- [ ] **Phone mount/tripod** - Stable camera position throughout all tests
- [ ] **Camera distance** - 1.5-2 meters from exercise area
- [ ] **Full body visibility** - Ensure entire body fits in frame
- [ ] **Consistent position** - Mark floor position for reproducibility

### App Preparation
- [ ] **Install updated PoseCoach app** with model selector
- [ ] **Grant camera permissions** (Settings → Permissions)
- [ ] **Verify all 3 models loaded** (check assets folder has all .task files)
- [ ] **Test model switcher** works (tap through Lite/Full/Heavy)
- [ ] **Test export function** (verify you can export test data)

---

## 🧪 Test Execution Protocol

### Test Matrix

| Test # | Model | Exercise | Duration | Expected FPS | Notes |
|--------|-------|----------|----------|--------------|-------|
| 1      | Lite  | Squats   | 30s      | ~30 FPS      |       |
| 2      | Lite  | Push-ups | 30s      | ~30 FPS      |       |
| 3      | Lite  | Planks   | 30s      | ~30 FPS      |       |
| 4      | Full  | Squats   | 30s      | ~20-25 FPS   |       |
| 5      | Full  | Push-ups | 30s      | ~20-25 FPS   |       |
| 6      | Full  | Planks   | 30s      | ~20-25 FPS   |       |
| 7      | Heavy | Squats   | 30s      | ~15-20 FPS   |       |
| 8      | Heavy | Push-ups | 30s      | ~15-20 FPS   |       |
| 9      | Heavy | Planks   | 30s      | ~15-20 FPS   |       |

---

## 📱 Step-by-Step Test Procedure

### Before Each Test Run

1. **Select Model**
   - Open PoseCoach app
   - Tap "Model: [Current]" button (top-left or settings)
   - Select target model (Lite/Full/Heavy)
   - Wait for "Model loaded successfully" message
   - Verify model name displays correctly

2. **Position Yourself**
   - Stand in marked position
   - Ensure full body visible in preview
   - Check lighting is adequate
   - Ready exercise starting position

3. **Start Recording**
   - Select exercise type (Squats/Push-ups/Planks)
   - Tap "Start Session" button
   - Wait for 3-2-1 countdown
   - Begin exercise when prompted

### During Test (30 seconds)

4. **Perform Exercise**
   
   **Squats:**
   - Stand feet shoulder-width apart
   - Perform 10 controlled squats
   - 3 seconds down, 3 seconds up
   - Keep consistent tempo
   
   **Push-ups:**
   - Standard push-up position
   - Perform 10 controlled push-ups
   - 3 seconds down, 3 seconds up
   - Keep body straight
   
   **Planks:**
   - Forearm plank position
   - Hold steady for 30 seconds
   - Minimal movement
   - Maintain straight body line

5. **Monitor Metrics** (displayed on screen)
   - Watch FPS counter (should be stable)
   - Observe pose detection (skeleton overlay)
   - Note any lag or stuttering
   - Check if landmarks are detected consistently

### After Each Test Run

6. **Save Session**
   - Wait for session to complete (automatic at 30s)
   - Review results screen
   - Take screenshot of results (optional backup)

7. **Export Test Data**
   - Tap "Export Performance Data" button
   - File will be saved as: `test_[#]_xiaomi8_[model]_[exercise]_[date].json`
   - Verify file created successfully
   - Check file appears in Downloads or app folder

8. **Record Manual Observations**
   - Any lag or stuttering?
   - Was pose detected consistently?
   - Did app crash or freeze?
   - Battery/temperature concerns?
   - Other notable behavior?

9. **Rest Between Tests**
   - Wait **2 minutes** between each test
   - Allow device to cool down
   - Let CPU/GPU settle to baseline
   - Check battery level hasn't dropped too much

---

## 📊 Data Collection Format

### Automatic Metrics (Exported in JSON)

Each test generates a JSON file with:

```json
{
  "test_metadata": {
    "test_number": 1,
    "device_model": "Xiaomi Mi 8",
    "android_version": "10",
    "model_type": "LITE",
    "exercise_type": "SQUATS",
    "delegate": "CPU",
    "timestamp": "2025-12-27T13:30:00Z",
    "duration_seconds": 30
  },
  "performance_metrics": {
    "fps_average": 28.5,
    "fps_min": 24.0,
    "fps_max": 30.0,
    "fps_stddev": 2.3,
    "frame_count": 855,
    "inference_time_avg_ms": 18.2,
    "inference_time_min_ms": 15.1,
    "inference_time_max_ms": 24.8,
    "bitmap_conversion_avg_ms": 8.5,
    "rotation_avg_ms": 2.1,
    "total_processing_avg_ms": 28.8
  },
  "detection_metrics": {
    "frames_with_pose": 840,
    "frames_without_pose": 15,
    "detection_success_rate": 98.2,
    "avg_landmark_confidence": 0.87,
    "avg_visibility_score": 0.91
  },
  "device_metrics": {
    "battery_start": 100,
    "battery_end": 99,
    "battery_drain_percent": 1,
    "temperature_celsius": 35.2
  }
}
```

### Manual Observations Template

Create a spreadsheet or document with these columns:

| Test # | Model | Exercise | FPS Avg | Lag? | Consistent Detection? | Notes |
|--------|-------|----------|---------|------|-----------------------|-------|
| 1      | Lite  | Squats   |         |      |                       |       |
| 2      | Lite  | Push-ups |         |      |                       |       |
| ...    | ...   | ...      |         |      |                       |       |

---

## 🎯 Success Criteria

### What Makes a Valid Test

✅ **Valid test run:**
- Full 30 seconds completed
- No app crashes or freezes
- Consistent camera view (no obstruction)
- Stable device position (no movement)
- Good lighting throughout
- All data exported successfully

❌ **Invalid test (needs repeat):**
- App crashed mid-session
- Camera blocked or moved
- Person moved out of frame significantly
- Poor lighting caused detection failure
- Export failed or data corrupted
- Device thermal throttling evident (extreme heat)

---

## 🔧 Troubleshooting

### Common Issues

**Problem:** App crashes when switching models  
**Solution:** Force close app, clear cache, restart app, try again

**Problem:** Export button doesn't work  
**Solution:** Check storage permissions, ensure sufficient space (500MB+)

**Problem:** FPS very low (< 10) on all models  
**Solution:** Restart device, close background apps, wait for device to cool

**Problem:** Pose not detected at all  
**Solution:** Check lighting, ensure full body visible, adjust camera distance

**Problem:** Model switch doesn't seem to work  
**Solution:** Verify model file exists in assets, check logcat for errors

**Problem:** Device getting very hot  
**Solution:** Take longer breaks between tests (5 minutes), test in cooler environment

---

## 📤 Sending Data to Analyst

### After Completing All 9 Tests

1. **Locate exported files**
   - Check app's Documents folder or Downloads
   - Should have 9 JSON files (one per test)
   - Verify file sizes are reasonable (10-50KB each)

2. **Organize files**
   - Create a folder named `xiaomi8_model_tests_2025-12-27`
   - Copy all 9 JSON files into this folder
   - Add your manual observations spreadsheet/document
   - Include any screenshots you took

3. **Compress folder**
   - Zip the entire folder
   - Final size should be ~200-500KB

4. **Send to me**
   - Upload zip file to cloud storage (Google Drive, Dropbox, etc.)
   - Share link with me in chat
   - Or attach directly if possible

5. **What I'll do with your data**
   - Parse all JSON files
   - Calculate statistical metrics
   - Generate comparison graphs
   - Create analysis summary
   - Provide recommendations

---

## 📈 Expected Graphs & Analysis

### Graphs I'll Generate

1. **FPS Comparison Bar Chart**
   - X-axis: Model type (Lite, Full, Heavy)
   - Y-axis: Average FPS
   - Shows which model is fastest

2. **Inference Time Box Plot**
   - Shows distribution of processing times
   - Identifies outliers and consistency
   - Per-model comparison

3. **Detection Success Rate**
   - Percentage of frames with successful pose detection
   - Higher = more reliable model

4. **Per-Exercise Performance**
   - Grouped bar chart
   - Compares all 3 models across each exercise
   - Identifies if certain exercises affect performance

5. **Processing Time Breakdown**
   - Stacked bar chart
   - Shows bitmap conversion, rotation, inference times
   - Identifies bottlenecks per model

6. **Efficiency Score**
   - Custom metric: (Accuracy × FPS) / Model Size
   - Overall "bang for buck" comparison

### Statistical Analysis

I'll provide:
- Mean, median, standard deviation for all metrics
- Statistical significance tests (is Heavy really better than Lite?)
- Confidence intervals
- Correlation analysis (FPS vs accuracy)
- Performance recommendations

---

## 🎓 Tips for Best Results

### For Consistent Data

1. **Same time of day** - Test all 9 runs within 1-2 hours
2. **Same location** - Don't move setup between tests
3. **Same lighting** - Close curtains if testing near windows
4. **Same person** - Use same person for all exercises
5. **Same clothing** - Wear same outfit (affects detection slightly)
6. **Same tempo** - Keep exercise speed consistent
7. **Take breaks** - 2 minutes minimum between tests
8. **Stay hydrated** - Your performance affects data quality
9. **Document everything** - More notes = better analysis
10. **Have fun!** - This is research, but it should be enjoyable

### Recommended Test Order

To minimize fatigue and device heating:

**Session 1 (Morning):**
1. Test 1: Lite - Squats
2. Test 4: Full - Squats  
3. Test 7: Heavy - Squats

**Break (2+ hours, let device cool completely)**

**Session 2 (Afternoon):**
4. Test 2: Lite - Push-ups
5. Test 5: Full - Push-ups
6. Test 8: Heavy - Push-ups

**Break (2+ hours)**

**Session 3 (Evening):**
7. Test 3: Lite - Planks
8. Test 6: Full - Planks
9. Test 9: Heavy - Planks

This way you test each exercise with all 3 models back-to-back, minimizing environmental variables.

---

## 📝 Quick Reference Checklist

**Before starting:**
- [ ] Device charged 100%
- [ ] All background apps closed
- [ ] Environment prepared (lighting, camera, position)
- [ ] Model switcher tested and working

**For each test:**
- [ ] Select correct model (Lite/Full/Heavy)
- [ ] Select correct exercise (Squats/Push-ups/Planks)
- [ ] Position yourself correctly
- [ ] Start session and perform exercise (30s)
- [ ] Export data immediately after
- [ ] Record manual observations
- [ ] Wait 2 minutes before next test

**After all tests:**
- [ ] Collect all 9 JSON files
- [ ] Compile manual observations
- [ ] Organize into folder
- [ ] Compress and send to analyst
- [ ] Celebrate! 🎉

---

## 📧 Contact & Questions

If you encounter any issues during testing:
1. Take screenshots of error messages
2. Note what you were doing when error occurred
3. Check logcat for detailed error info (if comfortable with ADB)
4. Document and continue with next test if possible

Send me:
- Error screenshots
- Description of issue
- Which test number it occurred on
- Any other relevant context

I'll help troubleshoot and we can re-run specific tests if needed.

---

## 🏆 Expected Outcomes

Based on Xiaomi Mi 8 specifications (Snapdragon 845, 2018):

**Lite Model:**
- FPS: 28-30
- Inference: 15-20ms
- Detection: 85-90% success
- Best for: Real-time apps needing high FPS

**Full Model (Current):**
- FPS: 20-25
- Inference: 25-35ms
- Detection: 92-96% success
- Best for: Balance of speed and accuracy

**Heavy Model:**
- FPS: 15-20
- Inference: 40-50ms
- Detection: 95-98% success
- Best for: Accuracy-critical applications

These are estimates - your actual results will provide real data!

---

## 🎯 Project Report Integration

After I generate the graphs and analysis, you can include:

### In Your Report

**Methods Section:**
"Performance testing was conducted on a Xiaomi Mi 8 device (Snapdragon 845, Android 10) comparing three MediaPipe Pose Landmarker models: Lite, Full, and Heavy. Nine test sessions were performed (3 models × 3 exercises), each lasting 30 seconds. Metrics collected included FPS, inference time, detection success rate, and landmark confidence scores."

**Results Section:**
- Include the graphs I generate
- Reference the statistical analysis
- Highlight key findings (e.g., "Heavy model achieved 23% better accuracy at 35% lower FPS")

**Discussion Section:**
- Trade-offs between models
- Recommendations for different use cases
- Limitations of testing methodology

**Tables:**
I'll provide ready-to-use tables with all metrics formatted for academic reports.

---

## 📚 Additional Resources

- MediaPipe Pose Documentation: https://developers.google.com/mediapipe/solutions/vision/pose_landmarker
- Model comparison official benchmarks (for reference)
- Logcat filtering guide (if needed for debugging)

---

**Good luck with your testing! 🚀**

Remember: Quality data > Quantity. Take your time, follow the protocol, and document everything. Your project report will thank you!
