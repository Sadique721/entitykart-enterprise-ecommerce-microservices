const { app, BrowserWindow, ipcMain, Tray, Menu, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const { spawn, exec } = require('child_process');
const net = require('net');

let mainWindow;
let splashWindow;
let wizardWindow;
let tray;
let activeLogProcess = null;

const PROJECT_ROOT = path.resolve(__dirname, '..');
const ENV_PATH = path.join(PROJECT_ROOT, '.env');

const SERVICES = [
    { name: 'API Gateway',      port: 9900, key: 'common-services',  color: '#00e5ff' },
    { name: 'User Service',     port: 8081, key: 'user-service',     color: '#a78bfa' },
    { name: 'Product Service',  port: 8082, key: 'product-service',  color: '#34d399' },
    { name: 'Cart Service',     port: 8083, key: 'cart-service',     color: '#fbbf24' },
    { name: 'Order Service',    port: 8084, key: 'order-service',    color: '#60a5fa' },
    { name: 'Payment Service',  port: 8085, key: 'payment-service',  color: '#f472b6' },
    { name: 'Review Service',   port: 8086, key: 'review-service',   color: '#fb923c' },
    { name: 'Return Service',   port: 8087, key: 'return-service',   color: '#e879f9' },
    { name: 'Wishlist Service', port: 8088, key: 'wishlist-service', color: '#2dd4bf' },
    { name: 'Kafka Broker',     port: 19092, key: 'kafka',           color: '#f87171' },
];

// Single instance lock
const isSingleInstance = app.requestSingleInstanceLock();
if (!isSingleInstance) {
    app.quit();
} else {
    app.on('second-instance', () => {
        if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.focus();
            mainWindow.show();
        }
    });
}

// ─── Splash Window (frameless is OK for splash) ───────────────────────────────
function createSplashWindow() {
    splashWindow = new BrowserWindow({
        width: 520,
        height: 320,
        frame: false,
        transparent: true,
        alwaysOnTop: true,
        resizable: false,
        center: true,
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true
        }
    });
    splashWindow.loadFile(path.join(__dirname, 'renderer', 'splash.html'));
    splashWindow.on('closed', () => { splashWindow = null; });
}

// ─── Main Window (NATIVE FRAME — real Windows title bar with Min/Max/Close) ───
function createMainWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 820,
        minWidth: 1000,
        minHeight: 680,
        show: false,
        title: 'EntityKart Control Panel',
        icon: path.join(__dirname, 'build', 'icon.ico'),
        // frame: true (DEFAULT) gives native Windows title bar with Min/Max/Close
        frame: true,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            nodeIntegration: false,
            contextIsolation: true
        }
    });

    mainWindow.loadFile(path.join(__dirname, 'renderer', 'index.html'));

    mainWindow.once('ready-to-show', () => {
        if (splashWindow) splashWindow.close();
        mainWindow.show();
        mainWindow.focus();
    });

    // Minimize to tray on close button — or fully quit if tray not needed
    mainWindow.on('close', (event) => {
        if (!app.isQuitting) {
            event.preventDefault();
            mainWindow.hide();
            if (tray) {
                tray.displayBalloon({
                    iconType: 'info',
                    title: 'EntityKart Control Panel',
                    content: 'Running in background. Right-click the tray icon to restore or exit.'
                });
            }
        }
    });

    mainWindow.on('closed', () => { mainWindow = null; });
}

// ─── Setup Wizard Window (native frame) ───────────────────────────────────────
function createWizardWindow() {
    wizardWindow = new BrowserWindow({
        width: 860,
        height: 660,
        frame: true,
        resizable: false,
        center: true,
        title: 'EntityKart — First Time Setup',
        icon: path.join(__dirname, 'build', 'icon.ico'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            nodeIntegration: false,
            contextIsolation: true
        }
    });
    wizardWindow.setMenuBarVisibility(false);
    wizardWindow.loadFile(path.join(__dirname, 'renderer', 'wizard.html'));
    wizardWindow.on('closed', () => { wizardWindow = null; });
}

// ─── System Tray ──────────────────────────────────────────────────────────────
function createTray() {
    const iconPath = path.join(__dirname, 'build', 'icon.ico');
    const fallback  = path.join(__dirname, 'renderer', 'assets', 'icon.png');
    tray = new Tray(fs.existsSync(iconPath) ? iconPath : fallback);

    const contextMenu = Menu.buildFromTemplate([
        { label: '⚡ Open Control Panel', click: () => { if (mainWindow) { mainWindow.show(); mainWindow.focus(); } } },
        { type: 'separator' },
        { label: '▶  Start All Services',  click: () => startAllServices() },
        { label: '■  Stop All Services',   click: () => stopAllServices() },
        { label: '♥  Health Check',        click: () => runHealthChecks() },
        { type: 'separator' },
        { label: '✕  Exit',               click: () => { app.isQuitting = true; app.quit(); } }
    ]);

    tray.setToolTip('EntityKart Control Panel');
    tray.setContextMenu(contextMenu);
    tray.on('double-click', () => { if (mainWindow) { mainWindow.show(); mainWindow.focus(); } });
}

