#!/bin/bash

echo "=== Testing FHIR Encounter Search ==="
echo ""

FHIR="https://hapi-fhir.app.cloud.cbh.kth.se/fhir"
PRAC_ID="30681750-1667-311a-a3e3-878ae10a35bb"

echo "Practitioner ID: $PRAC_ID"
echo ""

echo "Test 1: Direct FHIR query (correct way - what Swagger uses)..."
curl -s "$FHIR/Encounter?practitioner=$PRAC_ID&_count=5" -o /tmp/test1.json
TOTAL1=$(python3 -c "import json; print(json.load(open('/tmp/test1.json')).get('total', 0))" 2>/dev/null || echo "ERROR")
echo "Result: $TOTAL1 encounters"
if [ "$TOTAL1" != "0" ] && [ "$TOTAL1" != "ERROR" ]; then
    echo "✓ This works!"
fi
echo ""

echo "Test 2: With Practitioner/ prefix (wrong way)..."
curl -s "$FHIR/Encounter?practitioner=Practitioner/$PRAC_ID&_count=5" -o /tmp/test2.json
TOTAL2=$(python3 -c "import json; print(json.load(open('/tmp/test2.json')).get('total', 0))" 2>/dev/null || echo "ERROR")
echo "Result: $TOTAL2 encounters"
if [ "$TOTAL2" = "0" ]; then
    echo "✗ This doesn't work (returns 0)"
fi
echo ""

echo "Test 3: Using participant parameter (old way)..."
curl -s "$FHIR/Encounter?participant=Practitioner/$PRAC_ID&_count=5" -o /tmp/test3.json
TOTAL3=$(python3 -c "import json; print(json.load(open('/tmp/test3.json')).get('total', 0))" 2>/dev/null || echo "ERROR")
echo "Result: $TOTAL3 encounters"
if [ "$TOTAL3" = "0" ]; then
    echo "✗ This doesn't work (returns 0)"
fi
echo ""

echo "=== Summary ==="
echo "Correct parameter: practitioner=$PRAC_ID (no prefix)"
echo "Returns: $TOTAL1 encounters"
echo ""

if [ "$TOTAL1" != "0" ] && [ "$TOTAL1" != "ERROR" ]; then
    echo "✓✓✓ FHIR query works!"
    echo ""
    echo "Now test your Search Service:"
    echo "  curl http://localhost:8084/api/search/doctors/$PRAC_ID/patients"
    echo "  curl http://localhost:8084/api/search/doctors/9999999391/patients"
else
    echo "✗✗✗ Something is wrong with the FHIR server"
fi