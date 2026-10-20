/**
 * Scalable Online Banking System — Frontend Application Logic
 * UCS3513 System Design Laboratory
 * Plain Vanilla JavaScript, Browser Fetch API, Direct Spring Boot Static Serving
 */

const API_BASE = '/api';

// Application State
let state = {
    token: localStorage.getItem('bank_token') || null,
    user: null,
    accounts: [],
    currentAccount: null,
    beneficiaries: [],
    billers: [],
    currentHistoryPage: 0,
    totalHistoryPages: 1
};

// ================= INITIALIZATION =================
document.addEventListener('DOMContentLoaded', () => {
    regenerateIdempotencyKey();
    if (state.token) {
        verifySession();
    } else {
        showSection('auth');
    }
});

// ================= NOTIFICATION TOASTS =================
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 4000);
}

// ================= MODAL CONTROLS =================
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

// ================= NAVIGATION =================
function showSection(sectionId) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));

    const sec = document.getElementById(`section-${sectionId}`);
    if (sec) sec.classList.add('active');

    // Highlight nav button
    document.querySelectorAll('.nav-item').forEach(b => {
        if (b.getAttribute('onclick')?.includes(sectionId)) {
            b.classList.add('active');
        }
    });

    // Refresh specific section data
    if (sectionId === 'dashboard') loadDashboardData();
    if (sectionId === 'accounts') loadAccounts();
    if (sectionId === 'transfers') prepareTransferSection();
    if (sectionId === 'bills') loadBillers();
    if (sectionId === 'beneficiaries') loadBeneficiaries();
    if (sectionId === 'history') loadTransactionHistory(0);
    if (sectionId === 'notifications') loadNotifications();
    if (sectionId === 'system-insights') loadRateLimiterMetrics();
}

function updateNavState() {
    const isAuth = !!state.token;
    document.getElementById('nav-authenticated-menu').style.display = isAuth ? 'flex' : 'none';
    document.getElementById('notif-btn').style.display = isAuth ? 'block' : 'none';
    document.getElementById('logout-btn').style.display = isAuth ? 'block' : 'none';
    document.getElementById('user-display').style.display = isAuth ? 'block' : 'none';

    if (isAuth && state.user) {
        document.getElementById('user-display').innerHTML = `👤 <strong>${state.user.fullName}</strong>`;
    }
}

// ================= AUTHENTICATION =================
function toggleAuthTab(tab) {
    document.getElementById('tab-login-btn').classList.toggle('active', tab === 'login');
    document.getElementById('tab-register-btn').classList.toggle('active', tab === 'register');
    document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
}

function fillDemoLogin(email, password) {
    toggleAuthTab('login');
    document.getElementById('login-email').value = email;
    document.getElementById('login-password').value = password;
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('btn-login-submit');

    btn.disabled = true;
    btn.textContent = 'Verifying credentials...';

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();
        if (res.status === 429) {
            showToast(`⚠️ Rate limit exceeded! Please wait ${data.retryAfterSeconds}s`, 'error');
            return;
        }

        if (!res.ok) {
            showToast(data.message || 'Login failed', 'error');
            return;
        }

        state.token = data.token;
        state.user = {
            customerId: data.customerId,
            email: data.email,
            fullName: data.fullName,
            role: data.role
        };
        localStorage.setItem('bank_token', data.token);

        showToast(`Welcome back, ${data.fullName}!`, 'success');
        updateNavState();
        showSection('dashboard');
    } catch (err) {
        showToast('Network error during login: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sign In to Account';
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const fullName = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    const phoneNumber = document.getElementById('reg-phone').value;
    const initialDeposit = document.getElementById('reg-deposit').value;
    const btn = document.getElementById('btn-register-submit');

    btn.disabled = true;
    btn.textContent = 'Registering customer...';

    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, password, phoneNumber, initialDeposit })
        });

        const data = await res.json();
        if (!res.ok) {
            showToast(data.message || 'Registration failed', 'error');
            return;
        }

        state.token = data.token;
        state.user = {
            customerId: data.customerId,
            email: data.email,
            fullName: data.fullName,
            role: data.role
        };
        localStorage.setItem('bank_token', data.token);

        showToast(`Account created successfully! Account Number: ${data.primaryAccountNumber}`, 'success');
        updateNavState();
        showSection('dashboard');
    } catch (err) {
        showToast('Registration error: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Create Account & Open Default Bank Account';
    }
}

