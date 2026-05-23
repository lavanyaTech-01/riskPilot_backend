import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from datetime import datetime, timedelta
import numpy as np

# Set style
plt.style.use('seaborn-v0_8-darkgrid')

# ===================== GANTT CHART =====================
fig1, ax1 = plt.subplots(figsize=(16, 10))

# Project phases with start and duration (in days)
phases = [
    {'name': 'Requirements Analysis', 'start': 0, 'duration': 10, 'color': '#FF6B6B'},
    {'name': 'System Design', 'start': 10, 'duration': 15, 'color': '#4ECDC4'},
    {'name': 'Backend Development', 'start': 25, 'duration': 30, 'color': '#45B7D1'},
    {'name': 'Frontend Development', 'start': 25, 'duration': 25, 'color': '#FFA07A'},
    {'name': 'Integration & Testing', 'start': 50, 'duration': 15, 'color': '#98D8C8'},
    {'name': 'Deployment & Documentation', 'start': 65, 'duration': 10, 'color': '#F7DC6F'},
]

# Plot Gantt bars
for idx, phase in enumerate(phases):
    ax1.barh(idx, phase['duration'], left=phase['start'], 
            height=0.6, align='center', color=phase['color'], 
            edgecolor='black', linewidth=2, alpha=0.8)
    
    # Add phase name on the bar
    ax1.text(phase['start'] + phase['duration']/2, idx, 
            phase['name'], ha='center', va='center', 
            fontweight='bold', fontsize=10, color='white')

# Formatting
ax1.set_yticks(range(len(phases)))
ax1.set_yticklabels([phase['name'] for phase in phases], fontsize=11)
ax1.set_xlabel('Days', fontsize=12, fontweight='bold')
ax1.set_title('RiskPilot Project - Gantt Chart\n(Project Duration: 75 Days)', 
             fontsize=14, fontweight='bold', pad=20)
ax1.set_xlim(0, 80)
ax1.grid(True, axis='x', alpha=0.3)

# Add milestone markers
milestones = [
    (10, 'Design\nComplete'),
    (25, 'Dev\nStarts'),
    (50, 'Integration\nStarts'),
    (65, 'Deployment\nStarts'),
    (75, 'Project\nComplete')
]

for milestone_day, milestone_label in milestones:
    ax1.axvline(x=milestone_day, color='red', linestyle='--', linewidth=1.5, alpha=0.5)
    ax1.text(milestone_day, len(phases) - 0.3, milestone_label, 
            ha='center', fontsize=9, color='red', fontweight='bold')

plt.tight_layout()
plt.savefig('C:\\Users\\Lenovo\\eclipse-workspace\\RiskPilot\\GANTT_CHART.png', dpi=300, bbox_inches='tight')
print("Gantt Chart saved as: GANTT_CHART.png")

# ===================== PERT CHART =====================
fig2, ax2 = plt.subplots(figsize=(16, 10))

# Define tasks and their dependencies
tasks = {
    'A': {'name': 'Requirements\nGathering', 'x': 1, 'y': 5, 'duration': 10, 'color': '#FF6B6B'},
    'B': {'name': 'System\nDesign', 'x': 3, 'y': 5, 'duration': 15, 'color': '#4ECDC4'},
    'C': {'name': 'Backend\nDevelopment', 'x': 5, 'y': 7, 'duration': 30, 'color': '#45B7D1'},
    'D': {'name': 'Frontend\nDevelopment', 'x': 5, 'y': 3, 'duration': 25, 'color': '#FFA07A'},
    'E': {'name': 'API\nIntegration', 'x': 7, 'y': 5, 'duration': 8, 'color': '#98D8C8'},
    'F': {'name': 'Testing &\nQA', 'x': 8, 'y': 5, 'duration': 10, 'color': '#F38181'},
    'G': {'name': 'Deployment', 'x': 9, 'y': 5, 'duration': 5, 'color': '#F7DC6F'},
}

# Draw arrows for dependencies
dependencies = [
    ('A', 'B'),
    ('B', 'C'),
    ('B', 'D'),
    ('C', 'E'),
    ('D', 'E'),
    ('E', 'F'),
    ('F', 'G'),
]

for start_task, end_task in dependencies:
    start_x, start_y = tasks[start_task]['x'] + 1, tasks[start_task]['y']
    end_x, end_y = tasks[end_task]['x'] - 0.5, tasks[end_task]['y']
    ax2.annotate('', xy=(end_x, end_y), xytext=(start_x, start_y),
                arrowprops=dict(arrowstyle='->', lw=2, color='black', alpha=0.6))

