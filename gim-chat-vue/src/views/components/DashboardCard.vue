<template>
  <div class="stat-card">
    <div class="card-header">
      <span class="label">{{ title }}</span>
      <span class="trend">{{ trend }}</span>
    </div>
    <div class="card-body">
      <h2 class="value">{{ value }}</h2>
      <div :id="chartId" class="chart-box"></div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
export default {
  props: ['title', 'value', 'trend', 'chartId', 'dataPoints'],
  mounted() {
    const chart = echarts.init(document.getElementById(this.chartId));
    chart.setOption({
      grid: { left: 0, right: 0, top: 10, bottom: 0 },
      xAxis: { type: 'category', show: false },
      yAxis: { type: 'value', show: false },
      series: [{
        data: this.dataPoints,
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { color: '#3b82f6', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{offset: 0, color: 'rgba(59, 130, 246, 0.2)'}, {offset: 1, color: 'transparent'}]) }
      }]
    });
  }
};
</script>

<style scoped>
.stat-card { background: rgba(30, 41, 59, 0.4); border: 1px solid rgba(255,255,255,0.05); border-radius: 16px; padding: 20px; backdrop-filter: blur(10px); }
.card-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.label { color: #94a3b8; font-size: 13px; }
.trend { color: #10b981; font-size: 12px; }
.value { font-size: 24px; color: #f8fafc; margin: 0; }
.chart-box { height: 50px; }
</style>