async function verifySession() {
    try {
        const res = await fetch(`${API_BASE}/auth/me`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            state.user = await res.json();
            updateNavState();
            showSection('dashboard');
            loadNotifications();
        } else {
            logout();
        }
    } catch {
        logout();
    }
}

function logout() {
    state.token = null;
    state.user = null;
    state.accounts = [];
    localStorage.removeItem('bank_token');
    updateNavState();
    showSection('auth');
    showToast('Logged out successfully', 'info');
}

// ================= DASHBOARD & ACCOUNTS =================
async function loadDashboardData() {
    await loadAccounts();
    await loadRecentTransactions();
    await loadBeneficiaryCount();
    loadNotifications();
}

async function loadAccounts() {
    try {
        const res = await fetch(`${API_BASE}/accounts`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        state.accounts = await res.json();
        renderAccounts();
        updateDashboardBalance();
    } catch (err) {
        console.error('Failed to load accounts', err);
    }
}

function renderAccounts() {
    const grid = document.getElementById('accounts-grid');
    const dashSelect = document.getElementById('dash-account-selector');
    const transferSrcSelect = document.getElementById('transfer-source-account');
    const billSrcSelect = document.getElementById('bill-source-account');

    if (!state.currentAccount && state.accounts.length > 0) {
        state.currentAccount = state.accounts[0];
    }

    // Populate selectors
    [dashSelect, transferSrcSelect, billSrcSelect].forEach(sel => {
        if (!sel) return;
        sel.innerHTML = '';
        state.accounts.forEach(acc => {
            const opt = document.createElement('option');
            opt.value = acc.accountNumber;
            opt.textContent = `${acc.accountType} (${acc.accountNumber}) — ₹${parseFloat(acc.balance).toLocaleString('en-IN')}`;
            if (state.currentAccount && acc.accountNumber === state.currentAccount.accountNumber) {
                opt.selected = true;
            }
            sel.appendChild(opt);
        });
    });

    // Populate Accounts Grid
    if (grid) {
        grid.innerHTML = '';
        state.accounts.forEach(acc => {
            const card = document.createElement('div');
            card.className = 'card';
            card.innerHTML = `
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
                    <div>
                        <span class="badge badge-active">${acc.accountType}</span>
                        <h2 style="margin-top: 0.5rem; font-family: monospace;">${acc.accountNumber}</h2>
                    </div>
                    <span class="badge ${acc.status === 'ACTIVE' ? 'badge-credit' : 'badge-debit'}">${acc.status}</span>
                </div>
                <div style="margin-bottom: 1rem;">
                    <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase;">Authoritative Balance</div>
                    <div style="font-size: 2rem; font-weight: 700; color: #ffffff;">₹ ${parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
                </div>
                <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-secondary" style="font-size: 0.85rem;" onclick="checkAuthoritativeBalance('${acc.accountNumber}')">Verify Live Shard Balance</button>
                    <button class="btn btn-primary" style="font-size: 0.85rem;" onclick="quickTransferFrom('${acc.accountNumber}')">Transfer From</button>
                </div>
            `;
            grid.appendChild(card);
        });
    }
}

function onAccountChange(accountNumber) {
    state.currentAccount = state.accounts.find(a => a.accountNumber === accountNumber);
    updateDashboardBalance();
}

function updateDashboardBalance() {
    if (!state.currentAccount) return;
    document.getElementById('dash-balance').textContent = parseFloat(state.currentAccount.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('dash-cust-id').textContent = state.currentAccount.customerId;
    document.getElementById('dash-acc-status').textContent = state.currentAccount.status;
}

async function checkAuthoritativeBalance(accountNumber) {
    try {
        const res = await fetch(`${API_BASE}/accounts/${accountNumber}/balance`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        const data = await res.json();
        showToast(`Authoritative Balance for ${accountNumber}: ₹${data.balance} INR (${data.authoritativeSource})`, 'info');
        loadAccounts();
    } catch (err) {
        showToast('Error checking balance: ' + err.message, 'error');
    }
}

async function handleOpenAccountSubmit(e) {
    e.preventDefault();
    const accountType = document.getElementById('new-account-type').value;
    const initialDeposit = document.getElementById('new-account-deposit').value;

    try {
        const res = await fetch(`${API_BASE}/accounts`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ accountType, initialDeposit })
        });
        const acc = await res.json();
        closeModal('modal-open-account');
        showToast(`New ${acc.accountType} account opened: ${acc.accountNumber}`, 'success');
        loadAccounts();
    } catch (err) {
        showToast('Failed to open account: ' + err.message, 'error');
    }
}