// ─── App Lifecycle ────────────────────────────────────────────────────────────
app.whenReady().then(() => {
    const assetsDir = path.join(__dirname, 'renderer', 'assets');
    if (!fs.existsSync(assetsDir)) fs.mkdirSync(assetsDir, { recursive: true });

    createTray();
    createSplashWindow();

    setTimeout(() => {
        const configExists = fs.existsSync(ENV_PATH);
        if (!configExists) {
            if (splashWindow) splashWindow.close();
            createWizardWindow();
        } else {
            createMainWindow();
        }
    }, 2500);
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', () => { app.isQuitting = true; });

// ─── IPC: Config ─────────────────────────────────────────────────────────────
ipcMain.handle('check-config-exists', () => fs.existsSync(ENV_PATH));

ipcMain.handle('read-config', () => {
    if (!fs.existsSync(ENV_PATH)) return {};
    const config = {};
    fs.readFileSync(ENV_PATH, 'utf8').split(/\r?\n/).forEach(line => {
        const m = line.match(/^\s*([\w.-]+)\s*=\s*(.*)?$/);
        if (m) {
            let v = (m[2] || '').trim();
            if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) v = v.slice(1, -1);
            config[m[1]] = v;
        }
    });
    return config;
});

ipcMain.handle('save-config', (event, config) => {
    let content = '# EntityKart Configuration — generated by Control Panel\n\n';
    for (const [k, v] of Object.entries(config)) content += `${k}=${v}\n`;
    fs.writeFileSync(ENV_PATH, content, 'utf8');
    return true;
});

ipcMain.on('close-wizard', () => {
    if (wizardWindow) wizardWindow.close();
    createMainWindow();
});

ipcMain.on('open-project-folder', () => {
    shell.openPath(PROJECT_ROOT);
});

// ─── IPC: Service Status ──────────────────────────────────────────────────────
function checkPort(port) {
    return new Promise(resolve => {
        const s = new net.Socket();
        const fail = () => { s.destroy(); resolve(false); };
        s.setTimeout(800);
        s.once('error', fail);
        s.once('timeout', fail);
        s.connect(port, '127.0.0.1', () => { s.end(); resolve(true); });
    });
}

async function getStatuses() {
    const results = {};
    for (const svc of SERVICES) {
        results[svc.key] = (await checkPort(svc.port)) ? 'RUNNING' : 'STOPPED';
    }
    return results;
}

ipcMain.handle('get-service-status', async () => getStatuses());
ipcMain.handle('get-services-list',  ()      => SERVICES);

// ─── IPC: Docker Controls ─────────────────────────────────────────────────────
function runCmd(cmd) {
    return new Promise(resolve => {
        exec(cmd, { cwd: PROJECT_ROOT }, (err, stdout, stderr) => {
            resolve({ success: !err, stdout: stdout || '', stderr: stderr || '' });
        });
    });
}

function startAllServices()  { return runCmd('docker-compose up -d'); }
function stopAllServices()   { return runCmd('docker-compose stop'); }

ipcMain.handle('start-all',     async () => startAllServices());
ipcMain.handle('stop-all',      async () => stopAllServices());
ipcMain.handle('start-service', async (_, name) => runCmd(`docker-compose up -d ${name}`));
ipcMain.handle('stop-service',  async (_, name) => runCmd(`docker-compose stop ${name}`));
ipcMain.handle('restart-service', async (_, name) => runCmd(`docker-compose restart ${name}`));

// ─── IPC: Log Streaming ───────────────────────────────────────────────────────
ipcMain.on('stream-logs', (event, serviceName) => {
    if (activeLogProcess) activeLogProcess.kill();
    activeLogProcess = spawn('docker-compose', ['logs', '--tail=150', '-f', serviceName], {
        cwd: PROJECT_ROOT, shell: true
    });
    const send = data => event.reply('log-data', data.toString());
    activeLogProcess.stdout.on('data', send);
    activeLogProcess.stderr.on('data', send);
    activeLogProcess.on('close', () => { activeLogProcess = null; });
});

ipcMain.on('stop-log-stream', () => {
    if (activeLogProcess) { activeLogProcess.kill(); activeLogProcess = null; }
});

// ─── IPC: Health Checks ───────────────────────────────────────────────────────
async function runHealthChecks() {
    const report = {
        gateway:  await checkPort(9900),
        userSvc:  await checkPort(8081),
        kafka:    await checkPort(19092),
        timestamp: new Date().toISOString()
    };
    if (mainWindow) mainWindow.webContents.send('health-result', report);
    return report;
}

ipcMain.handle('check-health', async () => runHealthChecks());

// ─── IPC: Window Controls (for any custom UI needs) ──────────────────────────
ipcMain.on('window-minimize', () => { if (mainWindow) mainWindow.minimize(); });
ipcMain.on('window-maximize', () => {
    if (!mainWindow) return;
    mainWindow.isMaximized() ? mainWindow.restore() : mainWindow.maximize();
});
ipcMain.on('window-close', () => { if (mainWindow) mainWindow.close(); });
