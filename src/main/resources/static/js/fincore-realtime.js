/**
 * FinCore Real-Time WebSocket/STOMP Client
 * Connects securely to /ws-fincore and dynamically updates UI without full-page reloads.
 */
document.addEventListener("DOMContentLoaded", function () {
    let stompClient = null;
    let reconnectAttempts = 0;
    const maxReconnectAttempts = 5;

    function initWebSocket() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn("SockJS or Stomp library not loaded. Real-time updates disabled.");
            return;
        }

        const socket = new SockJS('/ws-fincore');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Suppress verbose console logs

        stompClient.connect({}, function (frame) {
            reconnectAttempts = 0;
            console.log("Connected to FinCore WebSocket broker");

            // 1. Subscribe to User-Specific Notifications
            stompClient.subscribe('/user/queue/notifications', function (message) {
                try {
                    const event = JSON.parse(message.body);
                    handleUserEvent(event);
                } catch (err) {
                    console.error("Error processing user STOMP notification:", err);
                }
            });

            // 2. Subscribe to Admin Activity if on admin dashboard
            const adminFeed = document.getElementById("admin-live-feed");
            if (adminFeed) {
                stompClient.subscribe('/topic/admin/activity', function (message) {
                    try {
                        const event = JSON.parse(message.body);
                        handleAdminEvent(event);
                    } catch (err) {
                        console.error("Error processing admin STOMP event:", err);
                    }
                });
            }

            updateConnectionStatus(true);
        }, function (error) {
            console.warn("WebSocket disconnected:", error);
            updateConnectionStatus(false);
            attemptReconnect();
        });
    }

    function attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            const timeout = Math.min(1000 * Math.pow(2, reconnectAttempts), 15000);
            console.log(`Scheduling reconnect attempt ${reconnectAttempts} in ${timeout}ms`);
            setTimeout(initWebSocket, timeout);
        }
    }

    function updateConnectionStatus(connected) {
        const indicator = document.getElementById("ws-status-indicator");
        if (indicator) {
            indicator.className = connected ? "badge bg-success" : "badge bg-warning";
            indicator.textContent = connected ? "Live (Connected)" : "Connecting...";
        }
    }

    function handleUserEvent(event) {
        // Show Toast Notification
        showToast(event.title || "Transaction Update", event.message, event.eventType === "TRANSFER_DEBIT" ? "warning" : "success");

        // Increment notification bell badge
        const badge = document.getElementById("notification-badge");
        if (badge) {
            let count = parseInt(badge.textContent || "0", 10);
            badge.textContent = count + 1;
            badge.classList.remove("d-none");
        }

        // Dynamically prepend to recent transactions table if on customer dashboard
        const tbody = document.getElementById("recent-tx-tbody");
        if (tbody) {
            const row = document.createElement("tr");
            const isDebit = event.eventType === "TRANSFER_DEBIT";
            row.className = "table-info"; // highlight new row
            row.innerHTML = `
                <td><code>${event.referenceNumber}</code></td>
                <td><span class="badge ${isDebit ? 'bg-danger' : 'bg-success'}">${isDebit ? 'DEBIT' : 'CREDIT'}</span></td>
                <td><strong>${parseFloat(event.amount).toLocaleString('en-IN', {minimumFractionDigits: 2})} ${event.currency}</strong></td>
                <td>${event.sourceAccountNumber} &rarr; ${event.destinationAccountNumber}</td>
                <td><span class="badge bg-success">COMPLETED</span></td>
                <td>Just now</td>
            `;
            tbody.insertBefore(row, tbody.firstChild);
        }
    }

    function handleAdminEvent(event) {
        const feed = document.getElementById("admin-live-feed");
        if (feed) {
            const item = document.createElement("li");
            item.className = `list-group-item list-group-item-${event.eventType === 'FLAGGED_TRANSACTION' ? 'warning' : 'light'} d-flex justify-content-between align-items-center`;
            item.innerHTML = `
                <div>
                    <strong>${event.referenceNumber}</strong>: ${event.message}
                </div>
                <span class="badge ${event.eventType === 'FLAGGED_TRANSACTION' ? 'bg-danger' : 'bg-primary'}">${event.eventType}</span>
            `;
            feed.insertBefore(item, feed.firstChild);
        }
    }

    function showToast(title, message, type) {
        const container = document.getElementById("toast-container");
        if (!container) return;

        const toastId = "toast-" + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0 show shadow-lg mb-2" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <strong>${title}</strong><br/>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;
        container.insertAdjacentHTML("beforeend", toastHtml);
        const toastElement = document.getElementById(toastId);
        setTimeout(() => {
            if (toastElement) {
                toastElement.remove();
            }
        }, 6000);
    }

    // Auto-fill demo account in transfer forms if button clicked
    document.querySelectorAll(".quick-fill-btn").forEach(btn => {
        btn.addEventListener("click", function () {
            const acc = this.getAttribute("data-account");
            const input = document.getElementById("destinationAccountNumber");
            if (input && acc) {
                input.value = acc;
            }
        });
    });

    initWebSocket();
});