// ================= TRANSFERS =================
function regenerateIdempotencyKey() {
    const key = 'IDEMP-' + crypto.randomUUID();
    const input = document.getElementById('transfer-idempotency-key');
    if (input) input.value = key;
}

function prepareTransferSection() {
    loadAccounts();
    regenerateIdempotencyKey();
    document.getElementById('transfer-result-card').style.display = 'none';
}

function quickTransferFrom(accountNumber) {
    showSection('transfers');
    const select = document.getElementById('transfer-source-account');
    if (select) select.value = accountNumber;
}

async function handleTransferSubmit(e) {
    e.preventDefault();
    const sourceAccountNumber = document.getElementById('transfer-source-account').value;
    const destinationAccountNumber = document.getElementById('transfer-dest-account').value.trim();
    const amount = document.getElementById('transfer-amount').value;
    const description = document.getElementById('transfer-description').value;
    const idempotencyKey = document.getElementById('transfer-idempotency-key').value;
    const btn = document.getElementById('btn-transfer-submit');

    btn.disabled = true;
    btn.textContent = 'Processing transaction in cluster...';

    try {
        const res = await fetch(`${API_BASE}/transfers`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json',
                'X-Idempotency-Key': idempotencyKey
            },
            body: JSON.stringify({
                sourceAccountNumber,
                destinationAccountNumber,
                amount,
                description,
                idempotencyKey
            })
        });

        const data = await res.json();
        if (res.status === 429) {
            showToast(`⚠️ Rate limit reached! Retry after ${data.retryAfterSeconds}s`, 'error');
            return;
        }

        if (!res.ok) {
            showToast(data.message || 'Transfer failed', 'error');
            return;
        }

        // Show confirmation card
        const resultCard = document.getElementById('transfer-result-card');
        const resultMsg = document.getElementById('transfer-result-message');
        const resultDetails = document.getElementById('transfer-result-details');

        resultMsg.textContent = data.message;
        resultDetails.innerHTML = `
            Reference: <strong>${data.transactionReference}</strong><br>
            Amount: <strong>₹${parseFloat(data.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR</strong><br>
            Source: ${data.sourceAccountNumber} ➔ Destination: ${data.destinationAccountNumber}<br>
            Updated Source Balance: <strong>₹${parseFloat(data.sourceBalanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR</strong><br>
            Status: ${data.status} | Idempotent Replay: ${data.idempotentReplay}
        `;
        resultCard.style.display = 'block';

        showToast('Transfer completed successfully!', 'success');
        loadAccounts();
        loadNotifications();
        regenerateIdempotencyKey();
    } catch (err) {
        showToast('Transfer error: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Authorize & Execute Transfer';
    }
}

