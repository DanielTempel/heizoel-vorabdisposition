import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'

type TourPaginationProps = {
  page: number
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
}

type PaginationEntry = number | 'start-ellipsis' | 'end-ellipsis'

function getPaginationEntries(
  currentPage: number,
  totalPages: number,
): PaginationEntry[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index)
  }

  if (currentPage <= 3) {
    return [0, 1, 2, 3, 4, 'end-ellipsis', totalPages - 1]
  }

  if (currentPage >= totalPages - 4) {
    return [
      0,
      'start-ellipsis',
      totalPages - 5,
      totalPages - 4,
      totalPages - 3,
      totalPages - 2,
      totalPages - 1,
    ]
  }

  return [
    0,
    'start-ellipsis',
    currentPage - 1,
    currentPage,
    currentPage + 1,
    'end-ellipsis',
    totalPages - 1,
  ]
}

export function TourPagination({
  page,
  totalElements,
  totalPages,
  onPageChange,
}: TourPaginationProps) {
  const paginationEntries = getPaginationEntries(page, totalPages)
  const isFirstPage = page === 0
  const isLastPage = page + 1 >= totalPages

  return (
    <footer className="flex flex-col gap-4 border-t pt-4 text-sm text-muted-foreground lg:flex-row lg:items-center lg:justify-between">
      <span className="whitespace-nowrap">
        {totalElements} Touren insgesamt
      </span>

      <Pagination className="mx-0 w-auto justify-start sm:justify-end">
          <PaginationContent>
            <PaginationItem>
              <PaginationPrevious
                aria-disabled={isFirstPage}
                className={
                  isFirstPage ? 'pointer-events-none opacity-50' : undefined
                }
                href="#"
                onClick={(event) => {
                  event.preventDefault()

                  if (!isFirstPage) {
                    onPageChange(page - 1)
                  }
                }}
                tabIndex={isFirstPage ? -1 : undefined}
                text="Zurück"
              />
            </PaginationItem>

            {paginationEntries.map((entry) =>
              typeof entry === 'number' ? (
                <PaginationItem key={entry}>
                  <PaginationLink
                    aria-label={`Seite ${entry + 1}`}
                    href="#"
                    isActive={entry === page}
                    onClick={(event) => {
                      event.preventDefault()

                      if (entry !== page) {
                        onPageChange(entry)
                      }
                    }}
                  >
                    {entry + 1}
                  </PaginationLink>
                </PaginationItem>
              ) : (
                <PaginationItem key={entry}>
                  <PaginationEllipsis />
                </PaginationItem>
              ),
            )}

            <PaginationItem>
              <PaginationNext
                aria-disabled={isLastPage}
                className={
                  isLastPage ? 'pointer-events-none opacity-50' : undefined
                }
                href="#"
                onClick={(event) => {
                  event.preventDefault()

                  if (!isLastPage) {
                    onPageChange(page + 1)
                  }
                }}
                tabIndex={isLastPage ? -1 : undefined}
                text="Weiter"
              />
            </PaginationItem>
          </PaginationContent>
      </Pagination>
    </footer>
  )
}
