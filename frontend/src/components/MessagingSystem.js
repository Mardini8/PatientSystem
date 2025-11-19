import React, { useState, useEffect } from 'react';

const API_URL = 'http://localhost:8080/api';

function MessagingSystem({ currentUser, patientPersonnummer }) {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [showNewMessageForm, setShowNewMessageForm] = useState(false);
    const [recipients, setRecipients] = useState([]);
    const [selectedRecipient, setSelectedRecipient] = useState(null);
    const [selectedPatient, setSelectedPatient] = useState(null);
    const [replyTo, setReplyTo] = useState(null);
    const [loading, setLoading] = useState(false);
    const [patientNames, setPatientNames] = useState({});
    const [practitionerNames, setPractitionerNames] = useState({});
    const [patients, setPatients] = useState([]);
    const [userIdToForeignId, setUserIdToForeignId] = useState({});

    useEffect(() => {
        fetchMessages();
        if (currentUser.role === 'PATIENT') {
            fetchRecipients();
        } else {
            fetchAllPatientNames();
            fetchAllPatients();
        }
        fetchAllPractitionerNames();
        fetchUserMappings();
    }, [currentUser, patientPersonnummer]);

    const fetchUserMappings = async () => {
        try {
            const practResponse = await fetch(`${API_URL}/practitioners`);
            if (practResponse.ok) {
                const practitioners = await practResponse.json();

                const mappings = {};
                for (const pract of practitioners) {
                    try {
                        const userResponse = await fetch(`${API_URL}/v1/auth/user-by-foreign/${pract.socialSecurityNumber}`);
                        if (userResponse.ok) {
                            const user = await userResponse.json();
                            mappings[user.id] = pract.socialSecurityNumber;
                            console.log(`Mapped User ID ${user.id} -> Practitioner UUID ${pract.socialSecurityNumber} (${pract.firstName} ${pract.lastName})`);
                        }
                    } catch (e) {
                    }
                }
                console.log('User ID -> Foreign ID mappings:', mappings);
                setUserIdToForeignId(mappings);
            }
        } catch (error) {
            console.error('Fel vid hämtning av user mappings:', error);
        }
    };

    const fetchAllPatients = async () => {
        try {
            const response = await fetch(`${API_URL}/patients`);
            if (response.ok) {
                const data = await response.json();
                setPatients(data);
            }
        } catch (error) {
            console.error('Fel vid hämtning av patienter:', error);
        }
    };

    const fetchAllPatientNames = async () => {
        try {
            const response = await fetch(`${API_URL}/patients`);
            if (response.ok) {
                const patients = await response.json();
                const names = {};
                patients.forEach(p => {
                    names[p.socialSecurityNumber] = `${p.firstName} ${p.lastName}`;
                });
                setPatientNames(names);
            }
        } catch (error) {
            console.error('Fel vid hämtning av patientnamn:', error);
        }
    };

    const fetchAllPractitionerNames = async () => {
        try {
            const response = await fetch(`${API_URL}/practitioners`);
            if (response.ok) {
                const practitioners = await response.json();
                const names = {};
                practitioners.forEach(p => {
                    names[p.socialSecurityNumber] = `${p.firstName} ${p.lastName}`;
                });
                setPractitionerNames(names);
            }
        } catch (error) {
            console.error('Fel vid hämtning av practitioner-namn:', error);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${day}/${month}/${year} ${hours}:${minutes}`;
    };

    const fetchMessages = async () => {
        setLoading(true);
        try {
            let allMessages = [];
            if (currentUser.role === 'PATIENT') {
                const url = `${API_URL}/v1/messages/patient/${patientPersonnummer}`;
                const response = await fetch(url);
                if (response.ok) {
                    allMessages = await response.json();
                }
            } else {
                const [toMeRes, fromMeRes] = await Promise.all([
                    fetch(`${API_URL}/v1/messages/to-user/${currentUser.id}`),
                    fetch(`${API_URL}/v1/messages/from-user/${currentUser.id}`)
                ]);

                const toMe = toMeRes.ok ? await toMeRes.json() : [];
                const fromMe = fromMeRes.ok ? await fromMeRes.json() : [];
                allMessages = [...toMe, ...fromMe];
            }

            allMessages.sort((a, b) => new Date(b.sentAt) - new Date(a.sentAt));
            setMessages(allMessages);
        } catch (error) {
            console.error('Fel vid hämtning av meddelanden:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchRecipients = async () => {
        try {
            const response = await fetch(`${API_URL}/practitioners`);
            if (response.ok) {
                const data = await response.json();
                setRecipients(data);
            }
        } catch (error) {
            console.error('Fel vid hämtning av mottagare:', error);
        }
    };

    const handleReply = (message) => {
        setReplyTo(message);
        setShowNewMessageForm(true);

        if (currentUser.role === 'PATIENT') {
            setSelectedRecipient(message.fromUserId);
        }

        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const sendMessage = async () => {
        if (!newMessage.trim()) {
            alert('Meddelandet får inte vara tomt');
            return;
        }

        let toUserId;
        let messagePatientPersonnummer;

        if (currentUser.role === 'PATIENT') {
            if (replyTo) {
                toUserId = replyTo.fromUserId;
                messagePatientPersonnummer = patientPersonnummer;
            } else if (selectedRecipient) {
                const userResponse = await fetch(`${API_URL}/v1/auth/user-by-foreign/${selectedRecipient}`);
                if (!userResponse.ok) {
                    alert('Kunde inte hitta mottagaren');
                    return;
                }
                const recipientUser = await userResponse.json();
                toUserId = recipientUser.id;
                messagePatientPersonnummer = patientPersonnummer;
            } else {
                alert('Välj en mottagare');
                return;
            }
        } else {
            if (replyTo) {
                toUserId = replyTo.fromUserId;
                messagePatientPersonnummer = replyTo.patientPersonnummer;
            } else {
                if (!selectedPatient) {
                    alert('Välj en patient');
                    return;
                }
                const userResponse = await fetch(`${API_URL}/v1/auth/user-by-foreign/${selectedPatient}`);
                if (!userResponse.ok) {
                    alert('Kunde inte hitta patient-användaren');
                    return;
                }
                const patientUser = await userResponse.json();
                toUserId = patientUser.id;
                messagePatientPersonnummer = selectedPatient;
            }
        }

        try {
            const response = await fetch(`${API_URL}/v1/messages`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fromUserId: currentUser.id,
                    toUserId: toUserId,
                    patientPersonnummer: messagePatientPersonnummer,
                    content: newMessage,
                    sentAt: new Date().toISOString()
                })
            });

            if (response.ok) {
                setNewMessage('');
                setShowNewMessageForm(false);
                setSelectedRecipient(null);
                setSelectedPatient(null);
                setReplyTo(null);
                fetchMessages();
                alert('Meddelande skickat!');
            } else {
                const errorText = await response.text();
                alert('Kunde inte skicka meddelandet: ' + errorText);
            }
        } catch (error) {
            console.error('Fel vid skickande av meddelande:', error);
            alert('Kunde inte skicka meddelandet');
        }
    };

    const styles = {
        container: {
            background: 'white',
            borderRadius: '8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        },
        header: {
            padding: '20px',
            borderBottom: '2px solid #e0e0e0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
        },
        newMessageButton: {
            padding: '10px 20px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: '600'
        },
        messageList: {
            maxHeight: '600px',
            overflowY: 'auto'
        },
        messageItem: (isSent) => ({
            padding: '15px 20px',
            borderBottom: '1px solid #f0f0f0',
            background: isSent ? '#f0f7ff' : 'white',
            transition: 'background 0.2s',
            cursor: 'pointer',
            ':hover': {
                background: isSent ? '#e6f2ff' : '#fafafa'
            }
        }),
        messageHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '8px'
        },
        messageFrom: (isSent) => ({
            fontWeight: '600',
            fontSize: '14px',
            color: isSent ? '#667eea' : '#333'
        }),
        messageDate: {
            fontSize: '12px',
            color: '#999'
        },
        messageContent: {
            fontSize: '14px',
            color: '#333',
            lineHeight: '1.5',
            marginBottom: '10px'
        },
        messagePatient: {
            fontSize: '12px',
            color: '#666',
            fontStyle: 'italic',
            marginBottom: '8px'
        },
        replyButton: {
            padding: '6px 12px',
            background: '#4caf50',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
            fontWeight: '500'
        },
        label: (isSent) => ({
            display: 'inline-block',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '11px',
            fontWeight: '600',
            background: isSent ? '#667eea' : '#4caf50',
            color: 'white',
            marginRight: '10px'
        }),
        newMessageForm: {
            padding: '20px',
            background: '#f9f9f9',
            borderBottom: '2px solid #e0e0e0'
        },
        formTitle: {
            fontSize: '18px',
            fontWeight: '600',
            marginBottom: '15px',
            color: '#333'
        },
        select: {
            width: '100%',
            padding: '10px',
            border: '1px solid #ddd',
            borderRadius: '6px',
            fontSize: '14px',
            marginBottom: '15px',
            boxSizing: 'border-box'
        },
        textarea: {
            width: '100%',
            padding: '12px',
            border: '1px solid #ddd',
            borderRadius: '6px',
            fontSize: '14px',
            minHeight: '120px',
            fontFamily: 'inherit',
            resize: 'vertical',
            boxSizing: 'border-box'
        },
        buttonGroup: {
            display: 'flex',
            gap: '10px',
            marginTop: '15px'
        },
        sendButton: {
            padding: '10px 20px',
            background: '#667eea',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: '600'
        },
        cancelButton: {
            padding: '10px 20px',
            background: '#999',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: '600'
        },
        emptyState: {
            padding: '60px 20px',
            textAlign: 'center',
            color: '#999',
            fontSize: '16px'
        }
    };

    if (loading) {
        return <div style={styles.emptyState}>Laddar meddelanden...</div>;
    }

    return (
        <div style={styles.container}>
            <div style={styles.header}>
                <h2 style={{ margin: 0 }}>Inkorg</h2>
                <button
                    style={styles.newMessageButton}
                    onClick={() => {
                        setShowNewMessageForm(!showNewMessageForm);
                        setReplyTo(null);
                    }}
                >
                    {showNewMessageForm ? '✕ Stäng' : 'Nytt meddelande'}
                </button>
            </div>

            {showNewMessageForm && (
                <div style={styles.newMessageForm}>
                    <div style={styles.formTitle}>
                        {replyTo ? '↩ Svara på meddelande' : 'Nytt meddelande'}
                    </div>

                    {replyTo && (
                        <div style={{
                            padding: '10px',
                            background: '#fff',
                            borderRadius: '6px',
                            marginBottom: '15px',
                            fontSize: '13px',
                            color: '#666',
                            borderLeft: '3px solid #667eea'
                        }}>
                            <strong>Svar till:</strong> {replyTo.content.substring(0, 100)}
                            {replyTo.content.length > 100 ? '...' : ''}
                        </div>
                    )}

                    {!replyTo && (
                        <>
                            {currentUser.role === 'PATIENT' ? (
                                <div>
                                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                                        Till:
                                    </label>
                                    <select
                                        style={styles.select}
                                        value={selectedRecipient || ''}
                                        onChange={(e) => setSelectedRecipient(e.target.value)}
                                    >
                                        <option value="">Välj mottagare...</option>
                                        {recipients.map(recipient => (
                                            <option key={recipient.socialSecurityNumber} value={recipient.socialSecurityNumber}>
                                                {recipient.firstName} {recipient.lastName} ({recipient.title})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            ) : (
                                <div>
                                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                                        Patient:
                                    </label>
                                    <select
                                        style={styles.select}
                                        value={selectedPatient || ''}
                                        onChange={(e) => setSelectedPatient(e.target.value)}
                                    >
                                        <option value="">Välj patient...</option>
                                        {patients.map(patient => (
                                            <option key={patient.socialSecurityNumber} value={patient.socialSecurityNumber}>
                                                {patient.firstName} {patient.lastName}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}
                        </>
                    )}

                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: '500' }}>
                        Meddelande:
                    </label>
                    <textarea
                        style={styles.textarea}
                        value={newMessage}
                        onChange={(e) => setNewMessage(e.target.value)}
                        placeholder="Skriv ditt meddelande här..."
                    />

                    <div style={styles.buttonGroup}>
                        <button style={styles.sendButton} onClick={sendMessage}>
                            Skicka
                        </button>
                        <button
                            style={styles.cancelButton}
                            onClick={() => {
                                setShowNewMessageForm(false);
                                setReplyTo(null);
                                setNewMessage('');
                            }}
                        >
                            Avbryt
                        </button>
                    </div>
                </div>
            )}

            <div style={styles.messageList}>
                {messages.length === 0 ? (
                    <div style={styles.emptyState}>
                        Inga meddelanden
                    </div>
                ) : (
                    messages.map(msg => {
                        const isSent = msg.fromUserId === currentUser.id;

                        let otherUserName;
                        if (isSent) {
                            if (currentUser.role === 'PATIENT') {
                                const practForeignId = userIdToForeignId[msg.toUserId];
                                otherUserName = practitionerNames[practForeignId] || 'Vårdpersonal';
                            } else {
                                otherUserName = patientNames[msg.patientPersonnummer] || 'Patient';
                            }
                        } else {
                            if (currentUser.role === 'PATIENT') {
                                const practForeignId = userIdToForeignId[msg.fromUserId];
                                otherUserName = practitionerNames[practForeignId] || 'Vårdpersonal';
                            } else {
                                otherUserName = patientNames[msg.patientPersonnummer] || 'Patient';
                            }
                        }

                        return (
                            <div
                                key={msg.id}
                                style={styles.messageItem(isSent)}
                            >
                                <div style={styles.messageHeader}>
                                    <div>
                                        <span style={styles.label(isSent)}>
                                            {isSent ? '➤ SKICKAT' : '⬅ MOTTAGET'}
                                        </span>
                                        <span style={styles.messageFrom(isSent)}>
                                            {isSent ? `Till: ${otherUserName}` : `Från: ${otherUserName}`}
                                        </span>
                                    </div>
                                    <span style={styles.messageDate}>
                                        {formatDate(msg.sentAt)}
                                    </span>
                                </div>

                                {currentUser.role !== 'PATIENT' && (
                                    <div style={styles.messagePatient}>
                                        Patient: {patientNames[msg.patientPersonnummer] || msg.patientPersonnummer}
                                    </div>
                                )}

                                <div style={styles.messageContent}>
                                    {msg.content}
                                </div>

                                {!isSent && (
                                    <button
                                        style={styles.replyButton}
                                        onClick={() => handleReply(msg)}
                                    >
                                        ↩ Svara
                                    </button>
                                )}
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
}

export default MessagingSystem;