// ================= BENEFICIARIES =================
async function loadBeneficiaries() {
    try {
        const res = await fetch(`${API_BASE}/beneficiaries`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        state.beneficiaries = await res.json();
        renderBeneficiaries();
    } catch (err) {
        console.error('Failed to load beneficiaries', err);
    }
}

async function loadBeneficiaryCount() {
    try {
        const res = await fetch(`${API_BASE}/beneficiaries`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            const list = await res.json();
            document.getElementById('dash-beneficiary-count').textContent = list.length;
        }
    } catch (ignored) {}
}

function renderBeneficiaries() {
    const grid = document.getElementById('beneficiaries-grid');
    if (!grid) return;

    grid.innerHTML = '';
    if (state.beneficiaries.length === 0) {
        grid.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 2rem;">No beneficiaries added yet.</p>';
        return;
    }

    state.beneficiaries.forEach(b => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
            <div style="font-size: 1.15rem; font-weight: 700; color: #ffffff; margin-bottom: 0.25rem;">${b.name}</div>
            <div style="font-family: monospace; color: #818cf8; font-size: 0.9rem; margin-bottom: 0.5rem;">${b.accountNumber}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 1rem;">Bank: ${b.bankName}</div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-primary" style="font-size: 0.8rem; padding: 0.4rem 0.8rem;" onclick="transferToBeneficiary('${b.accountNumber}')">Transfer</button>
                <button class="btn btn-outline-danger" style="font-size: 0.8rem; padding: 0.4rem 0.8rem;" onclick="removeBeneficiary('${b.id}')">Delete</button>
            </div>
        `;
        grid.appendChild(card);
    });
}

function transferToBeneficiary(accountNumber) {
    showSection('transfers');
    document.getElementById('transfer-dest-account').value = accountNumber;
}

function openBeneficiarySelector() {
    loadBeneficiaries();
    const list = document.getElementById('transfer-beneficiary-picker-list');
    list.innerHTML = '';
    if (state.beneficiaries.length === 0) {
        list.innerHTML = '<p style="color: var(--text-muted); text-align: center;">No beneficiaries available.</p>';
    } else {
        state.beneficiaries.forEach(b => {
            const item = document.createElement('div');
            item.className = 'autocomplete-item';
            item.innerHTML = `
                <div>
                    <strong>${b.name}</strong>
                    <div style="font-family: monospace; font-size: 0.8rem; color: #818cf8;">${b.accountNumber}</div>
                </div>
                <button class="btn btn-secondary" style="font-size: 0.75rem; padding: 0.3rem 0.6rem;">Select</button>
            `;
            item.onclick = () => {
                document.getElementById('transfer-dest-account').value = b.accountNumber;
                closeModal('modal-select-beneficiary');
            };
            list.appendChild(item);
        });
    }
    openModal('modal-select-beneficiary');
}

async function handleAddBeneficiarySubmit(e) {
    e.preventDefault();
    const name = document.getElementById('ben-name').value;
    const accountNumber = document.getElementById('ben-account').value.trim();
    const bankName = document.getElementById('ben-bank').value;
    const email = document.getElementById('ben-email').value;

    try {
        const res = await fetch(`${API_BASE}/beneficiaries`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name, accountNumber, bankName, email })
        });
        const data = await res.json();
        if (!res.ok) {
            showToast(data.message || 'Failed to add beneficiary', 'error');
            return;
        }

        closeModal('modal-add-beneficiary');
        showToast('Beneficiary registered successfully!', 'success');
        loadBeneficiaries();
    } catch (err) {
        showToast('Error adding beneficiary: ' + err.message, 'error');
    }
}

async function removeBeneficiary(id) {
    if (!confirm('Are you sure you want to delete this beneficiary?')) return;
    try {
        const res = await fetch(`${API_BASE}/beneficiaries/${id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (res.ok) {
            showToast('Beneficiary deleted', 'info');
            loadBeneficiaries();
        }
    } catch (err) {
        showToast('Error removing beneficiary: ' + err.message, 'error');
    }
}

// ================= BILL PAYMENTS =================
async function loadBillers(category = 'ALL') {
    try {
        const url = category && category !== 'ALL' ? `${API_BASE}/bills/billers?category=${category}` : `${API_BASE}/bills/billers`;
        const res = await fetch(url, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        state.billers = await res.json();
        renderBillerSelect();
        loadBillHistory();
    } catch (err) {
        console.error('Failed to load billers', err);
    }
}

function filterBillerCategory(category) {
    document.querySelectorAll('#section-bills .tab-btn').forEach(b => {
        b.classList.toggle('active', b.textContent.toUpperCase() === category);
    });
    loadBillers(category);
}

function renderBillerSelect() {
    const sel = document.getElementById('bill-biller-select');
    sel.innerHTML = '<option value="">-- Select Utility Provider --</option>';
    state.billers.forEach(b => {
        const opt = document.createElement('option');
        opt.value = b.billerCode;
        opt.textContent = `[${b.category}] ${b.name} (${b.billerCode})`;
        sel.appendChild(opt);
    });
}

function onBillerSelected(billerCode) {
    const biller = state.billers.find(b => b.billerCode === billerCode);
    if (biller) {
        document.getElementById('biller-search-input').value = biller.name;
    }
}

let searchDebounceTimeout = null;
function onBillerSearchInput(prefix) {
    clearTimeout(searchDebounceTimeout);
    const dropdown = document.getElementById('biller-autocomplete-dropdown');
    if (!prefix || prefix.length < 2) {
        dropdown.classList.remove('active');
        return;
    }

    searchDebounceTimeout = setTimeout(async () => {
        try {
            const res = await fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(prefix)}`);
            const data = await res.json();
            dropdown.innerHTML = '';
            if (data.suggestions && data.suggestions.length > 0) {
                data.suggestions.forEach(s => {
                    const item = document.createElement('div');
                    item.className = 'autocomplete-item';
                    item.innerHTML = `
                        <div>
                            <strong>${s.term}</strong>
                            <div style="font-size: 0.75rem; color: #818cf8;">Code: ${s.referenceCode}</div>
                        </div>
                        <span class="badge badge-active">${s.category}</span>
                    `;
                    item.onclick = () => {
                        document.getElementById('bill-biller-select').value = s.referenceCode;
                        document.getElementById('biller-search-input').value = s.term;
                        dropdown.classList.remove('active');
                    };
                    dropdown.appendChild(item);
                });
                dropdown.classList.add('active');
            } else {
                dropdown.classList.remove('active');
            }
        } catch (err) {
            console.error('Autocomplete fetch failed', err);
        }
    }, 200);
}

async function handleBillPaySubmit(e) {
    e.preventDefault();
    const sourceAccountNumber = document.getElementById('bill-source-account').value;
    const billerCode = document.getElementById('bill-biller-select').value;
    const consumerReference = document.getElementById('bill-consumer-ref').value;
    const amount = document.getElementById('bill-amount').value;

    try {
        const res = await fetch(`${API_BASE}/bills/pay`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ sourceAccountNumber, billerCode, consumerReference, amount })
        });
        const data = await res.json();
        if (!res.ok) {
            showToast(data.message || 'Payment failed', 'error');
            return;
        }

        showToast(`Utility bill paid successfully! Ref: ${data.paymentReference}`, 'success');
        document.getElementById('bill-amount').value = '';
        document.getElementById('bill-consumer-ref').value = '';
        loadAccounts();
        loadBillHistory();
        loadNotifications();
    } catch (err) {
        showToast('Bill payment error: ' + err.message, 'error');
    }
}

async function loadBillHistory() {
    try {
        const res = await fetch(`${API_BASE}/bills/history`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        const list = await res.json();
        const tbody = document.getElementById('bill-history-tbody');
        tbody.innerHTML = '';
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No bill payments recorded yet.</td></tr>';
            return;
        }

        list.forEach(p => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${new Date(p.timestamp).toLocaleDateString()}</td>
                <td><strong>${p.billerName}</strong></td>
                <td><code style="color: #a5b4fc;">${p.consumerReference}</code></td>
                <td>₹${parseFloat(p.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                <td><span class="badge badge-credit">${p.status}</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (ignored) {}
}

// ================= TRANSACTION HISTORY & AUDIT =================
async function loadRecentTransactions() {
    try {
        const res = await fetch(`${API_BASE}/transactions?page=0&size=5`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        const page = await res.json();
        renderTransactionTable('dash-recent-tx-tbody', page.content);
    } catch (err) {
        console.error('Failed to load recent transactions', err);
    }
}

async function loadTransactionHistory(page = 0) {
    state.currentHistoryPage = page;
    const type = document.getElementById('history-type-filter').value;
    const url = `${API_BASE}/transactions?page=${page}&size=10${type !== 'ALL' ? '&type=' + type : ''}`;

    try {
        const res = await fetch(url, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        const data = await res.json();
        state.totalHistoryPages = data.totalPages;

        renderTransactionTable('history-table-tbody', data.content, true);

        document.getElementById('history-page-info').textContent = `Page ${page + 1} of ${Math.max(1, data.totalPages)} (Total: ${data.totalElements})`;
        document.getElementById('btn-hist-prev').disabled = page <= 0;
        document.getElementById('btn-hist-next').disabled = page >= data.totalPages - 1;
    } catch (err) {
        console.error('Failed to load transaction history', err);
    }
}

function prevHistoryPage() {
    if (state.currentHistoryPage > 0) {
        loadTransactionHistory(state.currentHistoryPage - 1);
    }
}

function nextHistoryPage() {
    if (state.currentHistoryPage < state.totalHistoryPages - 1) {
        loadTransactionHistory(state.currentHistoryPage + 1);
    }
}

function renderTransactionTable(tbodyId, transactions, showAuditButton = false) {
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;

    tbody.innerHTML = '';
    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="${showAuditButton ? 9 : 7}" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No transaction records found.</td></tr>`;
        return;
    }

    transactions.forEach(t => {
        const tr = document.createElement('tr');
        const isDebit = t.direction === 'DEBIT';
        const badgeClass = isDebit ? 'badge-debit' : 'badge-credit';
        const sign = isDebit ? '-' : '+';
        const counterparty = isDebit ? t.destinationAccount : t.sourceAccount;

        let auditTd = '';
        if (showAuditButton) {
            auditTd = `<td><button class="btn btn-secondary" style="font-size: 0.75rem; padding: 0.3rem 0.6rem;" onclick="auditLedger('${t.transactionReference}')">Audit Ledger</button></td>`;
        }

        tr.innerHTML = `
            <td>${new Date(t.timestamp).toLocaleString()}</td>
            <td><code style="color: #818cf8;">${t.transactionReference}</code></td>
            <td>${t.type}</td>
            <td><span class="badge ${badgeClass}">${t.direction}</span></td>
            <td style="font-family: monospace;">${counterparty || 'N/A'}</td>
            <td style="font-weight: 700; color: ${isDebit ? '#fca5a5' : '#86efac'};">${sign}₹${parseFloat(t.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
            <td><span class="badge badge-credit">${t.status}</span></td>
            ${auditTd}
        `;
        tbody.appendChild(tr);
    });
}

async function auditLedger(reference) {
    try {
        const res = await fetch(`${API_BASE}/transactions/${reference}/audit`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        const audit = await res.json();

        const detailsEl = document.getElementById('ledger-audit-details');
        detailsEl.innerHTML = `
Transaction Reference: <strong>${audit.transactionReference}</strong>
Entry Count: <strong>${audit.entryCount}</strong> (Double-Entry Pairs)
Total Debits: <strong>₹${parseFloat(audit.totalDebits).toFixed(2)} INR</strong>
Total Credits: <strong>₹${parseFloat(audit.totalCredits).toFixed(2)} INR</strong>
Mathematical Balance Difference: <strong>₹${parseFloat(audit.difference).toFixed(2)} INR</strong>
Ledger Status: <span style="color: ${audit.isBalanced ? '#34d399' : '#f87171'}; font-weight: 700;">
  ${audit.isBalanced ? '✓ PERFECTLY BALANCED (Sum Debits == Sum Credits)' : '✗ UNBALANCED ERROR'}
</span>

=== Double-Entry Ledger Records ===
${JSON.stringify(audit.ledgerEntries, null, 2)}
        `;
        openModal('modal-ledger-audit');
    } catch (err) {
        showToast('Audit check failed: ' + err.message, 'error');
    }
}

// ================= NOTIFICATIONS =================
async function loadNotifications() {
    try {
        const res = await fetch(`${API_BASE}/notifications`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        if (!res.ok) return;

        const data = await res.json();
        document.getElementById('unread-count').textContent = data.unreadCount;
        renderNotifications(data.notifications);
    } catch (ignored) {}
}

function renderNotifications(notifications) {
    const list = document.getElementById('notifications-list');
    if (!list) return;

    list.innerHTML = '';
    if (!notifications || notifications.length === 0) {
        list.innerHTML = '<p style="text-align: center; color: var(--text-muted); padding: 2rem;">No notifications.</p>';
        return;
    }

    notifications.forEach(n => {
        const card = document.createElement('div');
        card.style.background = n.read ? 'rgba(255,255,255,0.02)' : 'rgba(99,102,241,0.08)';
        card.style.border = '1px solid ' + (n.read ? 'var(--border-subtle)' : 'rgba(99,102,241,0.3)');
        card.style.borderRadius = 'var(--radius-md)';
        card.style.padding = '1rem';
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.25rem;">
                <strong>${n.title}</strong>
                <span style="font-size: 0.75rem; color: var(--text-muted);">${new Date(n.timestamp).toLocaleTimeString()}</span>
            </div>
            <p style="font-size: 0.85rem; margin-bottom: 0.5rem; color: #e2e8f0;">${n.message}</p>
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <span class="badge ${n.read ? 'badge-active' : 'badge-debit'}">${n.read ? 'READ' : 'UNREAD'}</span>
                ${!n.read ? `<button class="btn btn-secondary" style="font-size: 0.75rem; padding: 0.2rem 0.5rem;" onclick="markNotificationAsRead('${n.notificationId}')">Mark Read</button>` : ''}
            </div>
        `;
        list.appendChild(card);
    });
}

async function markNotificationAsRead(id) {
    try {
        await fetch(`${API_BASE}/notifications/${id}/read`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        loadNotifications();
    } catch (ignored) {}
}

async function markAllNotificationsAsRead() {
    try {
        await fetch(`${API_BASE}/notifications/read-all`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
        showToast('All notifications marked as read', 'info');
        loadNotifications();
    } catch (ignored) {}
}

// ================= SYSTEM INSIGHTS =================
function switchInsightTab(tabId) {
    document.querySelectorAll('#section-system-insights .tab-btn').forEach(b => {
        b.classList.toggle('active', b.getAttribute('onclick')?.includes(tabId));
    });
    document.querySelectorAll('.insight-content').forEach(c => {
        c.style.display = c.id === `insight-tab-${tabId}` ? 'block' : 'none';
    });

    if (tabId === 'ratelimit') loadRateLimiterMetrics();
    if (tabId === 'sharding') checkShardingStatus();
    if (tabId === 'consistenthash') loadConsistentHashDistribution();
    if (tabId === 'shortener') loadShortenerMetrics();
    if (tabId === 'crawler') refreshCrawlerStatus();
}

// 1. Rate Limiter Telemetry
async function loadRateLimiterMetrics() {
    try {
        const res = await fetch(`${API_BASE}/system/ratelimiter/metrics`);
        const data = await res.json();
        document.getElementById('rl-total-reqs').textContent = data.totalRequests;
        document.getElementById('rl-allowed-reqs').textContent = data.allowedRequests;
        document.getElementById('rl-rejected-reqs').textContent = data.rejectedRequests;
        document.getElementById('rl-rejection-pct').textContent = data.rejectionRatePercentage + '%';
    } catch (ignored) {}
}

async function simulateBurstTraffic(count = 15) {
    const output = document.getElementById('rl-burst-results');
    output.innerHTML = 'Fring 15 rapid concurrent requests to /api/auth/login with token bucket evaluation...\n';

    let allowed = 0;
    let rejected = 0;

    for (let i = 1; i <= count; i++) {
        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: 'burst.test@example.com', password: 'wrong' })
            });
            if (res.status === 429) {
                rejected++;
                output.innerHTML += `Request #${i}: HTTP 429 Too Many Requests [REJECTED by Redis Token Bucket]\n`;
            } else {
                allowed++;
                output.innerHTML += `Request #${i}: HTTP ${res.status} [ALLOWED by Token Bucket]\n`;
            }
        } catch (e) {
            output.innerHTML += `Request #${i}: Error: ${e.message}\n`;
        }
    }
    output.innerHTML += `\nTest Complete: ${allowed} Allowed, ${rejected} Rejected (HTTP 429).`;
    loadRateLimiterMetrics();
}

async function resetRateLimiterMetrics() {
    await fetch(`${API_BASE}/system/ratelimiter/reset`, { method: 'POST' });
    showToast('Rate limiter metrics reset', 'info');
    loadRateLimiterMetrics();
}

// 2. Sharding Status
async function checkShardingStatus() {
    const out = document.getElementById('sharding-status-output');
    out.textContent = 'Querying MongoDB cluster through mongos router...';
    try {
        const res = await fetch(`${API_BASE}/system/sharding/status`);
        const data = await res.json();
        out.textContent = JSON.stringify(data, null, 2);
    } catch (err) {
        out.textContent = 'Error connecting to database: ' + err.message;
    }
}

// 3. Consistent Hashing
async function loadConsistentHashDistribution() {
    try {
        const res = await fetch(`${API_BASE}/system/consistent-hash/nodes`);
        const data = await res.json();
        document.getElementById('ch-node-count').textContent = data.nodeCount;
        document.getElementById('ch-vnode-count').textContent = data.virtualNodesTotal;

        const distRes = await fetch(`${API_BASE}/system/consistent-hash/distribution?sampleSize=1000`);
        const distData = await distRes.json();
        document.getElementById('ch-experiment-output').textContent = JSON.stringify(distData, null, 2);
    } catch (err) {
        console.error(err);
    }
}

async function runConsistentHashExperiment(action, nodeName) {
    const out = document.getElementById('ch-experiment-output');
    out.textContent = `Executing topology mutation: ${action} node ${nodeName} on 1,000 keys...`;
    try {
        const res = await fetch(`${API_BASE}/system/consistent-hash/experiment`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action, nodeName, sampleSize: 1000 })
        });
        const data = await res.json();
        out.textContent = JSON.stringify(data, null, 2);
        loadConsistentHashDistribution();
    } catch (err) {
        out.textContent = 'Experiment error: ' + err.message;
    }
}

// 4. Unique ID Benchmark
async function runIdBenchmark(count) {
    const out = document.getElementById('idgen-output');
    out.textContent = `Benchmarking generation of ${count.toLocaleString()} Unique IDs with collision detection...`;
    try {
        const res = await fetch(`${API_BASE}/system/id-benchmark?count=${count}`);
        const data = await res.json();
        out.textContent = `=== Unique ID Benchmark Results ===
Sample Size: ${data.sampleSize.toLocaleString()} IDs
Total Duration: ${data.durationMs} ms
Throughput: ${data.ratePerSecond.toLocaleString()} IDs / second
Collisions Observed: ${data.collisionsObserved} (0.00% Collision Rate)
Generated Sample ID: ${data.sampleId}
Timestamp: ${data.timestamp}`;
    } catch (err) {
        out.textContent = 'Benchmark error: ' + err.message;
    }
}

// 5. URL Shortener
async function createShortUrl() {
    const targetUrl = document.getElementById('shortener-route-select').value;
    try {
        const res = await fetch(`${API_BASE}/shortener`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ targetUrl })
        });
        const data = await res.json();
        const resDiv = document.getElementById('shortener-result');
        resDiv.style.display = 'block';
        resDiv.innerHTML = `
            Short Code Created: <code style="color: #a5b4fc; font-weight: 700;">${data.shortCode}</code><br>
            Short Link: <a href="${data.shortUrl}" target="_blank" style="color: #67e8f9;">${window.location.origin}${data.shortUrl}</a><br>
            Redirects to: <code>${data.targetUrl}</code> (Cached via Redis)
        `;
        loadShortenerMetrics();
    } catch (err) {
        showToast('Shortener error: ' + err.message, 'error');
    }
}

