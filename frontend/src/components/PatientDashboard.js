import React, { useState, useEffect } from 'react';
import MessagingSystem from './MessagingSystem';
import API_CONFIG from '../config/api';

const API_URL = API_CONFIG.CLINICAL_SERVICE;

function PatientDashboard({ user, onLogout }) {
    const [activeTab, setActiveTab] = useState('overview');
    const [patientInfo, setPatientInfo] = useState(null);
    const [observations, setObservations] = useState([]);
    const [conditions, setConditions] = useState([]);
    const [encounters, setEncounters] = useState([]);
    const [loading, setLoading] = useState(false);

    const patientId = user.foreignId;

    useEffect(() => {
        fetchPatientData();
    }, [patientId]);

    const fetchPatientData = async () => {
        setLoading(true);
        try {
            console.log('Hämtar data för patient med FHIR UUID:', patientId);

            const allPatientsRes = await fetch(`${API_URL}/api/patients`);
            if (allPatientsRes.ok) {
                const allPatients = await allPatientsRes.json();
                const patient = allPatients.find(p => p.socialSecurityNumber === patientId);

                if (patient) {
                    setPatientInfo(patient);
                    console.log('Patient hämtad från HAPI:', patient);

                    const [obsRes, condRes, encRes] = await Promise.all([
                        fetch(`${API_URL}/api/v1/clinical/observations/patient/${patientId}`),
                        fetch(`${API_URL}/api/v1/clinical/conditions/patient/${patientId}`),
                        fetch(`${API_URL}/api/v1/clinical/encounters/patient/${patientId}`)
                    ]);

                    if (obsRes.ok) {
                        const obs = await obsRes.json();
                        setObservations(obs);
                        console.log('Observations hämtade:', obs.length);
                    } else {
                        console.error('Kunde inte hämta observations, status:', obsRes.status);
                    }

                    if (condRes.ok) {
                        const cond = await condRes.json();
                        setConditions(cond);
                        console.log('Conditions hämtade:', cond.length);
                    } else {
                        console.error('Kunde inte hämta conditions, status:', condRes.status);
                    }

                    if (encRes.ok) {
                        const enc = await encRes.json();
                        setEncounters(enc);
                        console.log('Encounters hämtade:', enc.length);
                    } else {
                        console.error('Kunde inte hämta encounters, status:', encRes.status);
                    }
                } else {
                    console.error('Kunde inte hitta patient med UUID:', patientId);
                }
            } else {
                console.error('Kunde inte hämta patienter, status:', allPatientsRes.status);
            }

        } catch (error) {
            console.error('Fel vid hämtning av patientdata:', error);
        } finally {
            setLoading(false);
        }
    };

    const styles = {
        container: {
            minHeight: '100vh',
            background: '#f5f5f5',
        },
        header: {
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            padding: '20px 40px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
        },
        nav: {
            background: 'white',
            padding: '0 40px',
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            display: 'flex',
            gap: '20px'
        },
        navButton: (active) => ({
            padding: '15px 20px',
            background: 'none',
            border: 'none',
            borderBottom: active ? '3px solid #667eea' : '3px solid transparent',
            color: active ? '#667eea' : '#666',
            cursor: 'pointer',
            fontWeight: active ? '600' : '400',
            fontSize: '16px'
        }),
        content: {
            padding: '40px',
            maxWidth: '1200px',
            margin: '0 auto'
        },
        card: {
            background: 'white',
            borderRadius: '8px',
            padding: '20px',
            marginBottom: '20px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        },
        infoRow: {
            padding: '10px 0',
            borderBottom: '1px solid #eee',
            display: 'flex',
            justifyContent: 'space-between'
        }
    };

    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <div>
                    <h1 style={{ margin: 0 }}>Min Journal</h1>
                    <p style={{ margin: '5px 0 0 0', opacity: 0.9 }}>
                        Inloggad som: {user.username}
                    </p>
                </div>
                <button
                    onClick={onLogout}
                    style={{
                        padding: '10px 20px',
                        background: 'rgba(255,255,255,0.2)',
                        color: 'white',
                        border: '1px solid white',
                        borderRadius: '4px',
                        cursor: 'pointer'
                    }}
                >
                    Logga ut
                </button>
            </header>

            <nav style={styles.nav}>
                <button
                    style={styles.navButton(activeTab === 'overview')}
                    onClick={() => setActiveTab('overview')}
                >
                    Översikt
                </button>
                <button
                    style={styles.navButton(activeTab === 'observations')}
                    onClick={() => setActiveTab('observations')}
                >
                    Observationer
                </button>
                <button
                    style={styles.navButton(activeTab === 'conditions')}
                    onClick={() => setActiveTab('conditions')}
                >
                    Diagnoser
                </button>
                <button
                    style={styles.navButton(activeTab === 'encounters')}
                    onClick={() => setActiveTab('encounters')}
                >
                    Besök
                </button>
                <button
                    style={styles.navButton(activeTab === 'messages')}
                    onClick={() => setActiveTab('messages')}
                >
                    Meddelanden
                </button>
            </nav>

            <div style={styles.content}>
                {loading ? (
                    <div style={styles.card}>
                        <p>Laddar...</p>
                    </div>
                ) : (
                    <>
                        {activeTab === 'overview' && patientInfo && (
                            <div style={styles.card}>
                                <h2 style={{ marginBottom: '30px' }}>Min Information</h2>
                                <div style={styles.infoRow}>
                                    <strong>Namn:</strong>
                                    <span>{patientInfo.firstName} {patientInfo.lastName}</span>
                                </div>
                                <div style={styles.infoRow}>
                                    <strong>Personnummer:</strong>
                                    <span>{patientInfo.socialSecurityNumber}</span>
                                </div>
                                <div style={styles.infoRow}>
                                    <strong>Födelsedatum:</strong>
                                    <span>{patientInfo.dateOfBirth}</span>
                                </div>
                            </div>
                        )}

                        {activeTab === 'observations' && (
                            <div style={styles.card}>
                                <h2>Mina Observationer</h2>
                                {observations.length === 0 ? (
                                    <p>Inga observationer registrerade</p>
                                ) : (
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Datum</th>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Beskrivning</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {observations.map(obs => (
                                            <tr key={obs.id}>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>
                                                    {new Date(obs.effectiveDateTime).toLocaleString()}
                                                </td>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>{obs.description}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}

                        {activeTab === 'conditions' && (
                            <div style={styles.card}>
                                <h2>Mina Diagnoser</h2>
                                {conditions.length === 0 ? (
                                    <p>Inga diagnoser registrerade</p>
                                ) : (
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Datum</th>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Beskrivning</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {conditions.map(cond => (
                                            <tr key={cond.id}>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>
                                                    {new Date(cond.assertedDate).toLocaleDateString()}
                                                </td>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>{cond.description}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}

                        {activeTab === 'encounters' && (
                            <div style={styles.card}>
                                <h2>Mina Besök</h2>
                                {encounters.length === 0 ? (
                                    <p>Inga besök registrerade</p>
                                ) : (
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Starttid</th>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Sluttid</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {encounters.map(enc => (
                                            <tr key={enc.id}>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>
                                                    {new Date(enc.startTime).toLocaleString()}
                                                </td>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>
                                                    {enc.endTime ? new Date(enc.endTime).toLocaleString() : 'Pågående'}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}

                        {activeTab === 'messages' && (
                            <div style={styles.card}>
                                <h2>Meddelanden</h2>
                                <MessagingSystem currentUser={user} patientPersonnummer={patientId} />
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

export default PatientDashboard;