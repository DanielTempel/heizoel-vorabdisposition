import 'leaflet/dist/leaflet.css'
import { useEffect } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import L from 'leaflet'
import { MapPinned, Route, Truck } from 'lucide-react'
import {
  MapContainer,
  Marker,
  Polyline,
  TileLayer,
  Tooltip,
  useMap,
} from 'react-leaflet'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { CustomerConfirmationPreview } from '../../../types/confirmation'

type TrackingMapCardProps = {
  confirmation: CustomerConfirmationPreview
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

  useEffect(() => {
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

export function TrackingMapCard({ confirmation }: TrackingMapCardProps) {
  const vehiclePosition: Coordinate = [
    confirmation.locationY,
    confirmation.locationX,
  ]
  const destinationPosition: Coordinate = [
    confirmation.targetLocationY,
    confirmation.targetLocationX,
  ]
  const remainingDistance = distanceInKilometers(
    confirmation.locationY,
    confirmation.locationX,
    confirmation.targetLocationY,
    confirmation.targetLocationX,
  )

  return (
    <Card className="overflow-hidden rounded-[2rem] border-0 bg-white/90 shadow-[0_28px_80px_rgba(15,23,42,0.12)] backdrop-blur">
      <CardHeader className="gap-4 border-b border-stone-200/80 bg-[linear-gradient(135deg,#fff7db_0%,#fffdf7_55%,#eef8f6_100%)] p-6 sm:p-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.32em] text-stone-500">
              Live Tracking
            </p>
            <CardTitle className="mt-2 font-heading text-2xl text-stone-950">
              Ihr Fahrzeug ist unterwegs
            </CardTitle>
          </div>
          <div className="inline-flex items-center gap-2 rounded-full bg-stone-950 px-4 py-2 text-sm font-semibold text-stone-50">
            <Route className="size-4" />
            Noch {formatDistance(remainingDistance)}
          </div>
        </div>
        <p className="max-w-3xl text-sm leading-6 text-stone-600">
          Zieladresse: {confirmation.deliveryAddress}
        </p>
      </CardHeader>

      <CardContent className="p-4 sm:p-6">
        <div className="overflow-hidden rounded-[1.75rem] border border-stone-200 bg-[radial-gradient(circle_at_top,#fffdf5,transparent_48%),linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] p-3">
          <div className="h-[360px] overflow-hidden rounded-[1.35rem] border border-stone-200">
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
              <FitBounds positions={[vehiclePosition, destinationPosition]} />
              <Polyline
                pathOptions={{
                  color: '#f59e0b',
                  dashArray: '12 12',
                  lineCap: 'round',
                  opacity: 0.9,
                  weight: 5,
                }}
                positions={[vehiclePosition, destinationPosition]}
              />
              <Marker icon={createMarkerIcon('vehicle')} position={vehiclePosition}>
                <Tooltip
                  direction="top"
                  offset={[0, -22]}
                  opacity={1}
                  permanent
                >
                  Noch {formatDistance(remainingDistance)}
                </Tooltip>
              </Marker>
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
