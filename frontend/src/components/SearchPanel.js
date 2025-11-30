import React, { useState } from 'react';
import API_CONFIG from '../config/api';

function SearchPanel({ currentUser }) {
    const [searchType, setSearchType] = useState('name');
    const [searchQuery, setSearchQuery] = useState('');
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedPatient, setSelectedPatient] = useState(null);
    const [selectedDate, setSelectedDate] = useState('');

    const isDoctor = currentUser.role === 'DOCTOR';
    const isStaff = currentUser.role === 'STAFF';
    const isPatient = currentUser.role === 'PATIENT';

    const handleSearch = async () => {
        if (!searchQuery.trim() && searchType !== 'my-encounters' && searchType !== 'my-patients') {
            alert('Please enter a search term');
            return;
        }

        setLoading(true);
        setResults([]);

        try {
            let url = '';

            switch (searchType) {
                case 'name':
                    url = `${API_CONFIG.SEARCH_SERVICE}/api/search/patients?name=${encodeURIComponent(searchQuery)}`;
                    break;
                case 'condition':
                    url = `${API_CONFIG.SEARCH_SERVICE}/api/search/patients?condition=${encodeURIComponent(searchQuery)}`;
                    break;
                case 'my-patients':
                    url = `${API_CONFIG.SEARCH_SERVICE}/api/search/doctors/${currentUser.foreignId}/patients`;
                    break;
                case 'my-encounters':
                    if (!selectedDate) {
                        alert('Please select a date');
                        setLoading(false);
                        return;
                    }
                    url = `${API_CONFIG.SEARCH_SERVICE}/api/search/doctors/${currentUser.foreignId}/encounters?date=${selectedDate}`;
                    break;
                default:
                    break;
            }

            console.log('Fetching from URL:', url);
            const response = await fetch(url);

            if (response.ok) {
                const data = await response.json();
                console.log('Search results:', data);

                // Handle different response structures
                if (searchType === 'my-patients' && data.patients) {
                    // DoctorPatientsResult has a 'patients' array
                    setResults(data.patients);
                } else if (Array.isArray(data)) {
                    // Direct array response for name/condition/encounters
                    setResults(data);
                } else {
                    // Single result or other structure
                    setResults([data]);
                }
            } else {
                const errorText = await response.text();
                console.error('Search failed:', response.status, errorText);
                alert('Search failed: ' + errorText);
            }
        } catch (error) {
            console.error('Search error:', error);
            alert('Error performing search: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handlePatientClick = async (patient) => {
        setSelectedPatient(null);
        setLoading(true);

        try {
            // Use socialSecurityNumber as the patient identifier
            const patientId = patient.socialSecurityNumber || patient.id;
            console.log('Fetching details for patient:', patientId);

            const [obsRes, condRes, encRes] = await Promise.all([
                fetch(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/observations/patient/${patientId}`),
                fetch(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/conditions/patient/${patientId}`),
                fetch(`${API_CONFIG.CLINICAL_SERVICE}/api/v1/clinical/encounters/patient/${patientId}`)
            ]);

            const observations = obsRes.ok ? await obsRes.json() : [];
            const conditions = condRes.ok ? await condRes.json() : [];
            const encounters = encRes.ok ? await encRes.json() : [];

            console.log('Patient details loaded:', { observations: observations.length, conditions: conditions.length, encounters: encounters.length });

            setSelectedPatient({
                ...patient,
                observations,
                conditions,
                encounters
            });
        } catch (error) {
            console.error('Error fetching patient details:', error);
            alert('Could not fetch patient details: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const styles = {
        container: {
            background: 'white',
            borderRadius: '8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            padding: '20px'
        },
        header: {
            marginBottom: '20px',
            paddingBottom: '15px',
            borderBottom: '2px solid #e0e0e0'
        },
        searchSection: {
            padding: '20px',
            background: '#f9f9f9',
            borderRadius: '8px',
            marginBottom: '30px'
        },
        searchType: {
            display: 'flex',
            gap: '10px',
            marginBottom: '15px',
            flexWrap: 'wrap'
        },
        typeButton: (active) => ({
            padding: '10px 20px',
            background: active ? '#667eea' : '#ddd',
            color: active ? 'white' : '#333',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: active ? '600' : '400'
        }),
        searchInput: {
            width: '100%',
            padding: '12px',
            border: '1px solid #ddd',
            borderRadius: '6px',
            fontSize: '16px',
            marginBottom: '15px',
            boxSizing: 'border-box'
        },
        button: {
            padding: '12px 24px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '16px',
            fontWeight: '600'
        },
        resultsTable: {
            width: '100%',
            borderCollapse: 'collapse',
            marginTop: '20px'
        },
        th: {
            background: '#667eea',
            color: 'white',
            padding: '12px',
            textAlign: 'left',
            fontWeight: '600'
        },
        td: {
            padding: '12px',
            borderBottom: '1px solid #ddd'
        },
        row: {
            cursor: 'pointer',
            transition: 'background 0.2s'
        },
        modal: {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000
        },
        modalContent: {
            background: 'white',
            borderRadius: '12px',
            padding: '30px',
            maxWidth: '800px',
            maxHeight: '80vh',
            overflow: 'auto',
            width: '90%'
        },
        section: {
            marginBottom: '20px',
            padding: '15px',
            background: '#f9f9f9',
            borderRadius: '8px'
        }
    };

    // Patients cannot access search
    if (isPatient) {
        return (
            <div style={styles.container}>
                <h2>Access Denied</h2>
                <p>Search functionality is only available to doctors and staff.</p>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h2>Patient Search</h2>
                <p style={{ color: '#666', fontSize: '14px', marginTop: '5px' }}>
                    Search for patients by name, condition, or view your patients and encounters
                </p>
            </div>

            <div style={styles.searchSection}>
                {/* Search Type Selection */}
                <div style={styles.searchType}>
                    <button
                        style={styles.typeButton(searchType === 'name')}
                        onClick={() => setSearchType('name')}
                    >
                        🔍 Search by Name
                    </button>
                    <button
                        style={styles.typeButton(searchType === 'condition')}
                        onClick={() => setSearchType('condition')}
                    >
                        🏥 Search by Condition
                    </button>
                    {isDoctor && (
                        <>
                            <button
                                style={styles.typeButton(searchType === 'my-patients')}
                                onClick={() => setSearchType('my-patients')}
                            >
                                👥 My Patients
                            </button>
                            <button
                                style={styles.typeButton(searchType === 'my-encounters')}
                                onClick={() => setSearchType('my-encounters')}
                            >
                                📅 My Encounters
                            </button>
                        </>
                    )}
                </div>

                {/* Search Input */}
                {(searchType === 'name' || searchType === 'condition') && (
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder={
                            searchType === 'name'
                                ? 'Enter patient name...'
                                : 'Enter condition/diagnosis...'
                        }
                        style={styles.searchInput}
                        onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    />
                )}

                {searchType === 'my-encounters' && (
                    <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        style={styles.searchInput}
                    />
                )}

                <button
                    style={styles.button}
                    onClick={handleSearch}
                    disabled={loading}
                >
                    {loading ? 'Searching...' : 'Search'}
                </button>
            </div>

            {/* Results */}
            {loading && (
                <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                    Loading results...
                </div>
            )}

            {!loading && results.length > 0 && (
                <div>
                    <h3 style={{ marginBottom: '15px' }}>
                        Results ({results.length})
                    </h3>

                    {searchType === 'my-encounters' ? (
                        // Encounters table
                        <table style={styles.resultsTable}>
                            <thead>
                            <tr>
                                <th style={styles.th}>Patient</th>
                                <th style={styles.th}>Start Time</th>
                                <th style={styles.th}>End Time</th>
                                <th style={styles.th}>Duration</th>
                            </tr>
                            </thead>
                            <tbody>
                            {results.map((encounter, index) => (
                                <tr key={index} style={styles.row}>
                                    <td style={styles.td}>{encounter.patientName || 'Unknown'}</td>
                                    <td style={styles.td}>
                                        {encounter.startTime
                                            ? new Date(encounter.startTime).toLocaleString()
                                            : 'N/A'}
                                    </td>
                                    <td style={styles.td}>
                                        {encounter.endTime
                                            ? new Date(encounter.endTime).toLocaleString()
                                            : 'Ongoing'}
                                    </td>
                                    <td style={styles.td}>
                                        {encounter.startTime && encounter.endTime
                                            ? `${Math.round((new Date(encounter.endTime) - new Date(encounter.startTime)) / 60000)} min`
                                            : 'N/A'}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    ) : (
                        // Patients table
                        <table style={styles.resultsTable}>
                            <thead>
                            <tr>
                                <th style={styles.th}>Name</th>
                                <th style={styles.th}>Social Security Number</th>
                                <th style={styles.th}>Date of Birth</th>
                                <th style={styles.th}>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {results.map((patient, index) => (
                                <tr
                                    key={patient.id || index}
                                    style={styles.row}
                                    onMouseEnter={(e) => e.currentTarget.style.background = '#f5f5f5'}
                                    onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                                >
                                    <td style={styles.td}>
                                        {patient.firstName} {patient.lastName}
                                    </td>
                                    <td style={styles.td}>{patient.socialSecurityNumber || patient.id}</td>
                                    <td style={styles.td}>{patient.dateOfBirth || 'N/A'}</td>
                                    <td style={styles.td}>
                                        <button
                                            style={{ ...styles.button, padding: '6px 12px', fontSize: '14px' }}
                                            onClick={() => handlePatientClick(patient)}
                                        >
                                            View Details
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {!loading && results.length === 0 && (searchQuery || searchType === 'my-patients' || (searchType === 'my-encounters' && selectedDate)) && (
                <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                    No results found
                </div>
            )}

            {/* Patient Details Modal */}
            {selectedPatient && (
                <div style={styles.modal} onClick={() => setSelectedPatient(null)}>
                    <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
                        <h2 style={{ marginBottom: '20px' }}>
                            {selectedPatient.firstName} {selectedPatient.lastName}
                        </h2>

                        <div style={styles.section}>
                            <h3>Personal Information</h3>
                            <p><strong>SSN:</strong> {selectedPatient.socialSecurityNumber || selectedPatient.id}</p>
                            <p><strong>Date of Birth:</strong> {selectedPatient.dateOfBirth || 'N/A'}</p>
                        </div>

                        <div style={styles.section}>
                            <h3>Observations ({selectedPatient.observations?.length || 0})</h3>
                            {selectedPatient.observations?.length > 0 ? (
                                <ul>
                                    {selectedPatient.observations.slice(0, 5).map((obs, i) => (
                                        <li key={i}>{obs.description}</li>
                                    ))}
                                    {selectedPatient.observations.length > 5 && (
                                        <li style={{ color: '#666', fontStyle: 'italic' }}>
                                            ... and {selectedPatient.observations.length - 5} more
                                        </li>
                                    )}
                                </ul>
                            ) : (
                                <p>No observations recorded</p>
                            )}
                        </div>

                        <div style={styles.section}>
                            <h3>Conditions ({selectedPatient.conditions?.length || 0})</h3>
                            {selectedPatient.conditions?.length > 0 ? (
                                <ul>
                                    {selectedPatient.conditions.slice(0, 5).map((cond, i) => (
                                        <li key={i}>{cond.description}</li>
                                    ))}
                                    {selectedPatient.conditions.length > 5 && (
                                        <li style={{ color: '#666', fontStyle: 'italic' }}>
                                            ... and {selectedPatient.conditions.length - 5} more
                                        </li>
                                    )}
                                </ul>
                            ) : (
                                <p>No conditions recorded</p>
                            )}
                        </div>

                        <div style={styles.section}>
                            <h3>Encounters ({selectedPatient.encounters?.length || 0})</h3>
                            {selectedPatient.encounters?.length > 0 ? (
                                <p>{selectedPatient.encounters.length} encounter(s) on record</p>
                            ) : (
                                <p>No encounters recorded</p>
                            )}
                        </div>

                        <div style={{ textAlign: 'center', marginTop: '20px' }}>
                            <button
                                style={styles.button}
                                onClick={() => setSelectedPatient(null)}
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default SearchPanel;