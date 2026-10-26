/**
 * Northline Banking Design System — Frontend Application Architecture
 * Scalable Online Banking System — UCS3513 System Design Laboratory
 * Pure Vanilla JavaScript, Native Browser Fetch API
 */

const API_BASE = '/api';

// Central Application State
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

// Global Autocomplete State Tracking
const autocompleteState = {
    global: {
        activeQueryId: 0,
        selectedIndex: -1,
        items: []
    },
    transfer: {
        activeQueryId: 0,
        selectedIndex: -1,
        items: []
    },
    biller: {
        activeQueryId: 0,
        selectedIndex: -1,
        items: []
    }
};

// ================= INITIALIZATION =================
document.addEventListener('DOMContentLoaded', () => {
    regenerateIdempotencyKey();
    setupGlobalShortcuts();
    setupClickOutsideListeners();

    if (state.token) {
        verifySession();
    } else {
        showSection('auth');
    }
});

function setupGlobalShortcuts() {
    document.addEventListener('keydown', (e) => {
        // Pressing '/' focuses global search when not typing in an input
        if (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) {
            e.preventDefault();
            const searchInput = document.getElementById('global-search-input');
            if (searchInput && state.token) {
                searchInput.focus();
                searchInput.select();
            }
        }
    });
}

function setupClickOutsideListeners() {
    document.addEventListener('click', (e) => {
        // Global search flyout click outside
        const globalSearchWrapper = document.getElementById('global-search-container');
        const globalDropdown = document.getElementById('global-search-dropdown');
        if (globalSearchWrapper && !globalSearchWrapper.contains(e.target)) {
            if (globalDropdown) globalDropdown.classList.remove('active');
        }

        // Transfer recipient flyout click outside
        const transferInput = document.getElementById('transfer-recipient-search-input');
        const transferDropdown = document.getElementById('transfer-recipient-dropdown');
        if (transferDropdown && transferInput && !transferInput.contains(e.target) && !transferDropdown.contains(e.target)) {
            transferDropdown.classList.remove('active');
        }

        // Biller dropdown click outside
        const billerInput = document.getElementById('biller-search-input');
        const billerDropdown = document.getElementById('biller-autocomplete-dropdown');
        if (billerDropdown && billerInput && !billerInput.contains(e.target) && !billerDropdown.contains(e.target)) {
            billerDropdown.classList.remove('active');
        }
    });
}

