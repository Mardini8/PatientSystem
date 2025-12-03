import React, { useState, useEffect } from 'react';
import Login from './components/Login';
import Register from './components/Register';
import PractitionerDashboard from './components/PractitionerDashboard';
import PatientDashboard from './components/PatientDashboard';

function App() {
    const [currentUser, setCurrentUser] = useState(null);
    const [showRegister, setShowRegister] = useState(false);
    const [loading, setLoading] = useState(true);

    // Load user from localStorage on mount
    useEffect(() => {
        const savedUser = localStorage.getItem('currentUser');
        if (savedUser) {
            try {
                const user = JSON.parse(savedUser);
                setCurrentUser(user);
                console.log('User loaded from localStorage:', user);
            } catch (error) {
                console.error('Error parsing saved user:', error);
                localStorage.removeItem('currentUser');
            }
        }
        setLoading(false);
    }, []);

    const handleLoginSuccess = (user) => {
        setCurrentUser(user);
        // Save user to localStorage
        localStorage.setItem('currentUser', JSON.stringify(user));
        console.log('User logged in and saved to localStorage:', user);
    };

    const handleLogout = () => {
        setCurrentUser(null);
        // Remove user from localStorage
        localStorage.removeItem('currentUser');
        console.log('User logged out and removed from localStorage');
    };

    // Show loading while checking for saved session
    if (loading) {
        return (
            <div style={{
                minHeight: '100vh',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: '18px'
            }}>
                Loading...
            </div>
        );
    }

    if (currentUser) {
        if (currentUser.role === 'DOCTOR' || currentUser.role === 'STAFF') {
            return <PractitionerDashboard user={currentUser} onLogout={handleLogout} />;
        } else if (currentUser.role === 'PATIENT') {
            return <PatientDashboard user={currentUser} onLogout={handleLogout} />;
        }
    }

    return (
        <div className="App" style={{
            minHeight: '100vh',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px'
        }}>
            {showRegister ? (
                <Register
                    onRegisterSuccess={() => setShowRegister(false)}
                    onBackToLogin={() => setShowRegister(false)}
                />
            ) : (
                <Login
                    onLoginSuccess={handleLoginSuccess}
                    onShowRegister={() => setShowRegister(true)}
                />
            )}
        </div>
    );
}

export default App;