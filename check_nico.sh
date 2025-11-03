#!/bin/bash
echo "🔍 Checking Nico project structure..."
echo "Java files:"
find app/src -name "*.java" | head -10
echo ""
echo "XML files:"
find app/src -name "*.xml" | head -10
echo ""
echo "✅ Check complete!"