async function loadShortenerMetrics() {
    try {
        const res = await fetch(`${API_BASE}/shortener/metrics`);
        const data = await res.json();
        document.getElementById('shortener-metrics').textContent = JSON.stringify(data, null, 2);
    } catch (ignored) {}
}

// 6. Trie Autocomplete Evaluation
async function evaluateTrieQuery() {
    const prefix = document.getElementById('trie-eval-input').value;
    const out = document.getElementById('trie-eval-output');
    out.textContent = `Searching Trie and Redis for prefix "${prefix}"...`;
    try {
        const res = await fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(prefix)}`);
        const data = await res.json();
        out.textContent = `=== Trie & Redis Autocomplete Evaluation ===
Search Prefix: "${data.prefix}"
Data Source: ${data.source}
Redis Cache Hit: ${data.cached}
Lookup Latency: ${data.latencyMicroseconds} µs (${(data.latencyMicroseconds / 1000).toFixed(4)} ms)

Matched Suggestions:
${JSON.stringify(data.suggestions, null, 2)}`;
    } catch (err) {
        out.textContent = 'Autocomplete evaluation error: ' + err.message;
    }
}

// 7. Synthetic Web Crawler
async function startCrawler() {
    const out = document.getElementById('crawler-metrics-output');
    out.textContent = 'Crawler started: Traversing local synthetic HTML pages and updating Redis visited set...';
    try {
        const res = await fetch(`${API_BASE}/crawler/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ seedUrl: '/synthetic/page1.html', maxPages: 10 })
        });
        const data = await res.json();
        out.textContent = JSON.stringify(data, null, 2);
        showToast('Crawl completed!', 'success');
        refreshCrawlerStatus();
    } catch (err) {
        out.textContent = 'Crawler error: ' + err.message;
    }
}

