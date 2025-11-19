import React, { useState, useEffect } from 'react';

const API_URL = 'http://localhost:8080/api';
const AUTH_URL = 'http://localhost:8080/api/v1/auth';

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
            const response = await fetch(`${API_URL}/patients`);
            if (response.ok) {
                const data = await response.json();
                setPatients(data);
                console.log('Patienter hämtade från HAPI:', data);
            }
        } catch (error) {
            console.error('Fel vid hämtning av patienter:', error);
            setError('Kunde inte hämta patienter från servern');
        }
    };

    const fetchPractitioners = async () => {
        try {
            const response = await fetch(`${API_URL}/practitioners`);
            if (response.ok) {
                const data = await response.json();
                setPractitioners(data);
                console.log('Practitioners hämtade från HAPI:', data);
            }
        } catch (error) {
            console.error('Fel vid hämtning av practitioners:', error);
            setError('Kunde inte hämta vårdpersonal från servern');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        if (!foreignId) {
            setError('Du måste välja en person att koppla kontot till');
            setLoading(false);
            return;
        }

        const fhirUuid = foreignId;
        console.log('Skapar användare med FHIR UUID:', fhirUuid);

        try {
            const userResponse = await fetch(`${AUTH_URL}/register`, {
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
                    setError('Denna person har redan ett användarkonto. Välj en annan person.');
                } else if (errorData.includes('Username taken')) {
                    setError('Användarnamnet är upptaget. Välj ett annat användarnamn.');
                } else {
                    setError(errorData || 'Registrering misslyckades');
                }
            }
        } catch (err) {
            setError(err.message || 'Kunde inte ansluta till servern');
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
                <h2 style={{ color: '#4caf50' }}>✓ Registrering lyckades!</h2>
                <p>Ditt konto har skapats. Du omdirigeras till inloggningssidan...</p>
            </div>
        );
    }

    const availablePersons = role === 'PATIENT' ? patients : practitioners;
    const personLabel = role === 'PATIENT' ? 'Patient' : 'Vårdpersonal';

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
                Skapa konto
            </h2>

            <form onSubmit={handleSubmit}>
                {/* Roll */}
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Användartyp *
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
                        <option value="DOCTOR">Läkare</option>
                        <option value="STAFF">Övrig personal</option>
                    </select>
                </div>

                <hr style={{ margin: '30px 0', border: 'none', borderTop: '1px solid #eee' }} />

                {/* Välj Patient/Practitioner från HAPI */}
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Välj {personLabel} *
                    </label>
                    <select
                        value={foreignId}
                        onChange={(e) => {
                            console.log('Valt value:', e.target.value);
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
                        <option value="">-- Välj {personLabel} --</option>
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
                            Laddar {personLabel.toLowerCase()}...
                        </small>
                    )}
                </div>

                <hr style={{ margin: '30px 0', border: 'none', borderTop: '1px solid #eee' }} />

                <h3 style={{ marginBottom: '20px', fontSize: '18px' }}>Kontoinformation</h3>

                {/* Användarnamn */}
                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Användarnamn *
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

                {/* E-post */}
                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        E-post *
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

                {/* Lösenord */}
                <div style={{ marginBottom: '20px' }}>
                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Lösenord *
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
                        Minst 6 tecken
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
                    {loading ? 'Registrerar...' : 'Registrera'}
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
                    Tillbaka till inloggning
                </button>
            </div>
        </div>
    );
}

export default Register;