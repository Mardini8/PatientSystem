import React from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProfileSetup from './components/ProfileSetup';
import PractitionerDashboard from './components/PractitionerDashboard';
import PatientDashboard from './components/PatientDashboard';

// Loading component
const LoadingScreen = () => (
    <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white'
    }}>
        <div style={{
            width: '48px',
            height: '48px',
            border: '4px solid rgba(255, 255, 255, 0.3)',
            borderTopColor: 'white',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite'
        }} />
        <p style={{ marginTop: '16px', fontSize: '18px' }}>Laddar...</p>
        <style>{`
      @keyframes spin {
        to { transform: rotate(360deg); }
      }
    `}</style>
    </div>
);

// Main app content
const AppContent = () => {
    const { isLoading, isAuthenticated, needsProfileSetup, user, logout, hasRole } = useAuth();

    // Show loading while initializing
    if (isLoading) {
        return <LoadingScreen />;
    }

    // Not authenticated yet - Keycloak will redirect
    if (!isAuthenticated) {
        return <LoadingScreen />;
    }

    // User not loaded yet
    if (!user) {
        return <LoadingScreen />;
    }

    // Show profile setup if user hasn't completed it
    if (needsProfileSetup) {
        return <ProfileSetup />;
    }

    // Convert user to format expected by dashboards
    const dashboardUser = {
        id: user.id,
        username: user.username,
        email: user.email,
        role: user.role, // Already uppercase from AuthContext
        foreignId: user.foreignId,
    };

    // Route to correct dashboard based on role
    if (hasRole('DOCTOR') || hasRole('STAFF')) {
        return <PractitionerDashboard user={dashboardUser} onLogout={logout} />;
    } else if (hasRole('PATIENT')) {
        return <PatientDashboard user={dashboardUser} onLogout={logout} />;
    }

    // Fallback - should not happen if profile setup worked
    return (
        <div style={{
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            textAlign: 'center',
            padding: '20px'
        }}>
            <h1>Något gick fel</h1>
            <p>Din roll kunde inte identifieras: {user?.role || 'okänd'}</p>
            <button
                onClick={logout}
                style={{
                    marginTop: '20px',
                    padding: '12px 24px',
                    background: 'white',
                    color: '#667eea',
                    border: 'none',
                    borderRadius: '8px',
                    fontSize: '16px',
                    cursor: 'pointer'
                }}
            >
                Logga ut
            </button>
        </div>
    );
};

// Root App component with AuthProvider
function App() {
    return (
        <AuthProvider>
            <AppContent />
        </AuthProvider>
    );
}

export default App;