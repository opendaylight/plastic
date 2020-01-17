#!/usr/bin/env bash

# Run this from project root directory

echo ""
echo "This should be run on an idle machine to prevent clean results!"
echo ""

java -jar target/odl-plastic-*-fat-tests.jar VariablesBenchmark

# Update the message below if there is a new performance line
#
echo "Previous recorded run performance is "
echo "   parsingSingle(...)   (min, avg, max) = (1.629, 1.652, 1.693)"
echo "   parsingMultiple(...) (min, avg, max) = (5.683, 5.939, 6.344)"
echo ""
