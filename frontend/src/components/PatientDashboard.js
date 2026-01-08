import React, { useState, useEffect, useCallback } from 'react';
import MessagingSystem from './MessagingSystem';
import ImageGallery from './ImageGallery';
import API_CONFIG, { fetchWithAuth } from '../config/api';

function PatientDashboard({ user, onLogout }) {
    const [activeTab, setActiveTab] = useState('overview');
    const [patientInfo, setPatientInfo] = useState(null);
    const [observations, setObservations] = useState([]);
    const [conditions, setConditions] = useState([]);
    const [encounters, setEncounters] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const patientId = user.foreignId;

    const fetchPatientData = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            console.log('Fetching data for patient with FHIR UUID:', patientId);

            // Use fetchWithAuth instead of fetch
            const allPatientsRes = await fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/patients`);
            if (allPatientsRes.ok) {
                const allPatients = await allPatientsRes.json();
                const patient = allPatients.find(p => p.socialSecurityNumber === patientId);

                if (patient) {
                    setPatientInfo(patient);
                    console.log('Patient fetched from HAPI:', patient);

                    // Use fetchWithAuth for all clinical data
                    const [obsRes, condRes, encRes] = await Promise.all([
                        fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/observations/patient/${patientId}`),
                        fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/conditions/patient/${patientId}`),
                        fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/encounters/patient/${patientId}`)
                    ]);

                    if (obsRes.ok) {
                        const obs = await obsRes.json();
                        setObservations(obs);
                        console.log('Observations fetched:', obs.length);
                    } else {
                        console.error('Could not fetch observations, status:', obsRes.status);
                    }

                    if (condRes.ok) {
                        const cond = await condRes.json();
                        setConditions(cond);
                        console.log('Conditions fetched:', cond.length);
                    } else {
                        console.error('Could not fetch conditions, status:', condRes.status);
                    }

                    if (encRes.ok) {
                        const enc = await encRes.json();
                        setEncounters(enc);
                        console.log('Encounters fetched:', enc.length);
                    } else {
                        console.error('Could not fetch encounters, status:', encRes.status);
                    }
                } else {
                    console.error('Could not find patient with UUID:', patientId);
                    setError('Could not find your patient record');
                }
            } else {
                console.error('Could not fetch patients, status:', allPatientsRes.status);
                setError(`Could not load patient list (${allPatientsRes.status})`);
            }

        } catch (error) {
            console.error('Error fetching patient data:', error);
            setError('Could not connect to server: ' + error.message);
        } finally {
            setLoading(false);
        }
    }, [patientId]);

    useEffect(() => {
        fetchPatientData();
    }, [fetchPatientData]);

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
        },
        errorBox: {
            background: '#fee',
            border: '1px solid #fcc',
            borderRadius: '8px',
            padding: '20px',
            color: '#c00',
            marginBottom: '20px'
        }
    };

    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <div>
                    <h1 style={{ margin: 0 }}>My Journal</h1>
                    <p style={{ margin: '5px 0 0 0', opacity: 0.9 }}>
                        Logged in as: {user.username}
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
                    Log out
                </button>
            </header>

            <nav style={styles.nav}>
                <button
                    style={styles.navButton(activeTab === 'overview')}
                    onClick={() => setActiveTab('overview')}
                >
                    Overview
                </button>
                <button
                    style={styles.navButton(activeTab === 'observations')}
                    onClick={() => setActiveTab('observations')}
                >
                    Observations
                </button>
                <button
                    style={styles.navButton(activeTab === 'conditions')}
                    onClick={() => setActiveTab('conditions')}
                >
                    Diagnoses
                </button>
                <button
                    style={styles.navButton(activeTab === 'encounters')}
                    onClick={() => setActiveTab('encounters')}
                >
                    Visits
                </button>
                <button
                    style={styles.navButton(activeTab === 'messages')}
                    onClick={() => setActiveTab('messages')}
                >
                    Messages
                </button>
                <button
                    style={styles.navButton(activeTab === 'images')}
                    onClick={() => setActiveTab('images')}
                >
                    Images
                </button>
            </nav>

            <div style={styles.content}>
                {error && (
                    <div style={styles.errorBox}>
                        <strong>Error:</strong> {error}
                        <button
                            onClick={fetchPatientData}
                            style={{ marginLeft: '20px', padding: '5px 10px' }}
                        >
                            Try again
                        </button>
                    </div>
                )}

                {loading ? (
                    <div style={styles.card}>
                        <p>Loading...</p>
                    </div>
                ) : (
                    <>
                        {activeTab === 'overview' && patientInfo && (
                            <div style={styles.card}>
                                <h2 style={{ marginBottom: '30px' }}>My Information</h2>
                                <div style={styles.infoRow}>
                                    <strong>Name:</strong>
                                    <span>{patientInfo.firstName} {patientInfo.lastName}</span>
                                </div>
                                <div style={styles.infoRow}>
                                    <strong>Personal ID Number:</strong>
                                    <span>{patientInfo.socialSecurityNumber}</span>
                                </div>
                                <div style={styles.infoRow}>
                                    <strong>Date of Birth:</strong>
                                    <span>{patientInfo.dateOfBirth}</span>
                                </div>
                            </div>
                        )}

                        {activeTab === 'observations' && (
                            <div style={styles.card}>
                                <h2>My Observations</h2>
                                {observations.length === 0 ? (
                                    <p>No observations registered</p>
                                ) : (
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Date</th>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Description</th>
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
                                <h2>My Diagnoses</h2>
                                {conditions.length === 0 ? (
                                    <p>No diagnoses registered</p>
                                ) : (
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Date</th>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Description</th>
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
                                <h2>My Visits</h2>
                                {encounters.length === 0 ? (
                                    <p>No visits registered</p>
                                ) : (
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                        <tr>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>Start Time</th>
                                            <th style={{ textAlign: 'left', padding: '10px', borderBottom: '2px solid #ddd' }}>End Time</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {encounters.map(enc => (
                                            <tr key={enc.id}>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>
                                                    {new Date(enc.startTime).toLocaleString()}
                                                </td>
                                                <td style={{ padding: '10px', borderBottom: '1px solid #eee' }}>
                                                    {enc.endTime ? new Date(enc.endTime).toLocaleString() : 'Ongoing'}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        )}

                        {activeTab === 'messages' && (
                            <MessagingSystem currentUser={user} patientPersonnummer={patientId} />
                        )}

                        {activeTab === 'images' && (
                            <ImageGallery currentUser={user} patientPersonnummer={patientId} />
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

export default PatientDashboard;