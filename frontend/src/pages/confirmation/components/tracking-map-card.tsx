import 'leaflet/dist/leaflet.css'
import { useEffect, useRef } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import L from 'leaflet'
import {
  CircleCheckBig,
  LoaderCircle,
  MapPinned,
  RefreshCw,
  Route,
  Truck,
} from 'lucide-react'
import {
  MapContainer,
  Marker,
  Polyline,
  TileLayer,
  Tooltip,
  useMap,
} from 'react-leaflet'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { CustomerConfirmationPreview } from '../../../types/confirmation'
import type { DriverLocation, TrackingInfo } from '../../../types/tracking'

type TrackingMapCardProps = {
  confirmation: CustomerConfirmationPreview
  trackingInfo: TrackingInfo | null
  driverLocation: DriverLocation | null
  isRefreshing: boolean
  onRefresh: () => void
}

type Coordinate = [number, number]

function createMarkerIcon(kind: 'vehicle' | 'destination') {
  const icon =
    kind === 'vehicle' ? (
      <Truck size={22} strokeWidth={2.4} />
    ) : (
      <MapPinned size={22} strokeWidth={2.4} />
    )

  const palette =
    kind === 'vehicle'
      ? {
          background: '#fff7db',
          foreground: '#8a4b08',
          border: '#f6c453',
          shadow: 'rgba(196, 118, 13, 0.25)',
        }
      : {
          background: '#0f766e',
          foreground: '#f0fdfa',
          border: '#134e4a',
          shadow: 'rgba(15, 118, 110, 0.28)',
        }

  return L.divIcon({
    className: '',
    html: renderToStaticMarkup(
      <div
        data-testid={kind === 'vehicle' ? 'vehicle-marker' : 'destination-marker'}
        style={{
          alignItems: 'center',
          background: palette.background,
          border: `2px solid ${palette.border}`,
          borderRadius: '999px',
          boxShadow: `0 14px 28px ${palette.shadow}`,
          color: palette.foreground,
          display: 'flex',
          height: '44px',
          justifyContent: 'center',
          width: '44px',
        }}
      >
        {icon}
      </div>,
    ),
    iconAnchor: [22, 22],
    iconSize: [44, 44],
  })
}

function FitBounds({ positions }: { positions: Coordinate[] }) {
  const map = useMap()
  const previousPositionsRef = useRef<string | null>(null)

  useEffect(() => {
    const positionsKey = JSON.stringify(positions)
    if (previousPositionsRef.current === positionsKey) {
      return
    }
    previousPositionsRef.current = positionsKey

    if (positions.length === 1) {
      map.setView(positions[0], 13)
      return
    }

    map.fitBounds(positions, {
      padding: [36, 36],
      maxZoom: 13,
    })
  }, [map, positions])

  return null
}

function distanceInKilometers(
  startLatitude: number,
  startLongitude: number,
  targetLatitude: number,
  targetLongitude: number,
) {
  const earthRadiusKilometers = 6371
  const latitudeDistance = ((targetLatitude - startLatitude) * Math.PI) / 180
  const longitudeDistance =
    ((targetLongitude - startLongitude) * Math.PI) / 180
  const a =
    Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2) +
    Math.cos((startLatitude * Math.PI) / 180) *
      Math.cos((targetLatitude * Math.PI) / 180) *
      Math.sin(longitudeDistance / 2) *
      Math.sin(longitudeDistance / 2)

  return earthRadiusKilometers * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)))
}

function formatDistance(distanceKilometers: number) {
  return `${distanceKilometers.toFixed(1).replace('.', ',')} km`
}

