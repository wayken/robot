<template>
  <div class="appearance">
    <input type="checkbox" id="day-night-checkbox" class="checkbox__input" @change="handleClick" />
    <label for="day-night-checkbox" class="checkbox__items">
      <div class="checkbox__sun-moon"></div>
      <div class="checkbox__star-body">
        <div class="checkbox__star"></div>
      </div>
      <div class="checkbox__cloud-body">
        <div class="checkbox__cloud"></div>
      </div>
    </label>
  </div>
</template>

<script setup lang="ts">
const handleEmit = defineEmits(['click'])

const handleClick = (event: Event) => {
  const target = event.target as HTMLInputElement
  handleEmit('click', target.checked)
}
</script>
