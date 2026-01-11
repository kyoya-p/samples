// --- Configuration State ---
let CONFIG = {
    CLIENT_ID: '',
    API_KEY: '',
    SCOPES: 'https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.file'
};

const DOM = {
    configSection: document.getElementById('config-section'),
    inputClientId: document.getElementById('input-client-id'),
    inputApiKey: document.getElementById('input-api-key'),
    saveConfigBtn: document.getElementById('save-config-btn'),

    authSection: document.getElementById('auth-section'),
    loginContainer: document.getElementById('login-container'),
    userProfile: document.getElementById('user-profile'),
    userAvatar: document.getElementById('user-avatar'),
    userName: document.getElementById('user-name'),
    signoutBtn: document.getElementById('signout-btn'),

    mainContent: document.getElementById('main-content'),
    setupNotice: document.getElementById('setup-notice'),

    sheetTitleInput: document.getElementById('sheet-title'),
    createSheetBtn: document.getElementById('create-sheet-btn'),
    spreadsheetIdInput: document.getElementById('spreadsheet-id'),
    cellRangeInput: document.getElementById('cell-range'),
    cellValueInput: document.getElementById('cell-value'),
    updateCellBtn: document.getElementById('update-cell-btn'),
    statusLog: document.getElementById('status-log')
};

let accessToken = null;
let currentUser = null;

// --- Initialization ---
async function initApis() {
    const clientId = DOM.inputClientId.value.trim();
    const apiKey = DOM.inputApiKey.value.trim();

    if (!clientId) {
        logStatus("Error: Client ID is required.");
        return;
    }

    CONFIG.CLIENT_ID = clientId;
    CONFIG.API_KEY = apiKey;

    logStatus("Initializing Google APIs with provided credentials...");

    try {
        // 1. Initialize GAPI Client for Sheets
        logStatus("Loading GAPI client...");
        await new Promise((resolve, reject) => {
            const timeout = setTimeout(() => reject(new Error("GAPI load timeout (possibly due to file:// protocol restrictions)")), 5000);
            gapi.load('client', () => {
                clearTimeout(timeout);
                resolve();
            });
        });

        await gapi.client.init({
            apiKey: CONFIG.API_KEY,
            discoveryDocs: ['https://sheets.googleapis.com/$discovery/rest?version=v4'],
        });
        logStatus("GAPI client initialized.");

        // 2. Initialize Google Identity Services (GSI)
        logStatus("Initializing Google Identity Services...");
        google.accounts.id.initialize({
            client_id: CONFIG.CLIENT_ID,
            callback: handleCredentialResponse
        });

        // Render the sign-in button
        google.accounts.id.renderButton(
            document.getElementById('google-signin-btn'),
            { theme: 'outline', size: 'large', text: 'signin_with' }
        );

        DOM.authSection.classList.remove('disabled');
        DOM.configSection.classList.add('disabled');
        logStatus("Configuration saved. Please Sign In.");
    } catch (err) {
        logError(err);
        if (window.location.protocol === 'file:') {
            logStatus("TIP: Google APIs often block 'file://' requests. Please try using a local web server (e.g. VS Code Live Server).");
        }
    }
}

// --- Auth Logic ---
function handleCredentialResponse(response) {
    const payload = JSON.parse(atob(response.credential.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    currentUser = { name: payload.name, picture: payload.picture };

    logStatus(`Logged in as ${currentUser.name}. Requesting API access...`);

    // Request access token with OAuth2 client
    const tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: CONFIG.CLIENT_ID,
        scope: CONFIG.SCOPES,
        callback: (tokenResponse) => {
            if (tokenResponse.error !== undefined) {
                logError(tokenResponse);
                return;
            }
            accessToken = tokenResponse.access_token;
            logStatus("Access token acquired. Application ready.");
            updateAuthUI();
        },
    });
    tokenClient.requestAccessToken({ prompt: '' });
}

function updateAuthUI() {
    if (currentUser && accessToken) {
        DOM.loginContainer.classList.add('hidden');
        DOM.userProfile.classList.remove('hidden');
        DOM.mainContent.classList.remove('disabled');
        DOM.setupNotice.classList.add('hidden');
        DOM.userAvatar.src = currentUser.picture;
        DOM.userName.textContent = currentUser.name;
    } else {
        DOM.loginContainer.classList.remove('hidden');
        DOM.userProfile.classList.add('hidden');
        DOM.mainContent.classList.add('disabled');
        DOM.setupNotice.classList.remove('hidden');
    }
}

function signOut() {
    accessToken = null;
    currentUser = null;
    updateAuthUI();
    DOM.authSection.classList.add('disabled');
    DOM.configSection.classList.remove('disabled');
    logStatus("Signed out. Configuration reset.");
}

// --- Sheets API Operations ---
async function createSpreadsheet() {
    const title = DOM.sheetTitleInput.value.trim() || 'New Spreadsheet';
    try {
        logStatus(`Creating spreadsheet: "${title}"...`);
        const response = await gapi.client.sheets.spreadsheets.create({
            resource: { properties: { title: title } }
        });
        const spreadsheetId = response.result.spreadsheetId;
        DOM.spreadsheetIdInput.value = spreadsheetId;
        logStatus(`Success! Spreadsheet ID: ${spreadsheetId}`);
        logStatus(`Link: https://docs.google.com/spreadsheets/d/${spreadsheetId}`);
    } catch (err) {
        logError(err);
    }
}

async function updateCell() {
    const spreadsheetId = DOM.spreadsheetIdInput.value.trim();
    const range = DOM.cellRangeInput.value.trim();
    const value = DOM.cellValueInput.value.trim();

    if (!spreadsheetId || !range) {
        logStatus("Error: Spreadsheet ID and Range are required.");
        return;
    }

    try {
        logStatus(`Updating ${range} with "${value}"...`);
        const response = await gapi.client.sheets.spreadsheets.values.update({
            spreadsheetId: spreadsheetId,
            range: range,
            valueInputOption: 'RAW',
            resource: { values: [[value]] }
        });
        logStatus(`Updated! Cells updated: ${response.result.updatedCells}`);
    } catch (err) {
        logError(err);
    }
}

// --- Utilities ---
function logStatus(msg) {
    const entry = document.createElement('div');
    entry.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
    DOM.statusLog.prepend(entry);
}

function logError(err) {
    console.error(err);
    const msg = err.result?.error?.message || err.details || err.message || 'API call failed';
    logStatus(`Error: ${msg}`);
}

// --- Event Listeners ---
DOM.saveConfigBtn.addEventListener('click', initApis);
DOM.createSheetBtn.addEventListener('click', createSpreadsheet);
DOM.updateCellBtn.addEventListener('click', updateCell);
DOM.signoutBtn.addEventListener('click', signOut);
