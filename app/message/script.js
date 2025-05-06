document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');
    const conversationList = document.getElementById('conversationList');
    const messagesContainer = document.getElementById('messagesContainer');
    const chatContact = document.getElementById('chatContact');
    const backButton = document.getElementById('backButton');
    const loader = document.getElementById('loader');
    
    let conversations = {};
    let activeConversation = null;
    
    // Show loader
    loader.style.display = 'flex';
    
    // Fetch messages using AJAX
    fetch('read_messages.php')
        .then(response => response.json())
        .then(data => {
            loader.style.display = 'none';
            processMessages(data);
        })
        .catch(error => {
            loader.style.display = 'none';
            console.error('Error fetching messages:', error);
            messagesContainer.innerHTML = `
                <div class="empty-state">
                    <h3>Error loading messages</h3>
                    <p>${error.message}</p>
                </div>
            `;
        });
    
    // Process and group messages by sender
    function processMessages(messages) {
        // Group messages by sender
        messages.forEach(msg => {
            if (!conversations[msg.sender]) {
                conversations[msg.sender] = [];
            }
            conversations[msg.sender].push(msg);
        });
        
        // Sort messages within each conversation by timestamp
        for (const sender in conversations) {
            conversations[sender].sort((a, b) => {
                return new Date(a.timestamp) - new Date(b.timestamp);
            });
        }
        
        // Create conversation list
        populateConversationList();
    }
    
    // Create and display conversation list
    function populateConversationList() {
        let conversationHTML = '';
        
        // Sort conversations by most recent message
        const sortedConversations = Object.keys(conversations).sort((a, b) => {
            const aLatest = conversations[a][conversations[a].length - 1].timestamp;
            const bLatest = conversations[b][conversations[b].length - 1].timestamp;
            return new Date(bLatest) - new Date(aLatest);
        });
        
        sortedConversations.forEach(sender => {
            const messages = conversations[sender];
            const latestMessage = messages[messages.length - 1];
            const formattedTime = formatTimestamp(latestMessage.timestamp, true);
            
            conversationHTML += `
                <li class="conversation" data-sender="${sender}">
                    <div class="contact-name">${formatSenderName(sender)}</div>
                    <div class="preview-message">
                        ${latestMessage.message.substring(0, 50)}${latestMessage.message.length > 50 ? '...' : ''}
                        <span class="message-time">${formattedTime}</span>
                    </div>
                </li>
            `;
        });
        
        conversationList.innerHTML = conversationHTML;
        
        // Add click event to conversation items
        document.querySelectorAll('.conversation').forEach(item => {
            item.addEventListener('click', function() {
                const sender = this.getAttribute('data-sender');
                showConversation(sender);
                
                // For mobile: hide sidebar, show main content
                if (window.innerWidth <= 768) {
                    sidebar.classList.add('hidden');
                }
                
                // Mark this conversation as active
                document.querySelectorAll('.conversation').forEach(conv => {
                    conv.classList.remove('active');
                });
                this.classList.add('active');
            });
        });
    }
    
    // Show messages for selected conversation
    function showConversation(sender) {
        activeConversation = sender;
        const messages = conversations[sender];
        let messagesHTML = '';
        
        messages.forEach(msg => {
            const formattedTime = formatTimestamp(msg.timestamp);
            
            messagesHTML += `
                <div class="message received">
                    ${msg.message}
                    <span class="message-timestamp">${formattedTime}</span>
                </div>
            `;
        });
        
        messagesContainer.innerHTML = messagesHTML;
        chatContact.textContent = formatSenderName(sender);
        
        // Scroll to bottom of messages
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
    
    // Format sender name for display
    function formatSenderName(sender) {
        // Check if it's a service message (like M-PESA)
        if (!sender.startsWith('+')) {
            return sender;
        }
        
        // For regular phone numbers, format nicely
        return sender;
    }
    
    // Format timestamp for display
    function formatTimestamp(timestamp, shortFormat = false) {
        const date = new Date(timestamp);
        const now = new Date();
        const isToday = date.toDateString() === now.toDateString();
        
        // Format time
        const hours = date.getHours();
        const minutes = date.getMinutes().toString().padStart(2, '0');
        const timeStr = `${hours}:${minutes}`;
        
        if (shortFormat) {
            if (isToday) {
                return timeStr;
            }
            
            // Return just the date for older messages
            return `${date.getDate()}/${date.getMonth() + 1}`;
        }
        
        // Full format
        if (isToday) {
            return `Today at ${timeStr}`;
        }
        
        // For older messages, include the date
        return `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()} ${timeStr}`;
    }
    
    // Handle back button for mobile view
    backButton.addEventListener('click', function() {
        sidebar.classList.remove('hidden');
    });
    
    // Handle window resize
    window.addEventListener('resize', function() {
        if (window.innerWidth > 768) {
            sidebar.classList.remove('hidden');
        }
    });
});