// ================= NOTIFICATION TOASTS =================
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'info';
    if (type === 'success') icon = 'check_circle';
    if (type === 'error') icon = 'error';

    toast.innerHTML = `
        <span class="material-symbols-outlined" style="font-size: 18px; color: ${type === 'success' ? 'var(--status-healthy-text)' : type === 'error' ? 'var(--status-error-text)' : 'var(--primary)'};">${icon}</span>
        <span>${escapeHtml(message)}</span>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 4500);
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
    document.querySelectorAll('.nav-tab-link').forEach(b => b.classList.remove('active'));

    const sec = document.getElementById(`section-${sectionId}`);
    if (sec) sec.classList.add('active');

    // Highlight nav link
    document.querySelectorAll('.nav-tab-link').forEach(b => {
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
    const navMenu = document.getElementById('nav-authenticated-menu');
    const searchContainer = document.getElementById('global-search-container');
    const sessionPulse = document.getElementById('session-pulse');
    const notifBtn = document.getElementById('notif-btn');
    const userDisplay = document.getElementById('user-display');
    const logoutBtn = document.getElementById('logout-btn');

    if (navMenu) navMenu.style.display = isAuth ? 'block' : 'none';
    if (searchContainer) searchContainer.style.display = isAuth ? 'block' : 'none';
    if (sessionPulse) sessionPulse.style.display = isAuth ? 'inline-flex' : 'none';
    if (notifBtn) notifBtn.style.display = isAuth ? 'inline-flex' : 'none';
    if (logoutBtn) logoutBtn.style.display = isAuth ? 'inline-flex' : 'none';
    if (userDisplay) userDisplay.style.display = isAuth ? 'inline-flex' : 'none';

    if (isAuth && state.user) {
        const userNameEl = document.getElementById('user-name');
        const userAvatarEl = document.getElementById('user-avatar');
        if (userNameEl) userNameEl.textContent = state.user.fullName;
        if (userAvatarEl) {
            const initials = state.user.fullName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
            userAvatarEl.textContent = initials || 'CU';
        }
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
        btn.textContent = 'Sign In to Portal';
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

        showToast(`Account created! Default Account: ${data.primaryAccountNumber}`, 'success');
        updateNavState();
        showSection('dashboard');
    } catch (err) {
        showToast('Registration error: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Create & Open Bank Account';
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
    state.currentAccount = null;
    state.beneficiaries = [];
    localStorage.removeItem('bank_token');
    updateNavState();
    showSection('auth');
    showToast('Session ended safely', 'info');
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
            opt.textContent = `${acc.accountType} (${acc.accountNumber}) — ₹${parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
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
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.85rem;">
                    <div>
                        <span class="badge badge-success">${escapeHtml(acc.accountType)}</span>
                        <h2 class="mono" style="font-size: 16px; font-weight: 600; margin-top: 0.35rem;">${escapeHtml(acc.accountNumber)}</h2>
                    </div>
                    <span class="badge ${acc.status === 'ACTIVE' ? 'badge-success' : 'badge-error'}">${escapeHtml(acc.status)}</span>
                </div>
                <div style="margin-bottom: 1rem;">
                    <span class="label-caps" style="display: block; margin-bottom: 0.2rem;">Authoritative Shard Balance</span>
                    <div class="mono" style="font-size: 24px; font-weight: 600; color: var(--text-primary);">
                        ₹ ${parseFloat(acc.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        <span style="font-size: 13px; font-weight: 500; color: var(--text-secondary);">INR</span>
                    </div>
                </div>
                <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                    <button class="btn btn-secondary btn-sm" onclick="checkAuthoritativeBalance('${acc.accountNumber}')">
                        <span class="material-symbols-outlined" style="font-size: 14px;">sync</span>
                        Verify Live Shard
                    </button>
                    <button class="btn btn-primary btn-sm" onclick="quickTransferFrom('${acc.accountNumber}')">
                        <span class="material-symbols-outlined" style="font-size: 14px;">send_money</span>
                        Transfer From
                    </button>
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
    const dashBal = document.getElementById('dash-balance');
    const custId = document.getElementById('dash-cust-id');
    const accStatus = document.getElementById('dash-acc-status');
    if (dashBal) dashBal.textContent = parseFloat(state.currentAccount.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 });
    if (custId) custId.textContent = state.currentAccount.customerId;
    if (accStatus) accStatus.textContent = state.currentAccount.status;
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

// ================= GLOBAL SEARCH AUTOCOMPLETE =================
let globalSearchDebounce = null;

function onGlobalSearchInput(val) {
    const clearBtn = document.getElementById('global-search-clear');
    const hint = document.getElementById('global-search-hint');
    const dropdown = document.getElementById('global-search-dropdown');

    if (val && val.length > 0) {
        if (clearBtn) clearBtn.style.display = 'block';
        if (hint) hint.style.display = 'none';
    } else {
        if (clearBtn) clearBtn.style.display = 'none';
        if (hint) hint.style.display = 'block';
        if (dropdown) dropdown.classList.remove('active');
        return;
    }

    const trimmed = val.trim();
    if (trimmed.length < 1) {
        if (dropdown) dropdown.classList.remove('active');
        return;
    }

    clearTimeout(globalSearchDebounce);
    const queryId = ++autocompleteState.global.activeQueryId;

    globalSearchDebounce = setTimeout(async () => {
        try {
            const res = await fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(trimmed)}&limit=12`);
            if (queryId !== autocompleteState.global.activeQueryId) return; // Discard stale request

            const data = await res.json();
            const suggestions = data.suggestions || [];
            
            // Augment with matching navigation items
            const navMatches = getNavigationSuggestions(trimmed);

            renderGlobalSearchDropdown(suggestions, navMatches, trimmed);
        } catch (err) {
            console.error('Global search autocomplete error', err);
        }
    }, 250);
}

function clearGlobalSearch() {
    const input = document.getElementById('global-search-input');
    const clearBtn = document.getElementById('global-search-clear');
    const hint = document.getElementById('global-search-hint');
    const dropdown = document.getElementById('global-search-dropdown');

    if (input) input.value = '';
    if (clearBtn) clearBtn.style.display = 'none';
    if (hint) hint.style.display = 'block';
    if (dropdown) dropdown.classList.remove('active');
}

function getNavigationSuggestions(query) {
    const navItems = [
        { term: 'Fund Transfers', category: 'NAV', referenceCode: 'transfers', desc: 'Atomic cross-shard fund transfers' },
        { term: 'Utility Bill Payments', category: 'NAV', referenceCode: 'bills', desc: 'Settle electricity, water, internet bills' },
        { term: 'Account Management', category: 'NAV', referenceCode: 'accounts', desc: 'View accounts, balances, open new account' },
        { term: 'Beneficiary Management', category: 'NAV', referenceCode: 'beneficiaries', desc: 'Registered transfer destinations' },
        { term: 'Transaction History & Audit', category: 'NAV', referenceCode: 'history', desc: 'Statements and double-entry ledger proofs' },
        { term: 'In-App Notifications', category: 'NAV', referenceCode: 'notifications', desc: 'Transactional outbox event feed' },
        { term: 'System Insights & Telemetry', category: 'NAV', referenceCode: 'system-insights', desc: 'Rate limiting, sharding, consistent hashing' }
    ];

    const q = query.toLowerCase();
    return navItems.filter(item => item.term.toLowerCase().includes(q) || item.desc.toLowerCase().includes(q));
}

function renderGlobalSearchDropdown(suggestions, navMatches, query) {
    const dropdown = document.getElementById('global-search-dropdown');
    if (!dropdown) return;

    dropdown.innerHTML = '';
    autocompleteState.global.items = [];
    autocompleteState.global.selectedIndex = -1;

    const totalResults = suggestions.length + navMatches.length;

    if (totalResults === 0) {
        const emptyDiv = document.createElement('div');
        emptyDiv.className = 'autocomplete-state-msg';
        emptyDiv.textContent = `No results found for "${query}"`;
        dropdown.appendChild(emptyDiv);
        dropdown.classList.add('active');
        return;
    }

    // Group suggestions by category
    const recipients = suggestions.filter(s => s.category === 'BENEFICIARY');
    const billers = suggestions.filter(s => s.category !== 'BENEFICIARY');

    // 1. Beneficiaries Section
    if (recipients.length > 0) {
        const title = document.createElement('div');
        title.className = 'autocomplete-group-title';
        title.textContent = 'Transfer Beneficiaries / Recipients';
        dropdown.appendChild(title);

        recipients.forEach(item => {
            const row = createAutocompleteRow(item, query, 'person', 'Recipient', () => {
                selectGlobalRecipient(item);
            });
            dropdown.appendChild(row);
            autocompleteState.global.items.push({ row, action: () => selectGlobalRecipient(item) });
        });
    }

    // 2. Utility Billers Section
    if (billers.length > 0) {
        const title = document.createElement('div');
        title.className = 'autocomplete-group-title';
        title.textContent = 'Supported Utility Providers';
        dropdown.appendChild(title);

        billers.forEach(item => {
            const row = createAutocompleteRow(item, query, 'receipt_long', item.category, () => {
                selectGlobalBiller(item);
            });
            dropdown.appendChild(row);
            autocompleteState.global.items.push({ row, action: () => selectGlobalBiller(item) });
        });
    }

    // 3. Navigation Shortcuts Section
    if (navMatches.length > 0) {
        const title = document.createElement('div');
        title.className = 'autocomplete-group-title';
        title.textContent = 'Application Views & Shortcuts';
        dropdown.appendChild(title);

        navMatches.forEach(item => {
            const row = createAutocompleteRow(item, query, 'navigation', 'Shortcut', () => {
                showSection(item.referenceCode);
                clearGlobalSearch();
            });
            dropdown.appendChild(row);
            autocompleteState.global.items.push({ row, action: () => {
                showSection(item.referenceCode);
                clearGlobalSearch();
            }});
        });
    }

    dropdown.classList.add('active');
}

function createAutocompleteRow(item, query, iconName, badgeText, onSelect) {
    const row = document.createElement('div');
    row.className = 'autocomplete-option';
    row.role = 'option';

    const left = document.createElement('div');
    left.className = 'autocomplete-option-left';

    const iconBadge = document.createElement('div');
    iconBadge.className = 'autocomplete-icon-badge';
    iconBadge.innerHTML = `<span class="material-symbols-outlined" style="font-size: 15px;">${iconName}</span>`;

    const textCol = document.createElement('div');
    textCol.className = 'autocomplete-text-col';

    const mainText = document.createElement('div');
    mainText.className = 'autocomplete-main-text';
    highlightTextInElement(mainText, item.term, query);

    const subText = document.createElement('div');
    subText.className = 'autocomplete-sub-text';
    subText.textContent = item.referenceCode ? `${item.referenceCode} · ${item.desc || item.category}` : (item.desc || '');

    textCol.appendChild(mainText);
    textCol.appendChild(subText);

    left.appendChild(iconBadge);
    left.appendChild(textCol);

    const badge = document.createElement('span');
    badge.className = `autocomplete-badge ${badgeText === 'Recipient' ? 'autocomplete-badge-beneficiary' : badgeText === 'Shortcut' ? 'autocomplete-badge-nav' : 'autocomplete-badge-biller'}`;
    badge.textContent = badgeText;

    row.appendChild(left);
    row.appendChild(badge);

    row.addEventListener('click', onSelect);
    return row;
}

function selectGlobalRecipient(item) {
    clearGlobalSearch();
    showSection('transfers');
    setTransferRecipient(item.term, item.referenceCode);
}

function selectGlobalBiller(item) {
    clearGlobalSearch();
    showSection('bills');
    document.getElementById('biller-search-input').value = item.term;
    document.getElementById('bill-biller-select').value = item.referenceCode;
}

function onGlobalSearchKeyDown(e) {
    const stateObj = autocompleteState.global;
    if (!stateObj.items.length) return;

    if (e.key === 'ArrowDown') {
        e.preventDefault();
        stateObj.selectedIndex = (stateObj.selectedIndex + 1) % stateObj.items.length;
        updateAutocompleteHighlight(stateObj);
    } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        stateObj.selectedIndex = (stateObj.selectedIndex - 1 + stateObj.items.length) % stateObj.items.length;
        updateAutocompleteHighlight(stateObj);
    } else if (e.key === 'Enter') {
        e.preventDefault();
        if (stateObj.selectedIndex >= 0 && stateObj.selectedIndex < stateObj.items.length) {
            stateObj.items[stateObj.selectedIndex].action();
        }
    } else if (e.key === 'Escape') {
        clearGlobalSearch();
    }
}

function updateAutocompleteHighlight(stateObj) {
    stateObj.items.forEach((item, index) => {
        item.row.classList.toggle('is-selected', index === stateObj.selectedIndex);
        if (index === stateObj.selectedIndex) {
            item.row.scrollIntoView({ block: 'nearest' });
        }
    });
}

// ================= TRANSFERS & RECIPIENT AUTOCOMPLETE =================
function regenerateIdempotencyKey() {
    const key = 'IDEMP-' + crypto.randomUUID();
    const input = document.getElementById('transfer-idempotency-key');
    if (input) input.value = key;
}

function copyIdempotencyKey() {
    const input = document.getElementById('transfer-idempotency-key');
    if (input && input.value) {
        navigator.clipboard.writeText(input.value);
        showToast('Idempotency Key copied to clipboard', 'info');
    }
}

function prepareTransferSection() {
    loadAccounts();
    loadBeneficiaries();
    regenerateIdempotencyKey();
    const resCard = document.getElementById('transfer-result-card');
    if (resCard) resCard.style.display = 'none';
    onTransferAmountInput(document.getElementById('transfer-amount')?.value || '0');
}

function quickTransferFrom(accountNumber) {
    showSection('transfers');
    const select = document.getElementById('transfer-source-account');
    if (select) select.value = accountNumber;
    onTransferSourceChange();
}

function onTransferSourceChange() {
    onTransferAmountInput(document.getElementById('transfer-amount')?.value || '0');
}

function onTransferAmountInput(amountVal) {
    const srcAccountNum = document.getElementById('transfer-source-account')?.value;
    const remainingEl = document.getElementById('transfer-remaining-balance');
    if (!remainingEl) return;

    const acc = state.accounts.find(a => a.accountNumber === srcAccountNum) || state.currentAccount;
    if (!acc) {
        remainingEl.textContent = '₹ 0.00 INR';
        return;
    }

    const currentBal = parseFloat(acc.balance) || 0;
    const sendAmt = parseFloat(amountVal) || 0;
    const postSettlement = currentBal - sendAmt;

    if (sendAmt > currentBal) {
        remainingEl.innerHTML = `<span style="color: var(--status-error-text); font-weight: 700;">Insufficient Balance (₹${postSettlement.toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR)</span>`;
    } else {
        remainingEl.innerHTML = `₹ ${postSettlement.toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR`;
    }
}

function applyTransferPreset(amt) {
    const input = document.getElementById('transfer-amount');
    if (input) {
        const cur = parseFloat(input.value) || 0;
        input.value = (cur + amt).toFixed(2);
        onTransferAmountInput(input.value);
    }
}

function applyTransferMax() {
    const srcAccountNum = document.getElementById('transfer-source-account')?.value;
    const acc = state.accounts.find(a => a.accountNumber === srcAccountNum) || state.currentAccount;
    if (acc) {
        const input = document.getElementById('transfer-amount');
        if (input) {
            input.value = parseFloat(acc.balance).toFixed(2);
            onTransferAmountInput(input.value);
        }
    }
}

// Recipient Search on Transfers Page
let transferRecipientDebounce = null;

function onTransferRecipientInput(val) {
    const clearBtn = document.getElementById('transfer-recipient-clear');
    const dropdown = document.getElementById('transfer-recipient-dropdown');

    if (val && val.length > 0) {
        if (clearBtn) clearBtn.style.display = 'block';
    } else {
        if (clearBtn) clearBtn.style.display = 'none';
        if (dropdown) dropdown.classList.remove('active');
        return;
    }

    const trimmed = val.trim();
    if (trimmed.length < 1) {
        if (dropdown) dropdown.classList.remove('active');
        return;
    }

    clearTimeout(transferRecipientDebounce);
    const queryId = ++autocompleteState.transfer.activeQueryId;

    transferRecipientDebounce = setTimeout(async () => {
        try {
            // Query Trie autocomplete endpoint
            const res = await fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(trimmed)}&limit=10`);
            if (queryId !== autocompleteState.transfer.activeQueryId) return;

            const data = await res.json();
            const suggestions = data.suggestions || [];

            // Blend with local user beneficiaries
            const localMatches = state.beneficiaries.filter(b => 
                b.name.toLowerCase().includes(trimmed.toLowerCase()) || 
                b.accountNumber.toLowerCase().includes(trimmed.toLowerCase())
            );

            renderTransferRecipientDropdown(suggestions, localMatches, trimmed);
        } catch (err) {
            console.error('Recipient autocomplete error', err);
        }
    }, 250);
}

function renderTransferRecipientDropdown(suggestions, localMatches, query) {
    const dropdown = document.getElementById('transfer-recipient-dropdown');
    if (!dropdown) return;

    dropdown.innerHTML = '';
    autocompleteState.transfer.items = [];
    autocompleteState.transfer.selectedIndex = -1;

    // Filter Trie suggestions for beneficiaries or direct account hits
    const trieBeneficiaries = suggestions.filter(s => s.category === 'BENEFICIARY');

    // Combine distinct entries
    const combined = [];
    const seenAccounts = new Set();

    localMatches.forEach(b => {
        if (!seenAccounts.has(b.accountNumber)) {
            seenAccounts.add(b.accountNumber);
            combined.push({ term: b.name, referenceCode: b.accountNumber, desc: b.bankName || 'Verified Beneficiary' });
        }
    });

    trieBeneficiaries.forEach(s => {
        if (!seenAccounts.has(s.referenceCode)) {
            seenAccounts.add(s.referenceCode);
            combined.push({ term: s.term, referenceCode: s.referenceCode, desc: 'Cluster Trie Directory' });
        }
    });

    if (combined.length === 0) {
        const emptyDiv = document.createElement('div');
        emptyDiv.className = 'autocomplete-state-msg';
        emptyDiv.textContent = `No recipients matching "${query}". You can enter the account number directly.`;
        dropdown.appendChild(emptyDiv);
        dropdown.classList.add('active');
        return;
    }

    combined.forEach(item => {
        const row = createAutocompleteRow(item, query, 'person', 'Verified', () => {
            setTransferRecipient(item.term, item.referenceCode);
            dropdown.classList.remove('active');
        });
        dropdown.appendChild(row);
        autocompleteState.transfer.items.push({
            row,
            action: () => {
                setTransferRecipient(item.term, item.referenceCode);
                dropdown.classList.remove('active');
            }
        });
    });

    dropdown.classList.add('active');
}

function onTransferRecipientKeyDown(e) {
    const stateObj = autocompleteState.transfer;
    if (!stateObj.items.length) return;

    if (e.key === 'ArrowDown') {
        e.preventDefault();
        stateObj.selectedIndex = (stateObj.selectedIndex + 1) % stateObj.items.length;
        updateAutocompleteHighlight(stateObj);
    } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        stateObj.selectedIndex = (stateObj.selectedIndex - 1 + stateObj.items.length) % stateObj.items.length;
        updateAutocompleteHighlight(stateObj);
    } else if (e.key === 'Enter') {
        e.preventDefault();
        if (stateObj.selectedIndex >= 0 && stateObj.selectedIndex < stateObj.items.length) {
            stateObj.items[stateObj.selectedIndex].action();
        }
    } else if (e.key === 'Escape') {
        const dropdown = document.getElementById('transfer-recipient-dropdown');
        if (dropdown) dropdown.classList.remove('active');
    }
}

function setTransferRecipient(name, accountNumber) {
    const searchInput = document.getElementById('transfer-recipient-search-input');
    const destAccountInput = document.getElementById('transfer-dest-account');
    const card = document.getElementById('transfer-verified-recipient-card');
    const cardName = document.getElementById('verified-recipient-name');
    const cardAcc = document.getElementById('verified-recipient-acc');
    const cardAvatar = document.getElementById('verified-recipient-avatar');
    const clearBtn = document.getElementById('transfer-recipient-clear');
    const dropdown = document.getElementById('transfer-recipient-dropdown');

    if (searchInput) searchInput.value = name;
    if (destAccountInput) destAccountInput.value = accountNumber;
    if (clearBtn) clearBtn.style.display = 'block';
    if (dropdown) dropdown.classList.remove('active');

    if (card) {
        if (cardName) cardName.textContent = name;
        if (cardAcc) cardAcc.textContent = accountNumber;
        if (cardAvatar) {
            const initials = name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
            cardAvatar.textContent = initials || 'RE';
        }
        card.style.display = 'flex';
    }
}

function clearTransferRecipient() {
    const searchInput = document.getElementById('transfer-recipient-search-input');
    const destAccountInput = document.getElementById('transfer-dest-account');
    const card = document.getElementById('transfer-verified-recipient-card');
    const clearBtn = document.getElementById('transfer-recipient-clear');
    const dropdown = document.getElementById('transfer-recipient-dropdown');

    if (searchInput) {
        searchInput.value = '';
        searchInput.focus();
    }
    if (destAccountInput) destAccountInput.value = '';
    if (clearBtn) clearBtn.style.display = 'none';
    if (card) card.style.display = 'none';
    if (dropdown) dropdown.classList.remove('active');
}

async function handleTransferSubmit(e) {
    e.preventDefault();
    const sourceAccountNumber = document.getElementById('transfer-source-account').value;
    const destinationAccountNumber = document.getElementById('transfer-dest-account').value.trim();
    const amount = document.getElementById('transfer-amount').value;
    const description = document.getElementById('transfer-description').value;
    const idempotencyKey = document.getElementById('transfer-idempotency-key').value;
    const btn = document.getElementById('btn-transfer-submit');

    if (!destinationAccountNumber) {
        showToast('Please search and select a verified recipient destination', 'error');
        return;
    }

    if (sourceAccountNumber === destinationAccountNumber) {
        showToast('Source and destination accounts must be distinct', 'error');
        return;
    }

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

        // Show confirmation receipt card
        const resultCard = document.getElementById('transfer-result-card');
        const resultMsg = document.getElementById('transfer-result-message');
        const resultDetails = document.getElementById('transfer-result-details');

        resultMsg.textContent = data.message;
        resultDetails.innerHTML = `
Transaction Reference: <strong>${escapeHtml(data.transactionReference)}</strong>
Principal Amount: <strong>₹${parseFloat(data.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR</strong>
Routing: ${escapeHtml(data.sourceAccountNumber)} ➔ ${escapeHtml(data.destinationAccountNumber)}
Updated Ledger Balance: <strong>₹${parseFloat(data.sourceBalanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR</strong>
Cluster Status: ${escapeHtml(data.status)} | Idempotent Replay: ${data.idempotentReplay}
        `;
        resultCard.style.display = 'block';

        showToast('Atomic transfer settled successfully!', 'success');
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
            const el = document.getElementById('dash-beneficiary-count');
            if (el) el.textContent = list.length;
        }
    } catch (ignored) {}
}

