#!/usr/bin/env bash

// Run this from project root directory

echo "This should be run on an idle machine to prevent clean results!"
echo ""

java -jar target/odl-plastic-*-fat-tests.jar JsonFinderBinderBenchmark

# Update the message below if there is a new performance line
#
echo "Previous recorded run performance is (min, avg, max) = (7.881, 7.929, 8.011) sec"