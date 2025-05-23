<?php
// Define the path to the file where SMS data will be stored
$filePath = 'messages.txt';

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

    // Check if the data is valid
    if (isset($data['sender'], $data['body'], $data['timestamp'])) {
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
        echo json_encode(['status' => 'error', 'message' => 'Invalid data']);
    }
} else {
    // Send an error response if the request method is not POST
    header('Content-Type: application/json');
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>