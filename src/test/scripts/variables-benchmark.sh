#!/usr/bin/env bash

// Run this from project root directory

echo "This should be run on an idle machine to prevent clean results!"
echo ""

java -jar target/odl-plastic-*-fat-tests.jar VariablesBenchmark

# Update the message below if there is a new performance line
#
echo "Previous recorded run performance is "
echo "   parsingSingle(...)   (min, avg, max) = (1.645, 1.687, 1.746)"
echo "   parsingMultiple(...) (min, avg, max) = (7.605, 7.764, 8.021)"