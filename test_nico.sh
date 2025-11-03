#!/bin/bash

echo "🧪 NICO MESSENGER - COMPREHENSIVE TEST"
echo "========================================"

# Проверяем структуру проекта
echo ""
echo "1. PROJECT STRUCTURE TEST:"
echo "--------------------------"

# Проверяем Java файлы
java_files=$(find app/src/main/java -name "*.java" | wc -l)
echo "✅ Java files: $java_files"

important_files=(
    "app/src/main/java/com/nico/MainActivity.java"
    "app/src/main/java/com/nico/ChatActivity.java" 
    "app/src/main/java/com/nico/ConnectActivity.java"
    "app/src/main/java/com/nico/DatabaseHelper.java"
    "app/src/main/java/com/nico/NetworkManager.java"
    "app/src/main/java/com/nico/NotificationHelper.java"
)

for file in "${important_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ MISSING: $file"
    fi
done

# Проверяем XML файлы
echo ""
echo "2. LAYOUT FILES TEST:"
echo "---------------------"

xml_files=$(find app/src/main/res -name "*.xml" | wc -l)
echo "✅ XML files: $xml_files"

layout_files=(
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/res/layout/activity_chat.xml"
    "app/src/main/res/layout/activity_connect.xml"
    "app/src/main/res/drawable/ios_input_background.xml"
    "app/src/main/res/drawable/message_bubble.xml"
)

for file in "${layout_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ MISSING: $file"
    fi
done

# Проверяем AndroidManifest
echo ""
echo "3. ANDROID MANIFEST TEST:"
echo "--------------------------"

if [ -f "app/src/main/AndroidManifest.xml" ]; then
    manifest_ok=$(grep -c "com.nico" app/src/main/AndroidManifest.xml)
    if [ $manifest_ok -ge 1 ]; then
        echo "✅ AndroidManifest.xml - Correct package"
    else
        echo "❌ AndroidManifest.xml - Wrong package"
    fi
    
    # Проверяем разрешения
    permissions=(
        "INTERNET"
        "ACCESS_NETWORK_STATE" 
        "ACCESS_WIFI_STATE"
    )
    
    for perm in "${permissions[@]}"; do
        if grep -q "android.permission.$perm" app/src/main/AndroidManifest.xml; then
            echo "✅ Permission: $perm"
        else
            echo "❌ MISSING Permission: $perm"
        fi
    done
    
else
    echo "❌ AndroidManifest.xml not found"
fi

# Проверяем зависимости
echo ""
echo "4. DEPENDENCIES TEST:"
echo "---------------------"

if [ -f "app/build.gradle" ]; then
    echo "✅ build.gradle exists"
    
    # Проверяем основные зависимости
    deps=(
        "androidx.appcompat"
        "com.google.android.material"
        "androidx.constraintlayout"
    )
    
    for dep in "${deps[@]}"; do
        if grep -q "$dep" app/build.gradle; then
            echo "✅ Dependency: $dep"
        else
            echo "⚠️  Missing dependency: $dep"
        fi
    done
else
    echo "❌ build.gradle not found"
fi

# Проверяем код на очевидные ошибки
echo ""
echo "5. CODE SYNTAX TEST:"
echo "--------------------"

# Проверяем импорты в основных файлах
echo "Checking imports in MainActivity..."
imports_ok=$(grep -c "import.*Intent" app/src/main/java/com/nico/MainActivity.java)
if [ $imports_ok -ge 1 ]; then
    echo "✅ MainActivity imports OK"
else
    echo "❌ MainActivity missing imports"
fi

echo "Checking NetworkManager structure..."
if grep -q "NetworkListener" app/src/main/java/com/nico/NetworkManager.java; then
    echo "✅ NetworkManager interface OK"
else
    echo "❌ NetworkManager interface missing"
fi

echo "Checking DatabaseHelper methods..."
if grep -q "addMessage" app/src/main/java/com/nico/DatabaseHelper.java; then
    echo "✅ DatabaseHelper methods OK"
else
    echo "❌ DatabaseHelper methods missing"
fi

# Финальный отчет
echo ""
echo "========================================"
echo "📊 TEST SUMMARY:"
echo "----------------------------------------"

total_checks=$(( ${#important_files[@]} + ${#layout_files[@]} + ${#permissions[@]} + 10 ))
echo "Total checks performed: $total_checks"

if [ $java_files -ge 6 ] && [ $xml_files -ge 5 ]; then
    echo "🎉 PROJECT STRUCTURE: EXCELLENT"
elif [ $java_files -ge 4 ] && [ $xml_files -ge 3 ]; then
    echo "👍 PROJECT STRUCTURE: GOOD" 
else
    echo "⚠️  PROJECT STRUCTURE: NEEDS IMPROVEMENT"
fi

echo ""
echo "🚀 NEXT STEPS:"
echo "1. Build the APK with: cd app && gradle assembleDebug"
echo "2. Install on Android device"
echo "3. Test network connection between two devices"
echo "4. Send your first message!"

echo ""
echo "💡 TIP: Make sure both devices are on the same Wi-Fi network"
echo "========================================"
