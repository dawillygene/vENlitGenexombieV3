<?php
// Database connection
$servername = "";
$username = "";
$password = "";
$dbname = "";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Get JSON data
$json = file_get_contents('php://input');
$data = json_decode($json);

// Validate input
if (!isset($data->id)) {
    echo json_encode(array("success" => false, "error" => "Invalid input"));
    exit;
}

// Prepare and bind
$stmt = $conn->prepare("DELETE FROM poems WHERE id = ?");
$stmt->bind_param("i", $id);

// Set parameters and execute
$id = $data->id;

if ($stmt->execute()) {
    echo json_encode(array("success" => true));
} else {
    echo json_encode(array("success" => false, "error" => "Error: " . $stmt->error));
}

// Close statement and connection
$stmt->close();
$conn->close();
?>