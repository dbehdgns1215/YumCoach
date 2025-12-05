<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
  labels: {
    type: Array,
    required: true,
  },
  datasets: {
    type: Array,
    required: true,
  },
})

const canvasRef = ref(null)
let chartInstance

const renderChart = () => {
  if (!canvasRef.value) return
  if (chartInstance) {
    chartInstance.destroy()
  }

  chartInstance = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels: props.labels,
      datasets: props.datasets,
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        tooltip: {
          callbacks: {
            title: (tooltipItems) => tooltipItems[0]?.label || '',
            label: (tooltipItem) => `${tooltipItem.dataset.label}: ${tooltipItem.formattedValue}`,
          },
        },
        legend: {
          display: true,
          labels: {
            usePointStyle: true,
            padding: 12,
            boxWidth: 12,
          },
        },
      },
      scales: {
        x: {
          grid: {
            display: false,
          },
        },
        y: {
          beginAtZero: true,
          ticks: {
            callback: (value) => value.toLocaleString(),
          },
        },
      },
    },
  })
}

onMounted(renderChart)
watch(
  () => [props.labels, props.datasets],
  () => {
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.destroy()
  }
})
</script>

<template>
  <div class="weekly-chart">
    <canvas ref="canvasRef" />
  </div>
</template>
