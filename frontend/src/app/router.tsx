import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { DashboardLayout } from '@/layouts/dashboard-layout'
import { ConfirmationPage } from '@/pages/confirmation/page'
import { DashboardPage } from '@/pages/dashboard/page'
import { LoginPage } from '@/pages/login/page'
import { OrderDetailPage } from '@/pages/order-detail/page'
import { SettingsPage } from '@/pages/settings/page'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/confirmation/:token" element={<ConfirmationPage />} />

        <Route path="/dashboard" element={<DashboardLayout />}>
          <Route index element={<DashboardPage />} />
          <Route
            path="orders/:externalOrderId"
            element={<OrderDetailPage />}
          />
          <Route path="settings" element={<SettingsPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