function renderBeneficiaries() {
    const grid = document.getElementById('beneficiaries-grid');
    if (!grid) return;

    grid.innerHTML = '';
    if (state.beneficiaries.length === 0) {
        grid.innerHTML = '<p class="autocomplete-state-msg" style="grid-column: 1/-1;">No beneficiaries added yet.</p>';
        return;
    }

    state.beneficiaries.forEach(b => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem;">
                <div class="recipient-avatar-box">${escapeHtml(b.name.substring(0, 2).toUpperCase())}</div>
                <div>
                    <div style="font-weight: 600; font-size: 14px;">${escapeHtml(b.name)}</div>
                    <div class="mono" style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(b.accountNumber)}</div>
                </div>
            </div>
            <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 1rem;">
                Bank: <span style="color: var(--text-primary); font-weight: 500;">${escapeHtml(b.bankName)}</span>
            </div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-primary btn-sm" onclick="transferToBeneficiary('${b.name}', '${b.accountNumber}')">
                    <span class="material-symbols-outlined" style="font-size: 14px;">send</span>
                    Transfer
                </button>
                <button class="btn btn-danger btn-sm" onclick="removeBeneficiary('${b.id}')">
                    <span class="material-symbols-outlined" style="font-size: 14px;">delete</span>
                    Delete
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

function transferToBeneficiary(name, accountNumber) {
    showSection('transfers');
    setTransferRecipient(name, accountNumber);
}

function openBeneficiarySelector() {
    loadBeneficiaries();
    const list = document.getElementById('transfer-beneficiary-picker-list');
    list.innerHTML = '';
    if (state.beneficiaries.length === 0) {
        list.innerHTML = '<p class="autocomplete-state-msg">No beneficiaries available. You can add one under the Beneficiaries tab.</p>';
    } else {
        state.beneficiaries.forEach(b => {
            const item = document.createElement('div');
            item.className = 'autocomplete-option';
            item.innerHTML = `
                <div style="display: flex; align-items: center; gap: 0.75rem;">
                    <div class="recipient-avatar-box">${escapeHtml(b.name.substring(0, 2).toUpperCase())}</div>
                    <div>
                        <div style="font-weight: 600; font-size: 13px;">${escapeHtml(b.name)}</div>
                        <div class="mono" style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(b.accountNumber)}</div>
                    </div>
                </div>
                <button class="btn btn-secondary btn-sm">Select</button>
            `;
            item.onclick = () => {
                setTransferRecipient(b.name, b.accountNumber);
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
        showToast('Beneficiary registered and indexed in Trie!', 'success');
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
            showToast('Beneficiary removed', 'info');
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
    document.querySelectorAll('#section-bills .segmented-btn').forEach(b => {
        b.classList.toggle('active', b.textContent.toUpperCase() === category);
    });
    loadBillers(category);
}

function renderBillerSelect() {
    const sel = document.getElementById('bill-biller-select');
    if (!sel) return;
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
        const searchInput = document.getElementById('biller-search-input');
        if (searchInput) searchInput.value = biller.name;
    }
}

let billerSearchDebounce = null;
function onBillerSearchInput(prefix) {
    clearTimeout(billerSearchDebounce);
    const dropdown = document.getElementById('biller-autocomplete-dropdown');
    if (!prefix || prefix.length < 1) {
        if (dropdown) dropdown.classList.remove('active');
        return;
    }

    const trimmed = prefix.trim();
    billerSearchDebounce = setTimeout(async () => {
        try {
            const res = await fetch(`${API_BASE}/autocomplete?prefix=${encodeURIComponent(trimmed)}&limit=8`);
            const data = await res.json();
            if (!dropdown) return;
            dropdown.innerHTML = '';
            
            const billerSuggestions = (data.suggestions || []).filter(s => s.category !== 'BENEFICIARY');

            if (billerSuggestions.length > 0) {
                billerSuggestions.forEach(s => {
                    const item = document.createElement('div');
                    item.className = 'autocomplete-option';
                    item.innerHTML = `
                        <div style="display: flex; align-items: center; gap: 0.6rem;">
                            <span class="material-symbols-outlined" style="color: var(--primary); font-size: 16px;">receipt</span>
                            <div>
                                <div style="font-weight: 500; font-size: 13px;">${escapeHtml(s.term)}</div>
                                <div class="mono" style="font-size: 11px; color: var(--text-muted);">${escapeHtml(s.referenceCode)}</div>
                            </div>
                        </div>
                        <span class="badge badge-neutral">${escapeHtml(s.category)}</span>
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
            console.error('Biller autocomplete failed', err);
        }
    }, 200);
}

async function handleBillPaySubmit(e) {
    e.preventDefault();
    const sourceAccountNumber = document.getElementById('bill-source-account').value;
    const billerCode = document.getElementById('bill-biller-select').value;
    const consumerReference = document.getElementById('bill-consumer-ref').value;
    const amount = document.getElementById('bill-amount').value;
    const btn = document.getElementById('btn-bill-submit');

    btn.disabled = true;
    btn.textContent = 'Settling bill...';

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

        showToast(`Utility bill settled! Reference: ${data.paymentReference}`, 'success');
        document.getElementById('bill-amount').value = '';
        document.getElementById('bill-consumer-ref').value = '';
        loadAccounts();
        loadBillHistory();
        loadNotifications();
    } catch (err) {
        showToast('Bill payment error: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Confirm & Settle Utility Bill';
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
        if (!tbody) return;
        tbody.innerHTML = '';
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="autocomplete-state-msg">No bill payments recorded yet.</td></tr>';
            return;
        }

        list.forEach(p => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="mono">${new Date(p.timestamp).toLocaleDateString()}</td>
                <td><strong>${escapeHtml(p.billerName)}</strong></td>
                <td><span class="mono">${escapeHtml(p.consumerReference)}</span></td>
                <td class="text-right mono" style="font-weight: 600;">₹${parseFloat(p.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                <td><span class="badge badge-success">${escapeHtml(p.status)}</span></td>
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

        const pageInfo = document.getElementById('history-page-info');
        const prevBtn = document.getElementById('btn-hist-prev');
        const nextBtn = document.getElementById('btn-hist-next');

        if (pageInfo) pageInfo.textContent = `Page ${page + 1} of ${Math.max(1, data.totalPages)} (Total: ${data.totalElements} records)`;
        if (prevBtn) prevBtn.disabled = page <= 0;
        if (nextBtn) nextBtn.disabled = page >= data.totalPages - 1;
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
        tbody.innerHTML = `<tr><td colspan="${showAuditButton ? 9 : 7}" class="autocomplete-state-msg">No transaction records found.</td></tr>`;
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
            auditTd = `<td style="text-align: center;"><button class="btn btn-secondary btn-sm" onclick="auditLedger('${t.transactionReference}')">Audit Ledger</button></td>`;
        }

        tr.innerHTML = `
            <td class="mono" style="font-size: 12px; color: var(--text-secondary);">${new Date(t.timestamp).toLocaleString()}</td>
            <td><span class="mono" style="font-weight: 500;">${escapeHtml(t.transactionReference)}</span></td>
            <td><span class="badge badge-neutral">${escapeHtml(t.type)}</span></td>
            <td><span class="badge ${badgeClass}">${escapeHtml(t.direction)}</span></td>
            <td class="mono">${escapeHtml(counterparty || 'N/A')}</td>
            <td class="text-right mono" style="font-weight: 600; color: ${isDebit ? 'var(--status-error-text)' : 'var(--status-healthy-text)'};">${sign}₹${parseFloat(t.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
            <td><span class="badge badge-success">${escapeHtml(t.status)}</span></td>
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
=== Double-Entry Forensic Verification ===
Transaction Reference: ${audit.transactionReference}
Total Debits:          ₹ ${parseFloat(audit.totalDebits).toFixed(2)} INR
Total Credits:         ₹ ${parseFloat(audit.totalCredits).toFixed(2)} INR
Mathematical Delta:    ₹ ${parseFloat(audit.difference).toFixed(2)} INR

Ledger Invariant Status:
${audit.isBalanced ? '✓ PERFECTLY BALANCED [Sum(Debits) == Sum(Credits)]' : '✗ RECONCILIATION ERROR'}

Ledger Journal Postings:
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
        const unreadCountEl = document.getElementById('unread-count');
        const notifDot = document.getElementById('notif-dot');

        if (unreadCountEl) unreadCountEl.textContent = data.unreadCount;
        if (notifDot) notifDot.style.display = data.unreadCount > 0 ? 'block' : 'none';

        renderNotifications(data.notifications);
    } catch (ignored) {}
}

function renderNotifications(notifications) {
    const list = document.getElementById('notifications-list');
    if (!list) return;

    list.innerHTML = '';
    if (!notifications || notifications.length === 0) {
        list.innerHTML = '<p class="autocomplete-state-msg">No notifications.</p>';
        return;
    }

    notifications.forEach(n => {
        const card = document.createElement('div');
        card.style.background = n.read ? 'var(--surface-card)' : 'var(--surface-low)';
        card.style.border = '1px solid ' + (n.read ? 'var(--border)' : 'var(--primary-fixed)');
        card.style.borderRadius = 'var(--radius-sm)';
        card.style.padding = '0.85rem 1rem';
        card.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.25rem;">
                <strong style="font-size: 13px;">${escapeHtml(n.title)}</strong>
                <span class="mono" style="font-size: 11px; color: var(--text-muted);">${new Date(n.timestamp).toLocaleTimeString()}</span>
            </div>
            <p style="font-size: 12px; color: var(--text-secondary); margin-bottom: 0.5rem;">${escapeHtml(n.message)}</p>
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <span class="badge ${n.read ? 'badge-neutral' : 'badge-success'}">${n.read ? 'READ' : 'UNREAD'}</span>
                ${!n.read ? `<button class="btn btn-secondary btn-sm" onclick="markNotificationAsRead('${n.notificationId}')">Mark as Read</button>` : ''}
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
    document.querySelectorAll('#section-system-insights .insight-tab-pill').forEach(b => {
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
    output.textContent = 'Firing 15 rapid concurrent requests to /api/auth/login with token-bucket evaluation...\n';

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
                output.textContent += `Request #${i}: HTTP 429 Too Many Requests [REJECTED by Redis Token Bucket]\n`;
            } else {
                allowed++;
                output.textContent += `Request #${i}: HTTP ${res.status} [ALLOWED by Token Bucket]\n`;
            }
        } catch (e) {
            output.textContent += `Request #${i}: Error: ${e.message}\n`;
        }
    }
    output.textContent += `\nTest Complete: ${allowed} Allowed, ${rejected} Rejected (HTTP 429).`;
    loadRateLimiterMetrics();
}

async function resetRateLimiterMetrics() {
    await fetch(`${API_BASE}/system/ratelimiter/reset`, { method: 'POST' });
    showToast('Rate limiter telemetry reset', 'info');
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
Sample Size:        ${data.sampleSize.toLocaleString()} IDs
Total Duration:     ${data.durationMs} ms
Throughput:         ${data.ratePerSecond.toLocaleString()} IDs / second
Collisions:         ${data.collisionsObserved} (0.00% Collision Rate)
Sample Generated:   ${data.sampleId}
Timestamp:          ${data.timestamp}`;
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
            Short Code: <strong class="mono">${escapeHtml(data.shortCode)}</strong><br>
            Short Link: <a href="${data.shortUrl}" target="_blank" style="color: var(--primary); font-weight: 500;">${window.location.origin}${data.shortUrl}</a><br>
            Target Route: <code class="mono">${escapeHtml(data.targetUrl)}</code> (Redis Cache-Aside)
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
Prefix:             "${data.prefix}"
Data Source:        ${data.source}
Redis Cache Hit:    ${data.cached}
Lookup Latency:     ${data.latencyMicroseconds} µs (${(data.latencyMicroseconds / 1000).toFixed(4)} ms)

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
            tbody.innerHTML = '<tr><td colspan="4" class="autocomplete-state-msg">No records extracted yet. Run crawl first.</td></tr>';
            return;
        }

        records.forEach(r => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code class="mono">${escapeHtml(r.billerCode)}</code></td>
                <td><strong>${escapeHtml(r.name)}</strong></td>
                <td><span class="badge badge-success">${escapeHtml(r.category)}</span></td>
                <td><code class="mono" style="color: var(--primary);">${escapeHtml(r.sourceUrl)}</code></td>
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
        out.textContent = `=== Dataset Scaling Experiment Results ===\n${JSON.stringify(data, null, 2)}`;
        showToast('Synthetic dataset generated!', 'success');
    } catch (err) {
        out.textContent = 'Dataset scaling error: ' + err.message;
    }
}

// ================= UTILITIES & SAFE RENDERING =================
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function highlightTextInElement(parentEl, text, query) {
    parentEl.innerHTML = '';
    if (!query) {
        parentEl.textContent = text;
        return;
    }

    const lowerText = text.toLowerCase();
    const lowerQuery = query.toLowerCase();
    const idx = lowerText.indexOf(lowerQuery);

    if (idx === -1) {
        parentEl.textContent = text;
        return;
    }

    const before = text.substring(0, idx);
    const match = text.substring(idx, idx + query.length);
    const after = text.substring(idx + query.length);

    if (before) parentEl.appendChild(document.createTextNode(before));
    const span = document.createElement('span');
    span.className = 'autocomplete-highlight';
    span.textContent = match;
    parentEl.appendChild(span);
    if (after) parentEl.appendChild(document.createTextNode(after));
}
