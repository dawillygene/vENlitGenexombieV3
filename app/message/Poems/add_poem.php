<?php
// Database connection
$servername = "localhost";
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
if (!isset($data->title) || !isset($data->content) || !isset($data->author)) {
    echo json_encode(array("success" => false, "error" => "Invalid input"));
    exit;
}

// Prepare and bind
$stmt = $conn->prepare("INSERT INTO poems (title, content, author, created_at) VALUES (?, ?, ?, NOW())");
$stmt->bind_param("sss", $title, $content, $author);

// Set parameters and execute
$title = $data->title;
$content = $data->content;
$author = $data->author;

if ($stmt->execute()) {
    echo json_encode(array("success" => true));
} else {
    echo json_encode(array("success" => false, "error" => "Error: " . $stmt->error));
}

// Close statement and connection
$stmt->close();
$conn->close();
?>