<?php
// Define the path to the file where SMS data will be stored
$filePath = 'messages.txt';
$hashFilePath = 'message_hashes.txt'; // Separate file for tracking hashes

// Function to ensure the file exists
function ensureFileExists($filePath) {
    if (!file_exists($filePath)) {
        // Create the file if it doesn't exist
        touch($filePath);
        // Optionally, you can set permissions if needed
        chmod($filePath, 0644);
    }
}

// Check if the request method is POST
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get the raw POST data
    $rawData = file_get_contents('php://input');

    // Check if the data is in the expected format
    parse_str($rawData, $data);

    // Check if the data is valid - updated to handle both old and new format
    if (isset($data['address'], $data['body'], $data['timestamp'], $data['type'])) {
        // New format with message type
        // Ensure the file exists
        ensureFileExists($filePath);
        ensureFileExists($hashFilePath);

        // Format the data to be written to the file
        $timestamp = date('Y-m-d H:i:s', $data['timestamp'] / 1000); // Convert timestamp to readable format
        $messageType = strtoupper($data['type']); // SENT or RECEIVED
        
        // Create a unique identifier for this message to prevent duplicates
        $messageHash = md5($data['address'] . $data['body'] . $data['timestamp'] . $data['type']);
        
        // Check if this message already exists in hash file
        if (file_exists($hashFilePath)) {
            $existingHashes = file_get_contents($hashFilePath);
            if (strpos($existingHashes, $messageHash) !== false) {
                // Message already exists, don't add duplicate
                header('Content-Type: application/json');
                echo json_encode([
                    'status' => 'duplicate', 
                    'message' => 'Message already processed',
                    'hash' => $messageHash
                ]);
                exit;
            }
        }
        
        // Store hash in separate file for duplicate detection
        file_put_contents($hashFilePath, $messageHash . "\n", FILE_APPEND);
        
        // Store clean message (without hash) in main file for display
        $line = "[" . $timestamp . "] " . $messageType . " | " . $data['address'] . " | " . $data['body'] . "\n";

        // Append the data to the file
        file_put_contents($filePath, $line, FILE_APPEND);

        // Send a success response with debug info
        header('Content-Type: application/json');
        echo json_encode([
            'status' => 'success', 
            'type' => $messageType,
            'address' => $data['address'],
            'timestamp' => $timestamp
        ]);
    } elseif (isset($data['sender'], $data['body'], $data['timestamp'])) {
        // Backward compatibility with old format
        // Ensure the file exists
        ensureFileExists($filePath);

        // Format the data to be written to the file
        $timestamp = date('Y-m-d H:i:s', $data['timestamp'] / 1000); // Convert timestamp to readable format
        $line = "Sender: " . $data['sender'] . ", Timestamp: " . $timestamp . ", Message: " . $data['body'] . "\n";

        // Append the data to the file
        file_put_contents($filePath, $line, FILE_APPEND);

        // Send a success response
        header('Content-Type: application/json');
        echo json_encode(['status' => 'success']);
    } else {
        // Send an error response if the data is invalid
        header('Content-Type: application/json');
        echo json_encode(['status' => 'error', 'message' => 'Invalid data - missing required fields']);
    }
} else {
    // Send an error response if the request method is not POST
    header('Content-Type: application/json');
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>