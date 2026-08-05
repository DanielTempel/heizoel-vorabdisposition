import { useState } from 'react'
import { format } from 'date-fns'
import { de } from 'date-fns/locale'
import { CalendarDays, ListFilter, Search, X } from 'lucide-react'
import type { DateRange } from 'react-day-picker'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import {
  Combobox,
  ComboboxChip,
  ComboboxChips,
  ComboboxChipsInput,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxItem,
  ComboboxList,
  ComboboxValue,
  useComboboxAnchor,
} from '@/components/ui/combobox'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from '@/components/ui/input-group'
import {
  Popover,
  PopoverContent,
  PopoverHeader,
  PopoverTitle,
  PopoverTrigger,
} from '@/components/ui/popover'
import type { ConfirmationStatus } from '@/types/confirmation'
import type { DashboardFilters } from '@/types/dashboard'

type FilterPanelProps = {
  filters: DashboardFilters
  onApply: (filters: DashboardFilters) => void
  onChange: (filters: DashboardFilters) => void
}

const statusOptions: ConfirmationStatus[] = [
  'SENT',
  'CONFIRMED',
  'REJECTED',
  'NO_RESPONSE',
]

const statusLabels: Record<ConfirmationStatus, string> = {
  SENT: 'Versendet',
  CONFIRMED: 'Bestätigt',
  REJECTED: 'Abgelehnt',
  NO_RESPONSE: 'Keine Rückmeldung',
}

function parseDate(value: string) {
  if (value === '') {
    return undefined
  }

  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function formatApiDate(date: Date | undefined) {
  if (date === undefined) {
    return ''
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function getDateRange(filters: DashboardFilters): DateRange | undefined {
  const from = parseDate(filters.dateFrom)

  if (from === undefined) {
    return undefined
  }

  return {
    from,
    to: parseDate(filters.dateTo),
  }
}

function getDateRangeLabel(dateRange: DateRange | undefined) {
  if (dateRange?.from === undefined) {
    return 'Zeitraum auswählen'
  }

  if (dateRange.to === undefined) {
    return `Ab ${format(dateRange.from, 'dd.MM.yyyy')}`
  }

  return `${format(dateRange.from, 'dd.MM.yyyy')} – ${format(dateRange.to, 'dd.MM.yyyy')}`
}

export function FilterPanel({
  filters,
  onApply,
  onChange,
}: FilterPanelProps) {
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const [isStatusOpen, setIsStatusOpen] = useState(false)
  const statusComboboxAnchor = useComboboxAnchor()
  const dateRange = getDateRange(filters)
  const hasPopoverFilters =
    filters.statuses.length > 0 ||
    filters.dateFrom !== '' ||
    filters.dateTo !== ''

  function resetPopoverFilters() {
    const nextFilters = {
      ...filters,
      statuses: [],
      dateFrom: '',
      dateTo: '',
    }

    onChange(nextFilters)
    onApply(nextFilters)
    setIsFilterOpen(false)
  }

  function clearSearch() {
    const nextFilters = { ...filters, search: '' }

    onChange(nextFilters)
    onApply(nextFilters)
  }

  return (
    <form
      className="flex min-w-0 flex-1 flex-col gap-2 sm:flex-row sm:items-center"
      onSubmit={(event) => {
        event.preventDefault()
        onApply(filters)
      }}
    >
      <InputGroup className="h-9 cursor-pointer bg-background sm:max-w-md">
        <InputGroupAddon>
          <Search aria-hidden="true" />
        </InputGroupAddon>
        <InputGroupInput
          className="cursor-pointer caret-foreground appearance-none [&::-webkit-search-cancel-button]:hidden"
          onChange={(event) =>
            onChange({ ...filters, search: event.target.value })
          }
          placeholder="Auftrag, Kunde oder Adresse"
          type="search"
          value={filters.search}
        />
        {filters.search !== '' ? (
          <InputGroupAddon align="inline-end">
            <InputGroupButton
              aria-label="Suche löschen"
              onClick={clearSearch}
              size="icon-xs"
            >
              <X />
            </InputGroupButton>
          </InputGroupAddon>
        ) : null}
      </InputGroup>

      <Popover
        onOpenChange={(open) => {
          if (!open && isStatusOpen) {
            return
          }

          setIsFilterOpen(open)
        }}
        open={isFilterOpen}
      >
        <PopoverTrigger asChild>
          <Button type="button" variant="outline">
            <ListFilter />
            Filter
          </Button>
        </PopoverTrigger>
        <PopoverContent
          align="start"
          className="w-[min(26rem,calc(100vw-2rem))] gap-5 p-4"
          onInteractOutside={(event) => {
            const target = event.target

            if (
              target instanceof Element &&
              target.closest('[data-slot="combobox-content"]')
            ) {
              event.preventDefault()
            }
          }}
        >
          <PopoverHeader>
            <PopoverTitle>Filter</PopoverTitle>
          </PopoverHeader>

          <div className="grid gap-2">
            <span className="text-xs font-medium">Avisierungsstatus</span>
            <Combobox
              itemToStringLabel={(status) => statusLabels[status]}
              items={statusOptions}
              multiple
              onOpenChange={setIsStatusOpen}
              onValueChange={(statuses) =>
                onChange({ ...filters, statuses })
              }
              open={isStatusOpen}
              value={filters.statuses}
            >
              <ComboboxChips
                className="min-h-9 w-full cursor-pointer bg-background"
                ref={statusComboboxAnchor}
              >
                <ComboboxValue>
                  {filters.statuses.map((status) => (
                    <ComboboxChip key={status}>
                      {statusLabels[status]}
                    </ComboboxChip>
                  ))}
                </ComboboxValue>
                <ComboboxChipsInput
                  className="cursor-pointer caret-foreground"
                  placeholder="Status auswählen"
                />
              </ComboboxChips>
              <ComboboxContent anchor={statusComboboxAnchor}>
                <ComboboxEmpty>Kein Status gefunden.</ComboboxEmpty>
                <ComboboxList>
                  {(status: ConfirmationStatus) => (
                    <ComboboxItem key={status} value={status}>
                      {statusLabels[status]}
                    </ComboboxItem>
                  )}
                </ComboboxList>
              </ComboboxContent>
            </Combobox>
          </div>

          <div className="grid gap-2">
            <span className="text-xs font-medium">Tourdatum</span>
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  className="h-9 w-full justify-start text-left font-normal"
                  type="button"
                  variant="outline"
                >
                  <CalendarDays />
                  {getDateRangeLabel(dateRange)}
                </Button>
              </PopoverTrigger>
              <PopoverContent align="start" className="w-auto p-0">
                <Calendar
                  defaultMonth={dateRange?.from}
                  locale={de}
                  mode="range"
                  onSelect={(nextDateRange) =>
                    onChange({
                      ...filters,
                      dateFrom: formatApiDate(nextDateRange?.from),
                      dateTo: formatApiDate(nextDateRange?.to),
                    })
                  }
                  selected={dateRange}
                />
              </PopoverContent>
            </Popover>
          </div>

          <div className="flex justify-end gap-2 border-t pt-3">
            <Button
              disabled={!hasPopoverFilters}
              onClick={resetPopoverFilters}
              type="button"
              variant="ghost"
            >
              <X />
              Zurücksetzen
            </Button>
            <Button
              onClick={() => {
                onApply(filters)
                setIsFilterOpen(false)
              }}
              type="button"
            >
              Anwenden
            </Button>
          </div>
        </PopoverContent>
      </Popover>
    </form>
  )
}
