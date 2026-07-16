<template>
  <label class="a-checkbox"
    :class="[
      size ? 'a-checkbox--' + size : '',
      {
        'is-checked' : value,
        'is-disabled' : disabled,
        'is-indeterminate' : indeterminate
      }
    ]"
  >
    <span class="a-checkbox__input" @click="handleChecked"></span>
    <span v-if="$slots.default" class="a-checkbox__label"><slot></slot></span>
  </label>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ACheckbox'
})

const props = defineProps({
  value: {
    type: Boolean,
    default: false
  },
  indeterminate: {
    type: Boolean,
    default: false
  },
  size: String,
  disabled: {
    type: Boolean,
    default: false
  }
})

const handleEmit = defineEmits(['change'])
const handleChecked = (event: Event) => {
  if (!props.disabled) {
    handleEmit('change', event, !props.value)
  }
}
</script>
