#!/bin/bash
# Compilation script for Adaptive Fog Computing implementation
# Author: Narek Naltakyan

echo "========================================="
echo "Compiling Adaptive Fog Computing Framework"
echo "========================================="

# Set classpath
CLASSPATH="jars/*:jars/commons-math3-3.5/*:src:out"

# Create output directory if it doesn't exist
mkdir -p out

echo ""
echo "[1/7] Compiling models package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/models/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile models package"
    exit 1
fi

echo "[2/7] Compiling loadbalancing package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/loadbalancing/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile loadbalancing package"
    exit 1
fi

echo "[3/7] Compiling BaaS package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/baas/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile BaaS package"
    exit 1
fi

echo "[4/7] Compiling mobility package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/mobility/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile mobility package"
    exit 1
fi

echo "[5/7] Compiling federation package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/federation/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile federation package"
    exit 1
fi

echo "[6/7] Compiling resourcesharing package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/resourcesharing/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile resourcesharing package"
    exit 1
fi

echo "[7/7] Compiling scenarios package..."
javac -d out -cp "$CLASSPATH" src/org/fog/adaptive/scenarios/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile scenarios package"
    exit 1
fi

echo ""
echo "========================================="
echo "✅ Compilation successful!"
echo "========================================="
echo ""
echo "Class files generated:"
find out/org/fog/adaptive -name "*.class" | wc -l | xargs echo

echo ""
echo "To run the Smart City Traffic Scenario:"
echo "  ./run_scenario.sh"
echo ""
