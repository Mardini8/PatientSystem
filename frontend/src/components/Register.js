import React, { useState, useEffect } from 'react';
import API_CONFIG from '../config/api';

function Register({ onRegisterSuccess, onBackToLogin }) {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState('PATIENT');
    const [foreignId, setForeignId] = useState('');

    const [patients, setPatients] = useState([]);
    const [practitioners, setPractitioners] = useState([]);
    const [loading, setLoading] = useState(false);

    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    useEffect(() => {
        if (role === 'PATIENT') {
            fetchPatients();
        } else if (role === 'DOCTOR' || role === 'STAFF') {
            fetchPractitioners();
        }
    }, [role]);

    const fetchPatients = async () => {
        try {
            const response = await fetch(`${API_CONFIG.CLINICAL_SERVICE}/api/patients`);
            if (response.ok) {
                const data = await response.json();
                setPatients(data);
                console.log('Patients fetched:', data);
            }
        } catch (error) {
            console.error('Error fetching patients:', error);
            setError('Could not fetch patients from server');
        }
    };

    const fetchPractitioners = async () => {
        try {
            const response = await fetch(`${API_CONFIG.CLINICAL_SERVICE}/api/practitioners`);
            if (response.ok) {
                const data = await response.json();
                setPractitioners(data);
                console.log('Practitioners fetched:', data);
            }
        } catch (error) {
            console.error('Error fetching practitioners:', error);
            setError('Could not fetch practitioners from server');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        if (!foreignId) {
            setError('You must select a person to link the account to');
            setLoading(false);
            return;
        }

        const fhirUuid = foreignId;
        console.log('Creating user with FHIR UUID:', fhirUuid);

        try {
            const userResponse = await fetch(`${API_CONFIG.USER_SERVICE}/api/v1/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username,
                    email,
                    password,
                    role,
                    foreignId: fhirUuid
                })
            });

            if (userResponse.ok) {
                setSuccess(true);
                setTimeout(() => {
                    onRegisterSuccess();
                }, 2000);
            } else {
                const errorData = await userResponse.text();
                if (errorData.includes('already registered')) {
                    setError('This person already has a user account. Choose another person.');
                } else if (errorData.includes('Username taken')) {
                    setError('Username is taken. Choose another username.');
                } else {
                    setError(errorData || 'Registration failed');
                }
            }
        } catch (err) {
            setError(err.message || 'Could not connect to server');
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return (
            <div style={{
                background: 'white',
                padding: '40px',
                borderRadius: '12px',
                boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
                width: '100%',
                maxWidth: '500px',
                textAlign: 'center'
            }}>
                <h2 style={{ color: '#4caf50' }}>✓ Registration successful!</h2>
                <p>Your account has been created. Redirecting to login page...</p>
            </div>
        );
    }

    const availablePersons = role === 'PATIENT' ? patients : practitioners;
    const personLabel = role === 'PATIENT' ? 'Patient' : 'Healthcare Professional';

    return (
        <div style={{
            background: 'white',
            padding: '40px',
            borderRadius: '12px',
            boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
            width: '100%',
            maxWidth: '500px',
            maxHeight: '90vh',
            overflowY: 'auto'
        }}>
            <h2 style={{ textAlign: 'center', marginBottom: '30px', color: '#333' }}>
                Create Account
            </h2>

            <form onSubmit={handleSubmit}>
                {/* Role Selection */}
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        User Type *
                    </label>
                    <select
                        value={role}
                        onChange={(e) => {
                            setRole(e.target.value);
                            setForeignId('');
                        }}
                        style={{
                            width: '100%',
                            padding: '12px',
                            border: '1px solid #ddd',
                            borderRadius: '6px',
                            fontSize: '16px',
                            boxSizing: 'border-box'
                        }}
                    >
                        <option value="PATIENT">Patient</option>
                        <option value="DOCTOR">Doctor</option>
                        <option value="STAFF">Staff</option>
                    </select>
                </div>

                <hr style={{ margin: '30px 0', border: 'none', borderTop: '1px solid #eee' }} />

                {/* Select Patient/Practitioner */}
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Select {personLabel} *
                    </label>
                    <select
                        value={foreignId}
                        onChange={(e) => {
                            console.log('Selected value:', e.target.value);
                            setForeignId(e.target.value);
                        }}
                        required
                        style={{
                            width: '100%',
                            padding: '12px',
                            border: '1px solid #ddd',
                            borderRadius: '6px',
                            fontSize: '16px',
                            boxSizing: 'border-box'
                        }}
                    >
                        <option value="">-- Select {personLabel} --</option>
                        {availablePersons.map(person => (
                            <option
                                key={person.socialSecurityNumber}
                                value={person.socialSecurityNumber}
                            >
                                {person.firstName} {person.lastName}
                                {person.socialSecurityNumber && ` (${person.socialSecurityNumber.substring(0, 8)}...)`}
                            </option>
                        ))}
                    </select>
                    {availablePersons.length === 0 && (
                        <small style={{ color: '#999', fontSize: '12px', marginTop: '5px', display: 'block' }}>
                            Loading {personLabel.toLowerCase()}...
                        </small>
                    )}
                </div>

                <hr style={{ margin: '30px 0', border: 'none', borderTop: '1px solid #eee' }} />

                <h3 style={{ marginBottom: '20px', fontSize: '18px' }}>Account Information</h3>

                {/* Username */}
                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Username *
                    </label>
                    <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        style={{
                            width: '100%',
                            padding: '12px',
                            border: '1px solid #ddd',
                            borderRadius: '6px',
                            fontSize: '16px',
                            boxSizing: 'border-box'
                        }}
                    />
                </div>

                {/* Email */}
                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Email *
                    </label>
                    <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        style={{
                            width: '100%',
                            padding: '12px',
                            border: '1px solid #ddd',
                            borderRadius: '6px',
                            fontSize: '16px',
                            boxSizing: 'border-box'
                        }}
                    />
                </div>

                {/* Password */}
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Password *
                    </label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        minLength={6}
                        style={{
                            width: '100%',
                            padding: '12px',
                            border: '1px solid #ddd',
                            borderRadius: '6px',
                            fontSize: '16px',
                            boxSizing: 'border-box'
                        }}
                    />
                    <small style={{ color: '#666', fontSize: '12px' }}>
                        At least 6 characters
                    </small>
                </div>

                {error && (
                    <div style={{
                        padding: '10px',
                        marginBottom: '20px',
                        background: '#fee',
                        color: '#c33',
                        borderRadius: '6px',
                        fontSize: '14px'
                    }}>
                        {error}
                    </div>
                )}

                <button
                    type="submit"
                    disabled={loading}
                    style={{
                        width: '100%',
                        padding: '12px',
                        background: loading ? '#ccc' : '#667eea',
                        color: 'white',
                        border: 'none',
                        borderRadius: '6px',
                        fontSize: '16px',
                        fontWeight: '600',
                        cursor: loading ? 'not-allowed' : 'pointer',
                        transition: 'background 0.3s'
                    }}
                >
                    {loading ? 'Registering...' : 'Register'}
                </button>
            </form>

            <div style={{ textAlign: 'center', marginTop: '20px' }}>
                <button
                    onClick={onBackToLogin}
                    style={{
                        background: 'none',
                        border: 'none',
                        color: '#667eea',
                        cursor: 'pointer',
                        fontSize: '14px',
                        textDecoration: 'underline'
                    }}
                >
                    Back to login
                </button>
            </div>
        </div>
    );
}

export default Register;