export function TrackingMapCard({
  confirmation,
  trackingInfo,
  driverLocation,
  isRefreshing,
  onRefresh,
}: TrackingMapCardProps) {
  if (
    trackingInfo === null ||
    trackingInfo.targetLocationX === null ||
    trackingInfo.targetLocationY === null
  ) {
    return null
  }

  const arrivalThresholdKilometers = 0.08
  const vehiclePosition: Coordinate | null =
    driverLocation === null
      ? null
      : [driverLocation.locationY, driverLocation.locationX]
  const destinationPosition: Coordinate = [
    trackingInfo.targetLocationY,
    trackingInfo.targetLocationX,
  ]
  const remainingDistance =
    vehiclePosition === null
      ? null
      : distanceInKilometers(
          vehiclePosition[0],
          vehiclePosition[1],
          trackingInfo.targetLocationY,
          trackingInfo.targetLocationX,
        )
  const hasArrived =
    remainingDistance !== null && remainingDistance <= arrivalThresholdKilometers
  const badgeText =
    remainingDistance === null
      ? 'Standort laden'
      : hasArrived
        ? 'Angekommen'
        : `Noch ${formatDistance(remainingDistance)}`
  const titleText =
    remainingDistance === null
      ? 'Fahrerstandort abrufen'
      : hasArrived
        ? 'Ihr Fahrzeug ist angekommen'
        : 'Ihr Fahrzeug ist unterwegs'
  const descriptionText =
    remainingDistance !== null && hasArrived
      ? 'Das Fahrzeug hat die Lieferadresse erreicht.'
      : `Zieladresse: ${confirmation.deliveryAddress}`
  const positions = vehiclePosition === null
    ? [destinationPosition]
    : [vehiclePosition, destinationPosition]

  return (
    <Card className="overflow-hidden rounded-[2rem] border-0 bg-white/90 shadow-[0_28px_80px_rgba(15,23,42,0.12)] backdrop-blur">
      <CardHeader className="gap-4 border-b border-stone-200/80 bg-[linear-gradient(135deg,#fff7db_0%,#fffdf7_55%,#eef8f6_100%)] p-6 sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.32em] text-stone-500">
              Live Tracking
            </p>
            <CardTitle className="mt-2 font-heading text-2xl text-stone-950">
              {titleText}
            </CardTitle>
          </div>
          <div
            data-testid="tracking-status-badge"
            className={`inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${
              hasArrived
                ? 'bg-emerald-600 text-white'
                : 'bg-stone-950 text-stone-50'
            }`}
          >
            {hasArrived ? (
              <CircleCheckBig className="size-4" />
            ) : (
              <Route className="size-4" />
            )}
            {badgeText}
          </div>
        </div>
        <div className="max-w-3xl">
          <p className="text-sm leading-6 text-stone-600">{descriptionText}</p>
          <Button
            className="mt-4 inline-flex h-auto rounded-full bg-stone-950 px-4 py-2 text-sm font-semibold text-stone-50 hover:bg-stone-800"
            disabled={isRefreshing}
            type="button"
            onClick={onRefresh}
          >
            {isRefreshing ? (
              <LoaderCircle className="mr-2 size-4 animate-spin" />
            ) : (
              <RefreshCw className="mr-2 size-4" />
            )}
            {isRefreshing ? 'Wird aktualisiert...' : 'Aktualisieren'}
          </Button>
        </div>
      </CardHeader>

      <CardContent className="p-4 sm:p-6">
        <div className="overflow-hidden rounded-[1.75rem] border border-stone-200 bg-[radial-gradient(circle_at_top,#fffdf5,transparent_48%),linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] p-3">
          <div
            className="h-[360px] overflow-hidden rounded-[1.35rem] border border-stone-200"
            data-testid="tracking-map"
          >
            <MapContainer
              className="h-full w-full"
              center={destinationPosition}
              scrollWheelZoom={false}
              zoom={12}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <FitBounds positions={positions} />
              {vehiclePosition !== null ? (
                <>
                  <Polyline
                    pathOptions={{
                      color: hasArrived ? '#059669' : '#f59e0b',
                      dashArray: hasArrived ? undefined : '12 12',
                      lineCap: 'round',
                      opacity: 0.9,
                      weight: 5,
                    }}
                    positions={[vehiclePosition, destinationPosition]}
                  />
                  <Marker
                    icon={createMarkerIcon('vehicle')}
                    position={vehiclePosition}
                  >
                    <Tooltip
                      direction="top"
                      offset={[0, -22]}
                      opacity={1}
                      permanent
                    >
                      {badgeText}
                    </Tooltip>
                  </Marker>
                </>
              ) : null}
              <Marker
                icon={createMarkerIcon('destination')}
                position={destinationPosition}
              />
            </MapContainer>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
