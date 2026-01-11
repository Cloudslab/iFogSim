#!/bin/bash
# Run script for Smart City Traffic Scenario
# Author: Narek Naltakyan

echo "========================================="
echo "Smart City Traffic Management Scenario"
echo "Testing all 6 research papers"
echo "========================================="
echo ""

# Set classpath
CLASSPATH="jars/*:jars/commons-math3-3.5/*:out"

# Check if compiled
if [ ! -f "out/org/fog/adaptive/scenarios/SmartCityTrafficScenario.class" ]; then
    echo "ERROR: Scenario not compiled. Please run ./compile_adaptive.sh first"
    exit 1
fi

# Run the scenario
java -cp "$CLASSPATH" org.fog.adaptive.scenarios.SmartCityTrafficScenario

echo ""
echo "========================================="
echo "Scenario execution completed"
echo "========================================="
