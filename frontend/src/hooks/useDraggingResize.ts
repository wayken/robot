interface DraggingResizeOptions {
  min?: number
  max?: number
  initialWidth?: number
}

export function useDraggingResize(options: DraggingResizeOptions = {}) {
  const { initialWidth = 220, min = 158, max = 420 } = options

  const width = ref(initialWidth)
  const isDragging = ref(false)

  const handleResize = (e: MouseEvent) => {
    const startWidth = width.value
    isDragging.value = true

    const move = (moveEvent: MouseEvent) => {
      if (moveEvent.x > e.x) {
        const resizeWidth = startWidth + (moveEvent.x - e.x)
        if (resizeWidth <= max) {
          width.value = resizeWidth
        }
      } else {
        const resizeWidth = startWidth - (e.x - moveEvent.x)
        if (resizeWidth >= min) {
          width.value = resizeWidth
        }
      }
    }

    const up = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', move)
      document.removeEventListener('mouseup', up)
    }

    document.addEventListener('mousemove', move)
    document.addEventListener('mouseup', up)
  }

  return {
    width,
    isDragging,
    handleResize
  }
}
