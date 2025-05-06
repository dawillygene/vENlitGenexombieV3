<?php
// messages.php

$filename = 'file.txt';

if (!file_exists($filename)) {
    header('HTTP/1.1 404 Not Found');
    echo json_encode(['error' => 'File not found']);
    exit;
}

$messages = [];
$file = fopen($filename, 'r');

while (($line = fgets($file)) !== FALSE) {
    $trimmed = trim($line);
    if (empty($trimmed)) continue;

    // Updated regex to capture any sender (phone or name)
    if (preg_match('/^Sender: ([^,]+), Timestamp: ([^,]+), Message: (.+)$/', $trimmed, $matches)) {
        $messages[] = [
            'sender' => $matches[1], // Captures "+255..." or "M-PESA"
            'timestamp' => $matches[2],
            'message' => $matches[3]
        ];
    }
}

fclose($file);

header('Content-Type: application/json');
echo json_encode($messages);
?>