# Draw task nodes
for task_id, task_info in tasks.items():
    # Draw circle
    circle = mpatches.Circle((task_info['x'], task_info['y']), 0.4, 
                            color=task_info['color'], ec='black', linewidth=2, zorder=3)
    ax2.add_patch(circle)
    
    # Add task ID
    ax2.text(task_info['x'], task_info['y'], task_id, 
            ha='center', va='center', fontweight='bold', fontsize=12, color='white', zorder=4)
    
    # Add task name
    ax2.text(task_info['x'], task_info['y'] - 0.8, task_info['name'], 
            ha='center', va='top', fontsize=10, fontweight='bold')
    
    # Add duration
    ax2.text(task_info['x'], task_info['y'] - 1.5, f"({task_info['duration']}d)", 
            ha='center', va='top', fontsize=9, style='italic', color='#333333')

# Add legend
legend_elements = [
    mpatches.Patch(color='#FF6B6B', label='Analysis'),
    mpatches.Patch(color='#4ECDC4', label='Design'),
    mpatches.Patch(color='#45B7D1', label='Backend'),
    mpatches.Patch(color='#FFA07A', label='Frontend'),
    mpatches.Patch(color='#98D8C8', label='Integration'),
    mpatches.Patch(color='#F38181', label='Testing'),
    mpatches.Patch(color='#F7DC6F', label='Deployment'),
]
ax2.legend(handles=legend_elements, loc='upper right', fontsize=10)

# Formatting
ax2.set_xlim(0, 10)
ax2.set_ylim(1, 9)
ax2.set_aspect('equal')
ax2.axis('off')
ax2.set_title('RiskPilot Project - PERT Chart\n(Critical Path Analysis)', 
             fontsize=14, fontweight='bold', pad=20)

plt.tight_layout()
plt.savefig('C:\\Users\\Lenovo\\eclipse-workspace\\RiskPilot\\PERT_CHART.png', dpi=300, bbox_inches='tight')
print("PERT Chart saved as: PERT_CHART.png")

# ===================== DETAILED TIMELINE =====================
fig3, ax3 = plt.subplots(figsize=(16, 8))

# Create a more detailed timeline
timeline_data = [
    ('Week 1-2', 'Requirements Analysis', 10, '#FF6B6B'),
    ('Week 3-4', 'System Design', 15, '#4ECDC4'),
    ('Week 5-8', 'Backend Development', 30, '#45B7D1'),
    ('Week 5-7', 'Frontend Development', 25, '#FFA07A'),
    ('Week 9-9.5', 'API Integration', 8, '#98D8C8'),
    ('Week 10-11', 'Testing & QA', 10, '#F38181'),
    ('Week 12', 'Deployment', 5, '#F7DC6F'),
    ('Ongoing', 'Documentation', 75, '#B19CD9'),
]

y_pos = np.arange(len(timeline_data))
durations = [item[2] for item in timeline_data]
colors = [item[3] for item in timeline_data]
labels = [f"{item[0]}\n{item[1]}" for item in timeline_data]

bars = ax3.barh(y_pos, durations, color=colors, edgecolor='black', linewidth=2, alpha=0.8)

# Add duration labels on bars
for idx, (bar, duration) in enumerate(zip(bars, durations)):
    width = bar.get_width()
    ax3.text(width/2, bar.get_y() + bar.get_height()/2, 
            f'{duration} days', ha='center', va='center', 
            fontweight='bold', fontsize=10, color='white')

ax3.set_yticks(y_pos)
ax3.set_yticklabels(labels, fontsize=11)
ax3.set_xlabel('Duration (Days)', fontsize=12, fontweight='bold')
ax3.set_title('RiskPilot Project - Detailed Timeline\n(Total Project Duration: 75 Days)', 
             fontsize=14, fontweight='bold', pad=20)
ax3.set_xlim(0, 80)
ax3.grid(True, axis='x', alpha=0.3)

plt.tight_layout()
plt.savefig('C:\\Users\\Lenovo\\eclipse-workspace\\RiskPilot\\PROJECT_TIMELINE.png', dpi=300, bbox_inches='tight')
print("Project Timeline saved as: PROJECT_TIMELINE.png")

print("\nAll charts generated successfully!")
print("Location: C:\\Users\\Lenovo\\eclipse-workspace\\RiskPilot\\")
print("\nFiles created:")
print("1. GANTT_CHART.png")
print("2. PERT_CHART.png")
print("3. PROJECT_TIMELINE.png")