async function refreshCrawlerStatus() {
    try {
        const res = await fetch(`${API_BASE}/crawler/status`);
        const data = await res.json();
        document.getElementById('crawler-metrics-output').textContent = JSON.stringify(data, null, 2);

        const recRes = await fetch(`${API_BASE}/crawler/results`);
        const records = await recRes.json();
        const tbody = document.getElementById('crawler-records-tbody');
        tbody.innerHTML = '';
        if (records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--text-muted);">No records extracted yet.</td></tr>';
            return;
        }

        records.forEach(r => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>${r.billerCode}</code></td>
                <td><strong>${r.name}</strong></td>
                <td><span class="badge badge-active">${r.category}</span></td>
                <td><code style="color: #67e8f9;">${r.sourceUrl}</code></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (ignored) {}
}

// 8. Dataset Scaling
async function generateScalingData(customers, txns) {
    const out = document.getElementById('scaling-output');
    out.textContent = `Generating batch of ${customers} synthetic customers with ${customers * txns} transactions for MongoDB sharding stress test...`;
    try {
        const res = await fetch(`${API_BASE}/system/seed/dataset?customers=${customers}&transactionsPerCustomer=${txns}`, {
            method: 'POST'
        });
        const data = await res.json();
        out.textContent = `=== Dataset Scaling Experiment Results ===
${JSON.stringify(data, null, 2)}`;
        showToast('Synthetic dataset generated!', 'success');
    } catch (err) {
        out.textContent = 'Dataset scaling error: ' + err.message;
    }
}
