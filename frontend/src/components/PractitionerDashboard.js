import React, { useState, useEffect } from 'react';
import MessagingSystem from './MessagingSystem';

const API_URL = 'http://localhost:8080/api';

function PractitionerDashboard({ user, onLogout }) {
    const [activeTab, setActiveTab] = useState('patients');
    const [patients, setPatients] = useState([]);
    const [selectedPatient, setSelectedPatient] = useState(null);
    const [loading, setLoading] = useState(false);

    const isDoctor = user.role === 'DOCTOR';

    useEffect(() => {
        if (activeTab === 'patients') {
            fetchPatients();
        }
    }, [activeTab]);

    const fetchPatients = async () => {
        setLoading(true);
        try {
            const response = await fetch(`${API_URL}/patients`);
            if (response.ok) {
                const data = await response.json();
                setPatients(data);
            }
        } catch (error) {
            console.error('Fel vid hämtning av patienter:', error);
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
        }
    };

    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <div>
                    <h1 style={{ margin: 0 }}>PatientSystem</h1>
                    <p style={{ margin: '5px 0 0 0', opacity: 0.9 }}>
                        Inloggad som: {user.username} ({isDoctor ? 'Läkare' : 'Personal'})
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
                    style={styles.navButton(activeTab === 'patients')}
                    onClick={() => setActiveTab('patients')}
                >
                    Patienter
                </button>
                <button
                    style={styles.navButton(activeTab === 'messages')}
                    onClick={() => setActiveTab('messages')}
                >
                    Meddelanden
                </button>
            </nav>

            <div style={styles.content}>
                {activeTab === 'patients' && (
                    <div style={styles.card}>
                        <h2>Patientlista</h2>
                        {!isDoctor && (
                            <p style={{ color: '#666', fontSize: '14px', marginBottom: '20px' }}>
                                Som personal kan du registrera klinisk data för patienter
                            </p>
                        )}
                        {loading ? (
                            <p>Laddar patienter...</p>
                        ) : (
                            <table style={styles.table}>
                                <thead>
                                <tr>
                                    <th style={styles.th}>Förnamn</th>
                                    <th style={styles.th}>Efternamn</th>
                                    <th style={styles.th}>Personnummer</th>
                                    <th style={styles.th}>Åtgärder</th>
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
                                                {isDoctor ? 'Visa detaljer' : 'Registrera data'}
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
                    <PatientDetails
                        patient={selectedPatient}
                        practitionerPersonnummer={user.foreignId}
                        isDoctor={isDoctor}
                        onBack={() => setActiveTab('patients')}
                    />
                )}

                {activeTab === 'messages' && (
                    <div style={styles.card}>
                        <h2>Meddelanden</h2>
                        <MessagingSystem currentUser={user} patientPersonnummer={null} />
                    </div>
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

    useEffect(() => {
        fetchPatientData();
    }, [patient.socialSecurityNumber]);

    const fetchPatientData = async () => {
        try {
            const patientPersonnummer = patient.socialSecurityNumber;

            const [obsRes, condRes, encRes] = await Promise.all([
                fetch(`http://localhost:8080/api/v1/clinical/observations/patient/${patientPersonnummer}`),
                fetch(`http://localhost:8080/api/v1/clinical/conditions/patient/${patientPersonnummer}`),
                fetch(`http://localhost:8080/api/v1/clinical/encounters/patient/${patientPersonnummer}`)
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
            console.error('Fel vid hämtning av patientdata:', error);
        }
    };

    const handleAddObservation = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch('http://localhost:8080/api/v1/clinical/observations', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    patientPersonnummer: patient.socialSecurityNumber,
                    performerPersonnummer: null,
                    description: newObservation.description,
                    value: newObservation.value,
                    unit: newObservation.unit,
                    effectiveDate: newObservation.effectiveDate
                })
            });

            if (response.ok) {
                alert('Observation skapad i HAPI FHIR!');
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
                console.error('Fel från server:', errorText);
                alert('Fel vid skapande av observation: ' + errorText);
            }
        } catch (error) {
            console.error('Fel vid skapande av observation:', error);
            alert('Kunde inte skapa observation: ' + error.message);
        }
    };

    const handleAddCondition = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch('http://localhost:8080/api/v1/clinical/conditions', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    patientPersonnummer: patient.socialSecurityNumber,
                    practitionerPersonnummer: null,
                    description: newCondition.description,
                    assertedDate: newCondition.assertedDate
                })
            });

            if (response.ok) {
                alert('Diagnos skapad i HAPI FHIR!');
                setShowAddCondition(false);
                setNewCondition({
                    description: '',
                    assertedDate: new Date().toISOString().slice(0, 10)
                });
                fetchPatientData();
            } else {
                const errorText = await response.text();
                console.error('Fel från server:', errorText);
                alert('Fel vid skapande av diagnos: ' + errorText);
            }
        } catch (error) {
            console.error('Fel vid skapande av diagnos:', error);
            alert('Kunde inte skapa diagnos: ' + error.message);
        }
    };

    const handleAddEncounter = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch('http://localhost:8080/api/v1/clinical/encounters', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    patientPersonnummer: patient.socialSecurityNumber,
                    practitionerPersonnummer: null,
                    startTime: newEncounter.startTime,
                    endTime: newEncounter.endTime || null
                })
            });

            if (response.ok) {
                alert('Besök skapat i HAPI FHIR!');
                setShowAddEncounter(false);
                setNewEncounter({
                    startTime: new Date().toISOString().slice(0, 16),
                    endTime: ''
                });
                fetchPatientData();
            } else {
                const errorText = await response.text();
                console.error('Fel från server:', errorText);
                alert('Fel vid skapande av besök: ' + errorText);
            }
        } catch (error) {
            console.error('Fel vid skapande av besök:', error);
            alert('Kunde inte skapa besök: ' + error.message);
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
            <button style={styles.button} onClick={onBack}>← Tillbaka</button>

            <div style={styles.card}>
                <h2>Patientinformation {isDoctor ? '(Fullständig åtkomst som läkare)' : '(Personal - begränsad åtkomst)'}</h2>
                <p><strong>Namn:</strong> {patient.firstName} {patient.lastName}</p>
                <p><strong>Personnummer:</strong> {patient.socialSecurityNumber}</p>
                <p><strong>Födelsedatum:</strong> {patient.dateOfBirth}</p>
            </div>

            {/* OBSERVATIONER */}
            <div style={styles.card}>
                <h3>Observationer</h3>
                <button style={styles.button} onClick={() => setShowAddObservation(!showAddObservation)}>
                    {showAddObservation ? 'Avbryt' : '+ Lägg till observation'}
                </button>

                {showAddObservation && (
                    <form onSubmit={handleAddObservation} style={{ marginTop: '20px', padding: '20px', background: '#f9f9f9', borderRadius: '8px' }}>
                        <h4>Ny observation</h4>
                        <label>
                            Beskrivning:
                            <textarea
                                style={styles.textarea}
                                value={newObservation.description}
                                onChange={(e) => setNewObservation({...newObservation, description: e.target.value})}
                                placeholder="Beskriv observationen"
                                required
                            />
                        </label>
                        <label>
                            Värde (valfritt):
                            <input
                                type="text"
                                style={styles.input}
                                value={newObservation.value}
                                onChange={(e) => setNewObservation({...newObservation, value: e.target.value})}
                                placeholder="T.ex. 120"
                            />
                        </label>
                        <label>
                            Enhet (valfritt):
                            <input
                                type="text"
                                style={styles.input}
                                value={newObservation.unit}
                                onChange={(e) => setNewObservation({...newObservation, unit: e.target.value})}
                                placeholder="T.ex. mmHg"
                            />
                        </label>
                        <label>
                            Datum:
                            <input
                                type="date"
                                style={styles.input}
                                value={newObservation.effectiveDate}
                                onChange={(e) => setNewObservation({...newObservation, effectiveDate: e.target.value})}
                                required
                            />
                        </label>
                        <button type="submit" style={styles.button}>Spara observation</button>
                    </form>
                )}

                {/* VIKTIGT: LÄKARE ser listan, PERSONAL ser den INTE */}
                {isDoctor && (
                    <>
                        {observations.length === 0 ? (
                            <p>Inga observationer registrerade</p>
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

            {/* DIAGNOSER */}
            <div style={styles.card}>
                <h3>Diagnoser</h3>
                <button style={styles.button} onClick={() => setShowAddCondition(!showAddCondition)}>
                    {showAddCondition ? 'Avbryt' : '+ Lägg till diagnos'}
                </button>

                {showAddCondition && (
                    <form onSubmit={handleAddCondition} style={{ marginTop: '20px', padding: '20px', background: '#f9f9f9', borderRadius: '8px' }}>
                        <h4>Ny diagnos</h4>
                        <label>
                            Beskrivning:
                            <textarea
                                style={styles.textarea}
                                value={newCondition.description}
                                onChange={(e) => setNewCondition({...newCondition, description: e.target.value})}
                                placeholder="Beskriv diagnosen"
                                required
                            />
                        </label>
                        <label>
                            Datum:
                            <input
                                type="date"
                                style={styles.input}
                                value={newCondition.assertedDate}
                                onChange={(e) => setNewCondition({...newCondition, assertedDate: e.target.value})}
                                required
                            />
                        </label>
                        <button type="submit" style={styles.button}>Spara diagnos</button>
                    </form>
                )}

                {/* VIKTIGT: LÄKARE ser listan, PERSONAL ser den INTE */}
                {isDoctor && (
                    <>
                        {conditions.length === 0 ? (
                            <p>Inga diagnoser registrerade</p>
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

            {/* BESÖK - Både läkare och personal kan se och skapa */}
            <div style={styles.card}>
                <h3>Besök</h3>
                <button style={styles.button} onClick={() => setShowAddEncounter(!showAddEncounter)}>
                    {showAddEncounter ? 'Avbryt' : '+ Registrera besök'}
                </button>

                {showAddEncounter && (
                    <form onSubmit={handleAddEncounter} style={{ marginTop: '20px', padding: '20px', background: '#f9f9f9', borderRadius: '8px' }}>
                        <h4>Nytt besök</h4>
                        <label>
                            Starttid:
                            <input
                                type="datetime-local"
                                style={styles.input}
                                value={newEncounter.startTime}
                                onChange={(e) => setNewEncounter({...newEncounter, startTime: e.target.value})}
                                required
                            />
                        </label>
                        <label>
                            Sluttid (valfritt):
                            <input
                                type="datetime-local"
                                style={styles.input}
                                value={newEncounter.endTime}
                                onChange={(e) => setNewEncounter({...newEncounter, endTime: e.target.value})}
                            />
                        </label>
                        <button type="submit" style={styles.button}>Spara besök</button>
                    </form>
                )}

                {/* Både läkare och personal ser besök-listan */}
                {encounters.length === 0 ? (
                    <p>Inga besök registrerade</p>
                ) : (
                    <ul>
                        {encounters.map(enc => (
                            <li key={enc.id} style={{ padding: '10px 0', borderBottom: '1px solid #eee' }}>
                                Besök: {new Date(enc.startTime).toLocaleString()}
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