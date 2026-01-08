import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { fetchWithAuth, endpoints } from '../config/api';
import './ProfileSetup.css';

const ProfileSetup = () => {
    const { user, completeProfileSetup } = useAuth();
    const [step, setStep] = useState(1);
    const [selectedRole, setSelectedRole] = useState('');
    const [persons, setPersons] = useState([]);
    const [selectedPerson, setSelectedPerson] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [searchTerm, setSearchTerm] = useState('');

    // Fetch persons based on selected role
    useEffect(() => {
        const fetchPersons = async () => {
            if (!selectedRole) return;

            setIsLoading(true);
            setError('');

            try {
                let response;
                if (selectedRole === 'patient') {
                    response = await fetchWithAuth(endpoints.patients());
                } else {
                    // Doctor and staff are practitioners
                    response = await fetchWithAuth(endpoints.practitioners());
                }

                if (response.ok) {
                    const data = await response.json();
                    console.log('Fetched persons:', data);
                    setPersons(data);
                } else {
                    setError('Kunde inte hämta personer från databasen');
                }
            } catch (err) {
                console.error('Error fetching persons:', err);
                setError('Ett fel uppstod vid hämtning av personer');
            } finally {
                setIsLoading(false);
            }
        };

        if (step === 2) {
            fetchPersons();
        }
    }, [selectedRole, step]);

    const handleRoleSelect = (role) => {
        setSelectedRole(role);
        setSelectedPerson(null);
        setStep(2);
    };

    const handlePersonSelect = (person) => {
        console.log('Selected person:', person);
        setSelectedPerson(person);
    };

    const handleComplete = async () => {
        if (!selectedRole || !selectedPerson) {
            setError('Välj både roll och person');
            return;
        }

        setIsLoading(true);
        setError('');

        // Use socialSecurityNumber as foreignId (same as old Register.js)
        const foreignId = selectedPerson.socialSecurityNumber;
        console.log('Saving profile with foreignId:', foreignId);

        try {
            // Save profile to user-service
            const response = await fetchWithAuth(endpoints.setupProfile(), {
                method: 'POST',
                body: JSON.stringify({
                    keycloakId: user.keycloakId || user.id,
                    email: user.email,
                    username: user.username,
                    role: selectedRole.toUpperCase(), // PATIENT, DOCTOR, STAFF
                    foreignId: foreignId,
                    firstName: user.firstName || selectedPerson.firstName,
                    lastName: user.lastName || selectedPerson.lastName,
                }),
            });

            if (response.ok) {
                const savedUser = await response.json();
                console.log('Profile saved:', savedUser);
                // Update auth context - this will trigger re-render and show dashboard
                completeProfileSetup(selectedRole.toUpperCase(), foreignId);
            } else {
                const errorData = await response.text();
                if (errorData.includes('already registered')) {
                    setError('Denna person har redan ett konto. Välj en annan person.');
                } else {
                    setError(errorData || 'Kunde inte spara profilen');
                }
            }
        } catch (err) {
            console.error('Error saving profile:', err);
            setError('Ett fel uppstod vid sparande av profilen');
        } finally {
            setIsLoading(false);
        }
    };

    const handleBack = () => {
        setStep(1);
        setSelectedPerson(null);
        setPersons([]);
    };

    // Filter persons based on search term
    const filteredPersons = persons.filter(person => {
        const fullName = `${person.firstName || ''} ${person.lastName || ''}`.toLowerCase();
        const ssn = person.socialSecurityNumber || '';
        return fullName.includes(searchTerm.toLowerCase()) ||
            ssn.includes(searchTerm);
    });

    return (
        <div className="profile-setup-container">
            <div className="profile-setup-card">
                <h1>Välkommen, {user?.firstName || user?.username}!</h1>
                <p className="subtitle">Slutför din profil för att fortsätta</p>

                {error && <div className="error-message">{error}</div>}

                {step === 1 && (
                    <div className="step-content">
                        <h2>Steg 1: Välj din roll</h2>
                        <p>Vilken roll har du i systemet?</p>

                        <div className="role-options">
                            <button
                                className={`role-button ${selectedRole === 'doctor' ? 'selected' : ''}`}
                                onClick={() => handleRoleSelect('doctor')}
                            >
                                <span className="role-icon">👨‍⚕️</span>
                                <span className="role-title">Läkare</span>
                                <span className="role-description">Hantera patienter och journaler</span>
                            </button>

                            <button
                                className={`role-button ${selectedRole === 'staff' ? 'selected' : ''}`}
                                onClick={() => handleRoleSelect('staff')}
                            >
                                <span className="role-icon">👩‍💼</span>
                                <span className="role-title">Personal</span>
                                <span className="role-description">Administrativ åtkomst</span>
                            </button>

                            <button
                                className={`role-button ${selectedRole === 'patient' ? 'selected' : ''}`}
                                onClick={() => handleRoleSelect('patient')}
                            >
                                <span className="role-icon">🧑</span>
                                <span className="role-title">Patient</span>
                                <span className="role-description">Se mina journaler</span>
                            </button>
                        </div>
                    </div>
                )}

                {step === 2 && (
                    <div className="step-content">
                        <h2>Steg 2: Koppla till din profil</h2>
                        <p>
                            {selectedRole === 'patient'
                                ? 'Välj din patientprofil från listan'
                                : 'Välj din personalprofil från listan'}
                        </p>

                        <div className="search-box">
                            <input
                                type="text"
                                placeholder="Sök på namn eller personnummer..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>

                        {isLoading ? (
                            <div className="loading">Laddar...</div>
                        ) : (
                            <div className="persons-list">
                                {filteredPersons.length === 0 ? (
                                    <p className="no-results">Inga personer hittades</p>
                                ) : (
                                    filteredPersons.map((person) => (
                                        <div
                                            key={person.socialSecurityNumber}
                                            className={`person-card ${selectedPerson?.socialSecurityNumber === person.socialSecurityNumber ? 'selected' : ''}`}
                                            onClick={() => handlePersonSelect(person)}
                                        >
                                            <div className="person-info">
                        <span className="person-name">
                          {person.firstName} {person.lastName}
                        </span>
                                                <span className="person-id">
                          {person.socialSecurityNumber
                              ? `${person.socialSecurityNumber.substring(0, 8)}...`
                              : 'Inget personnummer'}
                        </span>
                                            </div>
                                            {selectedPerson?.socialSecurityNumber === person.socialSecurityNumber && (
                                                <span className="checkmark">✓</span>
                                            )}
                                        </div>
                                    ))
                                )}
                            </div>
                        )}

                        <div className="button-group">
                            <button className="back-button" onClick={handleBack}>
                                ← Tillbaka
                            </button>
                            <button
                                className="complete-button"
                                onClick={handleComplete}
                                disabled={!selectedPerson || isLoading}
                            >
                                {isLoading ? 'Sparar...' : 'Slutför registrering'}
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ProfileSetup;