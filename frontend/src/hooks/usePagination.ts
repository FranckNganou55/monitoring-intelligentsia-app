import { useState } from 'react'

export function usePagination(defaultSize = 20) {
  const [page, setPage] = useState(0)
  const [size] = useState(defaultSize)

  const reset = () => setPage(0)

  return { page, size, setPage, reset }
}
