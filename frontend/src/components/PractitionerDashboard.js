import React, { useState, useEffect, useCallback } from 'react';
import MessagingSystem from './MessagingSystem';
import ImageGallery from './ImageGallery';
import SearchPanel from './SearchPanel';
import API_CONFIG, { fetchWithAuth } from '../config/api';

function PractitionerDashboard({ user, onLogout }) {
    const [activeTab, setActiveTab] = useState('patients');
    const [patients, setPatients] = useState([]);
    const [selectedPatient, setSelectedPatient] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const isDoctor = user.role === 'DOCTOR';

    useEffect(() => {
        if (activeTab === 'patients') {
            fetchPatients();
        }
    }, [activeTab]);

    const fetchPatients = async () => {
        setLoading(true);
        setError(null);
        try {
            // Use fetchWithAuth instead of fetch
            const response = await fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/patients`);
            if (response.ok) {
                const data = await response.json();
                setPatients(data);
            } else {
                setError(`Could not load patients (${response.status})`);
            }
        } catch (error) {
            console.error('Error fetching patients:', error);
            setError('Could not connect to server: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const viewPatientDetails = async (patient) => {
        setSelectedPatient(patient);
        setActiveTab('patient-details');
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
        table: {
            width: '100%',
            borderCollapse: 'collapse'
        },
        th: {
            textAlign: 'left',
            padding: '12px',
            borderBottom: '2px solid #ddd',
            fontWeight: '600',
            color: '#333'
        },
        td: {
            padding: '12px',
            borderBottom: '1px solid #eee'
        },
        button: {
            padding: '8px 16px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px'
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
                    <h1 style={{ margin: 0 }}>PatientSystem</h1>
                    <p style={{ margin: '5px 0 0 0', opacity: 0.9 }}>
                        Logged in as: {user.username} ({isDoctor ? 'Doctor' : 'Staff'})
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
                    style={styles.navButton(activeTab === 'patients')}
                    onClick={() => setActiveTab('patients')}
                >
                    Patients
                </button>
                <button
                    style={styles.navButton(activeTab === 'search')}
                    onClick={() => setActiveTab('search')}
                >
                    Search
                </button>
                <button
                    style={styles.navButton(activeTab === 'messages')}
                    onClick={() => setActiveTab('messages')}
                >
                    Messages
                </button>
            </nav>

            <div style={styles.content}>
                {error && (
                    <div style={styles.errorBox}>
                        <strong>Error:</strong> {error}
                        <button
                            onClick={fetchPatients}
                            style={{ marginLeft: '20px', padding: '5px 10px' }}
                        >
                            Try again
                        </button>
                    </div>
                )}

                {activeTab === 'patients' && (
                    <div style={styles.card}>
                        <h2>Patient List</h2>
                        {!isDoctor && (
                            <p style={{ color: '#666', fontSize: '14px', marginBottom: '20px' }}>
                                As staff, you can register clinical data for patients
                            </p>
                        )}
                        {loading ? (
                            <p>Loading patients...</p>
                        ) : (
                            <table style={styles.table}>
                                <thead>
                                <tr>
                                    <th style={styles.th}>First Name</th>
                                    <th style={styles.th}>Last Name</th>
                                    <th style={styles.th}>Personal ID Number</th>
                                    <th style={styles.th}>Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                {patients.map(patient => (
                                    <tr key={patient.socialSecurityNumber}>
                                        <td style={styles.td}>{patient.firstName}</td>
                                        <td style={styles.td}>{patient.lastName}</td>
                                        <td style={styles.td}>{patient.socialSecurityNumber}</td>
                                        <td style={styles.td}>
                                            <button
                                                style={styles.button}
                                                onClick={() => viewPatientDetails(patient)}
                                            >
                                                {isDoctor ? 'View Details' : 'Register Data'}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {activeTab === 'patient-details' && selectedPatient && (
                    <>
                        <PatientDetails
                            patient={selectedPatient}
                            practitionerPersonnummer={user.foreignId}
                            isDoctor={isDoctor}
                            onBack={() => setActiveTab('patients')}
                        />

                        {/* Images section - Only for Doctors */}
                        {isDoctor && (
                            <div style={{ marginTop: '30px' }}>
                                <ImageGallery
                                    currentUser={user}
                                    patientPersonnummer={selectedPatient.socialSecurityNumber}
                                />
                            </div>
                        )}
                    </>
                )}

                {activeTab === 'search' && (
                    <SearchPanel currentUser={user} />
                )}

                {activeTab === 'messages' && (
                    <MessagingSystem currentUser={user} patientPersonnummer={null} />
                )}
            </div>
        </div>
    );
}

function PatientDetails({ patient, practitionerPersonnummer, isDoctor, onBack }) {
    const [observations, setObservations] = useState([]);
    const [conditions, setConditions] = useState([]);
    const [encounters, setEncounters] = useState([]);
    const [showAddObservation, setShowAddObservation] = useState(false);
    const [showAddCondition, setShowAddCondition] = useState(false);
    const [showAddEncounter, setShowAddEncounter] = useState(false);

    const [newObservation, setNewObservation] = useState({
        description: '',
        value: '',
        unit: '',
        effectiveDate: new Date().toISOString().slice(0, 10)
    });

    const [newCondition, setNewCondition] = useState({
        description: '',
        assertedDate: new Date().toISOString().slice(0, 10)
    });

    const [newEncounter, setNewEncounter] = useState({
        startTime: new Date().toISOString().slice(0, 16),
        endTime: ''
    });

    const fetchPatientData = useCallback(async () => {
        try {
            const patientPersonnummer = patient.socialSecurityNumber;

            // Use fetchWithAuth for all API calls
            const [obsRes, condRes, encRes] = await Promise.all([
                fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/observations/patient/${patientPersonnummer}`),
                fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/conditions/patient/${patientPersonnummer}`),
                fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/encounters/patient/${patientPersonnummer}`)
            ]);

            if (obsRes.ok) {
                const obs = await obsRes.json();
                setObservations(obs);
            }
            if (condRes.ok) {
                const cond = await condRes.json();
                setConditions(cond);
            }
            if (encRes.ok) {
                const enc = await encRes.json();
                setEncounters(enc);
            }
        } catch (error) {
            console.error('Error fetching patient data:', error);
        }
    }, [patient.socialSecurityNumber]);

    useEffect(() => {
        fetchPatientData();
    }, [fetchPatientData]);

    const handleAddObservation = async (e) => {
        e.preventDefault();
        try {
            // Use fetchWithAuth for POST request
            const response = await fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/observations`, {
                method: 'POST',
                body: JSON.stringify({
                    patientPersonnummer: patient.socialSecurityNumber,
                    performerPersonnummer: practitionerPersonnummer,
                    description: newObservation.description,
                    value: newObservation.value,
                    unit: newObservation.unit,
                    effectiveDate: newObservation.effectiveDate
                })
            });

            if (response.ok) {
                alert('Observation created in HAPI FHIR!');
                setShowAddObservation(false);
                setNewObservation({
                    description: '',
                    value: '',
                    unit: '',
                    effectiveDate: new Date().toISOString().slice(0, 10)
                });
                fetchPatientData();
            } else {
                const errorText = await response.text();
                console.error('Error from server:', errorText);
                alert('Error creating observation: ' + errorText);
            }
        } catch (error) {
            console.error('Error creating observation:', error);
            alert('Could not create observation: ' + error.message);
        }
    };

    const handleAddCondition = async (e) => {
        e.preventDefault();
        try {
            // Use fetchWithAuth for POST request
            const response = await fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/conditions`, {
                method: 'POST',
                body: JSON.stringify({
                    patientPersonnummer: patient.socialSecurityNumber,
                    practitionerPersonnummer: practitionerPersonnummer,
                    description: newCondition.description,
                    assertedDate: newCondition.assertedDate
                })
            });

            if (response.ok) {
                alert('Diagnosis created in HAPI FHIR!');
                setShowAddCondition(false);
                setNewCondition({
                    description: '',
                    assertedDate: new Date().toISOString().slice(0, 10)
                });
                fetchPatientData();
            } else {
                const errorText = await response.text();
                console.error('Error from server:', errorText);
                alert('Error creating diagnosis: ' + errorText);
            }
        } catch (error) {
            console.error('Error creating diagnosis:', error);
            alert('Could not create diagnosis: ' + error.message);
        }
    };

    const handleAddEncounter = async (e) => {
        e.preventDefault();
        try {
            // Use fetchWithAuth for POST request
            const response = await fetchWithAuth(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/encounters`, {
                method: 'POST',
                body: JSON.stringify({
                    patientPersonnummer: patient.socialSecurityNumber,
                    practitionerPersonnummer: practitionerPersonnummer,
                    startTime: newEncounter.startTime,
                    endTime: newEncounter.endTime || null
                })
            });

            if (response.ok) {
                alert('Visit created in HAPI FHIR!');
                setShowAddEncounter(false);
                setNewEncounter({
                    startTime: new Date().toISOString().slice(0, 16),
                    endTime: ''
                });
                fetchPatientData();
            } else {
                const errorText = await response.text();
                console.error('Error from server:', errorText);
                alert('Error creating visit: ' + errorText);
            }
        } catch (error) {
            console.error('Error creating visit:', error);
            alert('Could not create visit: ' + error.message);
        }
    };

    const styles = {
        card: {
            background: 'white',
            borderRadius: '8px',
            padding: '20px',
            marginBottom: '20px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        },
        button: {
            padding: '10px 20px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            marginRight: '10px'
        },
        input: {
            width: '100%',
            padding: '10px',
            border: '1px solid #ddd',
            borderRadius: '4px',
            fontSize: '14px',
            boxSizing: 'border-box',
            marginBottom: '15px'
        },
        textarea: {
            width: '100%',
            padding: '10px',
            border: '1px solid #ddd',
            borderRadius: '4px',
            fontSize: '14px',
            boxSizing: 'border-box',
            marginBottom: '15px',
            minHeight: '100px',
            fontFamily: 'inherit'
        }
    };

    return (
        <div>
            <button style={styles.button} onClick={onBack}>← Back</button>

            <div style={styles.card}>
                <h2>Patient Information {isDoctor ? '(Full access as doctor)' : '(Staff - limited access)'}</h2>
                <p><strong>Name:</strong> {patient.firstName} {patient.lastName}</p>
                <p><strong>Personal ID Number:</strong> {patient.socialSecurityNumber}</p>
                <p><strong>Date of Birth:</strong> {patient.dateOfBirth}</p>
            </div>

            {/* OBSERVATIONS */}
            <div style={styles.card}>
                <h3>Observations</h3>
                <button style={styles.button} onClick={() => setShowAddObservation(!showAddObservation)}>
                    {showAddObservation ? 'Cancel' : '+ Add observation'}
                </button>

                {showAddObservation && (
                    <form onSubmit={handleAddObservation} style={{ marginTop: '20px', padding: '20px', background: '#f9f9f9', borderRadius: '8px' }}>
                        <h4>New observation</h4>
                        <label>
                            Description:
                            <textarea
                                style={styles.textarea}
                                value={newObservation.description}
                                onChange={(e) => setNewObservation({...newObservation, description: e.target.value})}
                                placeholder="Describe the observation"
                                required
                            />
                        </label>
                        <label>
                            Value (optional):
                            <input
                                type="text"
                                style={styles.input}
                                value={newObservation.value}
                                onChange={(e) => setNewObservation({...newObservation, value: e.target.value})}
                                placeholder="E.g. 120"
                            />
                        </label>
                        <label>
                            Unit (optional):
                            <input
                                type="text"
                                style={styles.input}
                                value={newObservation.unit}
                                onChange={(e) => setNewObservation({...newObservation, unit: e.target.value})}
                                placeholder="E.g. mmHg"
                            />
                        </label>
                        <label>
                            Date:
                            <input
                                type="date"
                                style={styles.input}
                                value={newObservation.effectiveDate}
                                onChange={(e) => setNewObservation({...newObservation, effectiveDate: e.target.value})}
                                required
                            />
                        </label>
                        <button type="submit" style={styles.button}>Save observation</button>
                    </form>
                )}

                {/* Doctors see the list, Staff do NOT */}
                {isDoctor && (
                    <>
                        {observations.length === 0 ? (
                            <p>No observations registered</p>
                        ) : (
                            <ul>
                                {observations.map(obs => (
                                    <li key={obs.id} style={{ padding: '10px 0', borderBottom: '1px solid #eee' }}>
                                        {obs.description}
                                        <span style={{ color: '#666', fontSize: '12px', marginLeft: '10px' }}>
                                            ({new Date(obs.effectiveDateTime).toLocaleString()})
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}
            </div>

            {/* CONDITIONS */}
            <div style={styles.card}>
                <h3>Diagnoses</h3>
                <button style={styles.button} onClick={() => setShowAddCondition(!showAddCondition)}>
                    {showAddCondition ? 'Cancel' : '+ Add diagnosis'}
                </button>

                {showAddCondition && (
                    <form onSubmit={handleAddCondition} style={{ marginTop: '20px', padding: '20px', background: '#f9f9f9', borderRadius: '8px' }}>
                        <h4>New diagnosis</h4>
                        <label>
                            Description:
                            <textarea
                                style={styles.textarea}
                                value={newCondition.description}
                                onChange={(e) => setNewCondition({...newCondition, description: e.target.value})}
                                placeholder="Describe the diagnosis"
                                required
                            />
                        </label>
                        <label>
                            Date:
                            <input
                                type="date"
                                style={styles.input}
                                value={newCondition.assertedDate}
                                onChange={(e) => setNewCondition({...newCondition, assertedDate: e.target.value})}
                                required
                            />
                        </label>
                        <button type="submit" style={styles.button}>Save diagnosis</button>
                    </form>
                )}

                {/* Doctors see the list, Staff do NOT */}
                {isDoctor && (
                    <>
                        {conditions.length === 0 ? (
                            <p>No diagnoses registered</p>
                        ) : (
                            <ul>
                                {conditions.map(cond => (
                                    <li key={cond.id} style={{ padding: '10px 0', borderBottom: '1px solid #eee' }}>
                                        {cond.description}
                                        <span style={{ color: '#666', fontSize: '12px', marginLeft: '10px' }}>
                                            ({new Date(cond.assertedDate).toLocaleDateString()})
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}
            </div>

            {/* ENCOUNTERS - Both doctors and staff can see and create */}
            <div style={styles.card}>
                <h3>Visits</h3>
                <button style={styles.button} onClick={() => setShowAddEncounter(!showAddEncounter)}>
                    {showAddEncounter ? 'Cancel' : '+ Register visit'}
                </button>

                {showAddEncounter && (
                    <form onSubmit={handleAddEncounter} style={{ marginTop: '20px', padding: '20px', background: '#f9f9f9', borderRadius: '8px' }}>
                        <h4>New visit</h4>
                        <label>
                            Start time:
                            <input
                                type="datetime-local"
                                style={styles.input}
                                value={newEncounter.startTime}
                                onChange={(e) => setNewEncounter({...newEncounter, startTime: e.target.value})}
                                required
                            />
                        </label>
                        <label>
                            End time (optional):
                            <input
                                type="datetime-local"
                                style={styles.input}
                                value={newEncounter.endTime}
                                onChange={(e) => setNewEncounter({...newEncounter, endTime: e.target.value})}
                            />
                        </label>
                        <button type="submit" style={styles.button}>Save visit</button>
                    </form>
                )}

                {/* Both doctors and staff see the visit list */}
                {encounters.length === 0 ? (
                    <p>No visits registered</p>
                ) : (
                    <ul>
                        {encounters.map(enc => (
                            <li key={enc.id} style={{ padding: '10px 0', borderBottom: '1px solid #eee' }}>
                                Visit: {new Date(enc.startTime).toLocaleString()}
                                {enc.endTime && ` - ${new Date(enc.endTime).toLocaleString()}`}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}

export default PractitionerDashboard;