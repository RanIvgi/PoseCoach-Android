#!/usr/bin/env python3
"""
MediaPipe Model Performance Analysis Script
Generates graphs and statistical analysis from test JSON files
"""

import json
import os
import glob
from pathlib import Path
from typing import List, Dict, Any
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from scipy import stats
import pandas as pd
from datetime import datetime

# Set style for publication-quality graphs
sns.set_style("whitegrid")
plt.rcParams['figure.figsize'] = (12, 8)
plt.rcParams['font.size'] = 10
plt.rcParams['axes.labelsize'] = 12
plt.rcParams['axes.titlesize'] = 14
plt.rcParams['xtick.labelsize'] = 10
plt.rcParams['ytick.labelsize'] = 10
plt.rcParams['legend.fontsize'] = 10

# Color scheme for models
MODEL_COLORS = {
    'LITE': '#2ecc71',    # Green
    'FULL': '#3498db',    # Blue
    'HEAVY': '#e74c3c'    # Red
}

class PerformanceAnalyzer:
    def __init__(self, results_dir: str):
        self.results_dir = Path(results_dir)
        self.output_dir = self.results_dir / "analysis_output"
        self.output_dir.mkdir(exist_ok=True)
        self.data = []
        self.df = None
        
    def load_test_files(self):
        """Load all JSON test files"""
        json_files = glob.glob(str(self.results_dir / "*.json"))
        print(f"Found {len(json_files)} test files")
        
        for file_path in json_files:
            with open(file_path, 'r') as f:
                data = json.load(f)
                # Calculate real FPS (actual frame rate)
                real_fps = data['performance_metrics']['frame_count'] / data['test_metadata']['duration_seconds']
                
                # Flatten the nested structure for easier analysis
                flat_data = {
                    'model': data['test_metadata']['model_type'],
                    'exercise': data['test_metadata']['exercise_type'],
                    'duration': data['test_metadata']['duration_seconds'],
                    'frame_count': data['performance_metrics']['frame_count'],
                    'fps_reported': data['performance_metrics']['fps_average'],
                    'fps_real': real_fps,
                    'fps_min': data['performance_metrics']['fps_min'],
                    'fps_max': data['performance_metrics']['fps_max'],
                    'fps_stddev': data['performance_metrics']['fps_stddev'],
                    'inference_avg': data['performance_metrics']['inference_time_avg_ms'],
                    'inference_min': data['performance_metrics']['inference_time_min_ms'],
                    'inference_max': data['performance_metrics']['inference_time_max_ms'],
                    'detection_rate': data['detection_metrics']['detection_success_rate'],
                    'confidence': data['detection_metrics']['avg_landmark_confidence'],
                    'visibility': data['detection_metrics']['avg_visibility_score'],
                    'frames_with_pose': data['detection_metrics']['frames_with_pose'],
                    'frames_without_pose': data['detection_metrics']['frames_without_pose']
                }
                self.data.append(flat_data)
        
        # Convert to DataFrame for easier analysis
        self.df = pd.DataFrame(self.data)
        print(f"Loaded {len(self.df)} test results")
        print(f"\nModels: {self.df['model'].unique()}")
        print(f"Exercises: {self.df['exercise'].unique()}")
        
    def generate_graph_1_fps_comparison(self):
        """Graph 1: FPS Comparison Bar Chart"""
        fig, ax = plt.subplots(figsize=(12, 6))
        
        # Group by model and exercise
        grouped = self.df.groupby(['model', 'exercise'])['fps_real'].mean().reset_index()
        
        # Create grouped bar chart
        exercises = sorted(self.df['exercise'].unique())
        models = ['LITE', 'FULL', 'HEAVY']
        x = np.arange(len(exercises))
        width = 0.25
        
        for i, model in enumerate(models):
            model_data = grouped[grouped['model'] == model]
            fps_values = [model_data[model_data['exercise'] == ex]['fps_real'].values[0] 
                         if len(model_data[model_data['exercise'] == ex]) > 0 else 0 
                         for ex in exercises]
            ax.bar(x + i*width, fps_values, width, label=model, color=MODEL_COLORS[model])
        
        ax.set_xlabel('Exercise Type')
        ax.set_ylabel('Average Real FPS')
        ax.set_title('FPS Comparison Across Models and Exercises')
        ax.set_xticks(x + width)
        ax.set_xticklabels([ex.capitalize() for ex in exercises])
        ax.legend()
        ax.grid(axis='y', alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '1_fps_comparison.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '1_fps_comparison.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 1: FPS Comparison")
        
    def generate_graph_2_inference_boxplot(self):
        """Graph 2: Inference Time Box Plot"""
        fig, ax = plt.subplots(figsize=(10, 6))
        
        # Prepare data for boxplot
        models = ['LITE', 'FULL', 'HEAVY']
        data_to_plot = [self.df[self.df['model'] == model]['inference_avg'].values 
                       for model in models]
        
        bp = ax.boxplot(data_to_plot, labels=models, patch_artist=True,
                       medianprops=dict(color='black', linewidth=2),
                       whiskerprops=dict(linewidth=1.5),
                       capprops=dict(linewidth=1.5))
        
        # Color the boxes
        for patch, model in zip(bp['boxes'], models):
            patch.set_facecolor(MODEL_COLORS[model])
            patch.set_alpha(0.7)
        
        ax.set_xlabel('Model Type')
        ax.set_ylabel('Inference Time (ms)')
        ax.set_title('Inference Time Distribution by Model')
        ax.grid(axis='y', alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '2_inference_boxplot.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '2_inference_boxplot.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 2: Inference Time Box Plot")
        
    def generate_graph_3_detection_success(self):
        """Graph 3: Detection Success Rate"""
        fig, ax = plt.subplots(figsize=(10, 6))
        
        # Calculate average detection rate per model
        detection_avg = self.df.groupby('model')['detection_rate'].mean().reindex(['LITE', 'FULL', 'HEAVY'])
        
        bars = ax.bar(detection_avg.index, detection_avg.values, 
                     color=[MODEL_COLORS[m] for m in detection_avg.index])
        
        # Add value labels on bars
        for bar in bars:
            height = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2., height,
                   f'{height:.1f}%',
                   ha='center', va='bottom', fontweight='bold')
        
        ax.set_xlabel('Model Type')
        ax.set_ylabel('Detection Success Rate (%)')
        ax.set_title('Pose Detection Success Rate by Model')
        ax.set_ylim([0, 105])
        ax.grid(axis='y', alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '3_detection_success.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '3_detection_success.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 3: Detection Success Rate")
        
    def generate_graph_4_per_exercise_performance(self):
        """Graph 4: Per-Exercise Performance (Grouped Bar Chart)"""
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))
        
        exercises = sorted(self.df['exercise'].unique())
        models = ['LITE', 'FULL', 'HEAVY']
        x = np.arange(len(models))
        width = 0.25
        
        # Left plot: FPS by exercise
        for i, exercise in enumerate(exercises):
            exercise_data = self.df[self.df['exercise'] == exercise]
            fps_values = [exercise_data[exercise_data['model'] == m]['fps_real'].mean() 
                         for m in models]
            ax1.bar(x + i*width, fps_values, width, label=exercise.capitalize())
        
        ax1.set_xlabel('Model Type')
        ax1.set_ylabel('Average Real FPS')
        ax1.set_title('FPS Performance Across Exercises')
        ax1.set_xticks(x + width)
        ax1.set_xticklabels(models)
        ax1.legend()
        ax1.grid(axis='y', alpha=0.3)
        
        # Right plot: Confidence by exercise
        for i, exercise in enumerate(exercises):
            exercise_data = self.df[self.df['exercise'] == exercise]
            conf_values = [exercise_data[exercise_data['model'] == m]['confidence'].mean() * 100
                          for m in models]
            ax2.bar(x + i*width, conf_values, width, label=exercise.capitalize())
        
        ax2.set_xlabel('Model Type')
        ax2.set_ylabel('Average Confidence (%)')
        ax2.set_title('Detection Confidence Across Exercises')
        ax2.set_xticks(x + width)
        ax2.set_xticklabels(models)
        ax2.legend()
        ax2.grid(axis='y', alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '4_per_exercise_performance.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '4_per_exercise_performance.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 4: Per-Exercise Performance")
        
    def generate_graph_5_processing_breakdown(self):
        """Graph 5: Processing Time Breakdown (Stacked Bar Chart)"""
        fig, ax = plt.subplots(figsize=(10, 6))
        
        models = ['LITE', 'FULL', 'HEAVY']
        
        # Average inference times per model
        inference_times = [self.df[self.df['model'] == m]['inference_avg'].mean() for m in models]
        
        # Create stacked bar (in this case just inference time, but structure for expansion)
        ax.bar(models, inference_times, label='Inference Time', color=[MODEL_COLORS[m] for m in models])
        
        # Add value labels
        for i, (model, time) in enumerate(zip(models, inference_times)):
            ax.text(i, time/2, f'{time:.1f}ms', ha='center', va='center', 
                   fontweight='bold', color='white', fontsize=12)
        
        ax.set_xlabel('Model Type')
        ax.set_ylabel('Processing Time (ms)')
        ax.set_title('Average Processing Time Breakdown by Model')
        ax.legend()
        ax.grid(axis='y', alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '5_processing_breakdown.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '5_processing_breakdown.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 5: Processing Time Breakdown")
        
    def generate_graph_6_real_vs_reported_fps(self):
        """Graph 6: Real FPS vs Reported FPS"""
        fig, ax = plt.subplots(figsize=(12, 6))
        
        models = ['LITE', 'FULL', 'HEAVY']
        x = np.arange(len(models))
        width = 0.35
        
        real_fps = [self.df[self.df['model'] == m]['fps_real'].mean() for m in models]
        reported_fps = [self.df[self.df['model'] == m]['fps_reported'].mean() for m in models]
        
        bars1 = ax.bar(x - width/2, real_fps, width, label='Real FPS (frames/duration)', 
                      color=[MODEL_COLORS[m] for m in models], alpha=0.8)
        bars2 = ax.bar(x + width/2, reported_fps, width, label='Reported FPS (internal)', 
                      color=[MODEL_COLORS[m] for m in models], alpha=0.4, hatch='//')
        
        # Add value labels
        for bars in [bars1, bars2]:
            for bar in bars:
                height = bar.get_height()
                ax.text(bar.get_x() + bar.get_width()/2., height,
                       f'{height:.1f}',
                       ha='center', va='bottom', fontsize=9)
        
        ax.set_xlabel('Model Type')
        ax.set_ylabel('Frames Per Second (FPS)')
        ax.set_title('Real FPS vs Reported FPS - Frame Dropping Analysis')
        ax.set_xticks(x)
        ax.set_xticklabels(models)
        ax.legend()
        ax.grid(axis='y', alpha=0.3)
        
        # Add annotation for HEAVY model issue
        heavy_idx = models.index('HEAVY')
        heavy_real = real_fps[heavy_idx]
        heavy_reported = reported_fps[heavy_idx]
        if heavy_reported - heavy_real > 5:
            ax.annotate(f'Frame dropping:\n{heavy_reported - heavy_real:.1f} FPS loss',
                       xy=(heavy_idx, heavy_real), xytext=(heavy_idx + 0.5, heavy_real + 5),
                       arrowprops=dict(arrowstyle='->', color='red', lw=2),
                       fontsize=10, color='red', fontweight='bold',
                       bbox=dict(boxstyle='round,pad=0.5', facecolor='yellow', alpha=0.7))
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '6_real_vs_reported_fps.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '6_real_vs_reported_fps.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 6: Real vs Reported FPS")
        
    def generate_graph_7_correlation_analysis(self):
        """Graph 7: FPS vs Accuracy Correlation Analysis"""
        fig, ax = plt.subplots(figsize=(10, 8))
        
        models = ['LITE', 'FULL', 'HEAVY']
        
        # Create scatter plot with different colors for each model
        for model in models:
            model_data = self.df[self.df['model'] == model]
            ax.scatter(model_data['fps_real'], model_data['confidence'] * 100,
                      s=model_data['visibility'] * 300,  # Size based on visibility
                      c=MODEL_COLORS[model], alpha=0.6, label=model,
                      edgecolors='black', linewidth=1)
        
        # Calculate correlation
        correlation = stats.pearsonr(self.df['fps_real'], self.df['confidence'])
        r_value = correlation[0]
        p_value = correlation[1]
        
        # Add trend line
        z = np.polyfit(self.df['fps_real'], self.df['confidence'] * 100, 1)
        p = np.poly1d(z)
        x_trend = np.linspace(self.df['fps_real'].min(), self.df['fps_real'].max(), 100)
        ax.plot(x_trend, p(x_trend), "r--", alpha=0.8, linewidth=2, label='Trend line')
        
        # Add R² and correlation info
        r_squared = r_value ** 2
        ax.text(0.05, 0.95, f'Pearson r = {r_value:.3f}\nR² = {r_squared:.3f}\np-value = {p_value:.4f}',
               transform=ax.transAxes, fontsize=11, verticalalignment='top',
               bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
        
        # Interpretation
        if abs(r_value) > 0.7:
            strength = "Strong"
        elif abs(r_value) > 0.4:
            strength = "Moderate"
        else:
            strength = "Weak"
        
        direction = "negative" if r_value < 0 else "positive"
        
        ax.text(0.05, 0.75, f'{strength} {direction} correlation',
               transform=ax.transAxes, fontsize=10, style='italic',
               bbox=dict(boxstyle='round', facecolor='lightblue', alpha=0.6))
        
        ax.set_xlabel('Real FPS (frames/second)')
        ax.set_ylabel('Average Landmark Confidence (%)')
        ax.set_title('Correlation Analysis: FPS vs Detection Accuracy\n(Bubble size = Visibility score)')
        ax.legend(loc='lower right')
        ax.grid(alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(self.output_dir / '7_correlation_fps_accuracy.png', dpi=300, bbox_inches='tight')
        plt.savefig(self.output_dir / '7_correlation_fps_accuracy.pdf', bbox_inches='tight')
        plt.close()
        print("✓ Generated Graph 7: Correlation Analysis (FPS vs Accuracy)")
        
    def generate_statistical_summary(self):
        """Generate comprehensive statistical summary"""
        summary_lines = []
        summary_lines.append("=" * 80)
        summary_lines.append("MEDIAPIPE MODEL PERFORMANCE - STATISTICAL SUMMARY")
        summary_lines.append("=" * 80)
        summary_lines.append(f"\nGenerated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        summary_lines.append(f"Total Tests Analyzed: {len(self.df)}")
        summary_lines.append(f"Device: {self.df.iloc[0]['model'] if len(self.df) > 0 else 'N/A'}")
        summary_lines.append("\n" + "=" * 80)
        
        # Per-model statistics
        for model in ['LITE', 'FULL', 'HEAVY']:
            model_data = self.df[self.df['model'] == model]
            if len(model_data) == 0:
                continue
                
            summary_lines.append(f"\n{model} MODEL STATISTICS:")
            summary_lines.append("-" * 40)
            summary_lines.append(f"  Tests Performed: {len(model_data)}")
            summary_lines.append(f"\n  FPS Metrics:")
            summary_lines.append(f"    Real FPS (avg):     {model_data['fps_real'].mean():.2f} ± {model_data['fps_real'].std():.2f}")
            summary_lines.append(f"    Real FPS (min):     {model_data['fps_real'].min():.2f}")
            summary_lines.append(f"    Real FPS (max):     {model_data['fps_real'].max():.2f}")
            summary_lines.append(f"    Reported FPS (avg): {model_data['fps_reported'].mean():.2f}")
            summary_lines.append(f"\n  Processing Time:")
            summary_lines.append(f"    Inference (avg):    {model_data['inference_avg'].mean():.2f} ms")
            summary_lines.append(f"    Inference (min):    {model_data['inference_min'].mean():.2f} ms")
            summary_lines.append(f"    Inference (max):    {model_data['inference_max'].mean():.2f} ms")
            summary_lines.append(f"\n  Detection Quality:")
            summary_lines.append(f"    Success Rate:       {model_data['detection_rate'].mean():.2f}%")
            summary_lines.append(f"    Confidence:         {model_data['confidence'].mean() * 100:.2f}%")
            summary_lines.append(f"    Visibility:         {model_data['visibility'].mean() * 100:.2f}%")
            summary_lines.append(f"\n  Frame Analysis:")
            summary_lines.append(f"    Total Frames:       {model_data['frame_count'].sum()}")
            summary_lines.append(f"    Avg Frames/Test:    {model_data['frame_count'].mean():.1f}")
            summary_lines.append(f"    Frames with Pose:   {model_data['frames_with_pose'].sum()}")
            summary_lines.append(f"    Frames without:     {model_data['frames_without_pose'].sum()}")
        
        # Cross-model comparisons
        summary_lines.append("\n" + "=" * 80)
        summary_lines.append("COMPARATIVE ANALYSIS")
        summary_lines.append("=" * 80)
        
        # Best performers
        best_fps_model = self.df.groupby('model')['fps_real'].mean().idxmax()
        best_conf_model = self.df.groupby('model')['confidence'].mean().idxmax()
        best_vis_model = self.df.groupby('model')['visibility'].mean().idxmax()
        
        summary_lines.append(f"\n  Highest Real FPS:       {best_fps_model}")
        summary_lines.append(f"  Highest Confidence:     {best_conf_model}")
        summary_lines.append(f"  Highest Visibility:     {best_vis_model}")
        
        # Correlation analysis
        corr_fps_conf = stats.pearsonr(self.df['fps_real'], self.df['confidence'])
        corr_fps_vis = stats.pearsonr(self.df['fps_real'], self.df['visibility'])
        
        summary_lines.append(f"\n  Correlation (FPS vs Confidence): r = {corr_fps_conf[0]:.3f}, p = {corr_fps_conf[1]:.4f}")
        summary_lines.append(f"  Correlation (FPS vs Visibility): r = {corr_fps_vis[0]:.3f}, p = {corr_fps_vis[1]:.4f}")
        
        # Per-exercise breakdown
        summary_lines.append("\n" + "=" * 80)
        summary_lines.append("PER-EXERCISE BREAKDOWN")
        summary_lines.append("=" * 80)
        
        for exercise in sorted(self.df['exercise'].unique()):
            ex_data = self.df[self.df['exercise'] == exercise]
            summary_lines.append(f"\n{exercise.upper()}:")
            summary_lines.append("-" * 40)
            for model in ['LITE', 'FULL', 'HEAVY']:
                model_ex_data = ex_data[ex_data['model'] == model]
                if len(model_ex_data) > 0:
                    summary_lines.append(f"  {model:6s}: {model_ex_data['fps_real'].mean():6.2f} FPS, "
                                       f"{model_ex_data['confidence'].mean() * 100:5.2f}% conf, "
                                       f"{model_ex_data['visibility'].mean() * 100:5.2f}% vis")
        
        # Recommendations
        summary_lines.append("\n" + "=" * 80)
        summary_lines.append("RECOMMENDATIONS")
        summary_lines.append("=" * 80)
        
        lite_fps = self.df[self.df['model'] == 'LITE']['fps_real'].mean()
        full_fps = self.df[self.df['model'] == 'FULL']['fps_real'].mean()
        heavy_fps = self.df[self.df['model'] == 'HEAVY']['fps_real'].mean()
        
        full_conf = self.df[self.df['model'] == 'FULL']['confidence'].mean() * 100
        heavy_conf = self.df[self.df['model'] == 'HEAVY']['confidence'].mean() * 100
        
        summary_lines.append(f"\n  LITE Model:")
        summary_lines.append(f"    ✓ Best for: Real-time applications requiring maximum FPS")
        summary_lines.append(f"    ✓ Trade-off: {lite_fps:.1f} FPS with acceptable accuracy")
        
        summary_lines.append(f"\n  FULL Model (RECOMMENDED):")
        summary_lines.append(f"    ⭐ Best for: Balanced performance and accuracy")
        summary_lines.append(f"    ⭐ Delivers: {full_fps:.1f} FPS with {full_conf:.2f}% confidence")
        summary_lines.append(f"    ⭐ Optimal for most real-time pose coaching applications")
        
        summary_lines.append(f"\n  HEAVY Model:")
        if heavy_fps < 5:
            summary_lines.append(f"    ❌ NOT RECOMMENDED: Severe frame dropping ({heavy_fps:.1f} FPS)")
            summary_lines.append(f"    ❌ Only {heavy_conf - full_conf:.2f}% accuracy gain over FULL")
            summary_lines.append(f"    ❌ Unusable for real-time user experience")
        else:
            summary_lines.append(f"    ⚠ Use only for: Offline analysis or accuracy-critical applications")
            summary_lines.append(f"    ⚠ Trade-off: {heavy_fps:.1f} FPS for {heavy_conf:.2f}% confidence")
        
        summary_lines.append("\n" + "=" * 80)
        
        # Save summary
        summary_text = "\n".join(summary_lines)
        with open(self.output_dir / 'STATISTICAL_SUMMARY.txt', 'w') as f:
            f.write(summary_text)
        
        print("\n" + summary_text)
        print(f"\n✓ Statistical summary saved to: {self.output_dir / 'STATISTICAL_SUMMARY.txt'}")
        
    def generate_csv_export(self):
        """Export data to CSV for further analysis"""
        # Main data export
        self.df.to_csv(self.output_dir / 'performance_data.csv', index=False)
        
        # Summary statistics by model
        summary_stats = self.df.groupby('model').agg({
            'fps_real': ['mean', 'std', 'min', 'max'],
            'fps_reported': ['mean'],
            'inference_avg': ['mean', 'std', 'min', 'max'],
            'confidence': ['mean', 'std'],
            'visibility': ['mean', 'std'],
            'detection_rate': ['mean'],
            'frame_count': ['sum', 'mean']
        }).round(2)
        
        summary_stats.to_csv(self.output_dir / 'summary_statistics.csv')
        
        print(f"✓ CSV exports saved to: {self.output_dir}")
        
    def run_full_analysis(self):
        """Run complete analysis pipeline"""
        print("\n" + "=" * 80)
        print("MEDIAPIPE MODEL PERFORMANCE ANALYSIS")
        print("=" * 80 + "\n")
        
        print("Step 1: Loading test files...")
        self.load_test_files()
        
        print("\nStep 2: Generating graphs...")
        self.generate_graph_1_fps_comparison()
        self.generate_graph_2_inference_boxplot()
        self.generate_graph_3_detection_success()
        self.generate_graph_4_per_exercise_performance()
        self.generate_graph_5_processing_breakdown()
        self.generate_graph_6_real_vs_reported_fps()
        self.generate_graph_7_correlation_analysis()
        
        print("\nStep 3: Generating statistical summary...")
        self.generate_statistical_summary()
        
        print("\nStep 4: Exporting data to CSV...")
        self.generate_csv_export()
        
        print("\n" + "=" * 80)
        print("ANALYSIS COMPLETE!")
        print("=" * 80)
        print(f"\nAll outputs saved to: {self.output_dir}")
        print("\nGenerated files:")
        print("  • 7 graphs (PNG + PDF format)")
        print("  • Statistical summary (TXT)")
        print("  • Raw data export (CSV)")
        print("  • Summary statistics (CSV)")
        print("\n" + "=" * 80 + "\n")

def main():
    # Get the directory containing this script
    script_dir = Path(__file__).parent
    
    # Create analyzer and run
    analyzer = PerformanceAnalyzer(script_dir)
    analyzer.run_full_analysis()

if __name__ == "__main__":
    main()
