import React, { useState, useEffect } from 'react';

function App() {
    // 1. Skapa ett tillstånd (state) för att lagra meddelandet från backend
    const [backendMessage, setBackendMessage] = useState('Laddar...');

    useEffect(() => {
        // 2. Anropa backend-slutpunkten när komponenten laddas
        fetch('http://localhost:8080/api/hello')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.text(); // Vi förväntar oss en sträng från backend
            })
            .then(data => {
                // 3. Uppdatera tillståndet med meddelandet
                setBackendMessage(data);
            })
            .catch(error => {
                console.error("Fel vid hämtning av data från backend:", error);
                setBackendMessage('Kunde inte ansluta till backend. Kontrollera att backend-servern körs på port 8080 och att CORS är konfigurerat.');
            });
    }, []); // [] säkerställer att effekten bara körs en gång (vid mount)

    return (
        <div className="App">
            <h1>Min PatientSystem Frontend</h1>
            <p>Meddelande från backend:</p>
            <h2>{backendMessage}</h2>
        </div>
    );
}

export default App;