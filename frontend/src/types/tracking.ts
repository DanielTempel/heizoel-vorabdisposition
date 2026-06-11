export type DriverLocation = {
  locationX: number
  locationY: number
}

export type TrackingInfo = {
  trackingAvailable: boolean
  targetLocationX: number | null
  targetLocationY: number | null
}
