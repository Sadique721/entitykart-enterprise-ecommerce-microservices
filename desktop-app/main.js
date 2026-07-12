'use strict';

const { app, BrowserWindow, ipcMain, shell, Tray, Menu, nativeImage } = require('electron');
const { spawn, exec } = require('child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');
const net = require('net');

// ─── App Configuration ─────────────────────────────────────────────────────
const APP_NAME = 'EntityKart';
const DOCKER_COMPOSE_FILE = path.join(__dirname, '..', 'docker-compose.yml');
const FRONTEND_URL = 'http://localhost:9001';
const GATEWAY_URL = 'http://localhost:9900';

const SERVICES = [
  { name: 'Zookeeper',       port: 9090, container: 'zookeeper_entitykart',      required: true  },
  { name: 'Kafka',           port: 9092, container: 'kafka_entitykart',           required: true  },
  { name: 'Gateway/Eureka',  port: 9900, container: 'common-services_entitykart', required: true  },
  { name: 'User Service',    port: 9902, container: 'user-service_entitykart',    required: false },
  { name: 'Product Service', port: 9903, container: 'product-service_entitykart', required: false },
  { name: 'Cart Service',    port: 9904, container: 'cart-service_entitykart',    required: false },
  { name: 'Order Service',   port: 9905, container: 'order-service_entitykart',   required: false },
  { name: 'Payment Service', port: 9906, container: 'payment-service_entitykart', required: false },
  { name: 'Wishlist Service',port: 9907, container: 'wishlist-service_entitykart',required: false },
  { name: 'Review Service',  port: 9908, container: 'review-service_entitykart',  required: false },
  { name: 'Return Service',  port: 9909, container: 'return-service_entitykart',  required: false },
  { name: 'Nginx/Frontend',  port: 9001, container: 'nginx_entitykart',           required: true  },
];

// ─── State ─────────────────────────────────────────────────────────────────
let mainWindow = null;
let splashWindow = null;
let startupWindow = null;
let tray = null;
let dockerProcess = null;
let logStreams = {};
let serviceStatuses = {};
let startupComplete = false;

// Initialize all service statuses to unknown
SERVICES.forEach(s => { serviceStatuses[s.name] = 'unknown'; });

// ─── Utility ───────────────────────────────────────────────────────────────
function getIconPath() {
  const ico = path.join(__dirname, 'build', 'icon.ico');
  return fs.existsSync(ico) ? ico : null;
}

function checkPort(port) {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    socket.setTimeout(1000);
    socket.on('connect', () => { socket.destroy(); resolve(true); });
    socket.on('error', () => resolve(false));
    socket.on('timeout', () => { socket.destroy(); resolve(false); });
    socket.connect(port, '127.0.0.1');
  });
}

function runCommand(cmd, args, cwd) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { cwd, shell: true });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', d => { stdout += d.toString(); });
    child.stderr.on('data', d => { stderr += d.toString(); });
    child.on('close', code => {
      if (code === 0) resolve(stdout.trim());
      else reject(new Error(stderr || stdout || `Exit code ${code}`));
    });
  });
}

function sendToRenderer(channel, data) {
  [mainWindow, startupWindow].forEach(win => {
    if (win && !win.isDestroyed()) {
      win.webContents.send(channel, data);
    }
  });
}

// ─── Docker Management ────────────────────────────────────────────────────
async function isDockerRunning() {
  try {
    await runCommand('docker', ['info'], os.tmpdir());
    return true;
  } catch {
    return false;
  }
}

async function startDockerDesktop() {
  const paths = [
    'C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe',
    path.join(os.homedir(), 'AppData\\Local\\Programs\\Docker\\Docker Desktop.exe'),
  ];
  for (const p of paths) {
    if (fs.existsSync(p)) {
      spawn(p, [], { detached: true, stdio: 'ignore' }).unref();
      return true;
    }
  }
  return false;
}

async function waitForDocker(maxWaitMs = 120000) {
  const start = Date.now();
  while (Date.now() - start < maxWaitMs) {
    if (await isDockerRunning()) return true;
    sendToRenderer('startup-log', '⏳ Waiting for Docker Engine...');
    await new Promise(r => setTimeout(r, 3000));
  }
  return false;
}

async function startAllServices() {
  try {
    sendToRenderer('startup-log', '🚀 Starting all microservices...');
    sendToRenderer('startup-status', { phase: 'Starting services', progress: 10 });

    dockerProcess = spawn('docker', ['compose', '-f', DOCKER_COMPOSE_FILE, 'up', '-d'], {
      shell: true,
      cwd: path.dirname(DOCKER_COMPOSE_FILE),
    });

    dockerProcess.stdout.on('data', d => {
      const line = d.toString().trim();
      if (line) sendToRenderer('startup-log', line);
    });

    dockerProcess.stderr.on('data', d => {
      const line = d.toString().trim();
      if (line) sendToRenderer('startup-log', line);
    });

    await new Promise(r => dockerProcess.on('close', r));
    sendToRenderer('startup-log', '✅ Docker compose up completed');
    sendToRenderer('startup-status', { phase: 'Waiting for services...', progress: 40 });
  } catch (err) {
    sendToRenderer('startup-log', `❌ Docker start error: ${err.message}`);
  }
}

async function stopAllServices() {
  try {
    await runCommand('docker', ['compose', '-f', DOCKER_COMPOSE_FILE, 'down'], path.dirname(DOCKER_COMPOSE_FILE));
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

async function pollServiceHealth() {
  let totalReady = 0;
  const total = SERVICES.length;

  for (const svc of SERVICES) {
    const up = await checkPort(svc.port);
    serviceStatuses[svc.name] = up ? 'healthy' : 'starting';
    if (up) totalReady++;
  }

  const progress = Math.min(40 + Math.round((totalReady / total) * 55), 99);
  sendToRenderer('service-status', { services: serviceStatuses });
  sendToRenderer('startup-status', { phase: `${totalReady}/${total} services ready`, progress });
  return totalReady;
}

// ─── Log Streaming ─────────────────────────────────────────────────────────
function startLogStreaming(containerName) {
  if (logStreams[containerName]) return;
  const proc = spawn('docker', ['logs', '-f', '--tail', '50', containerName], { shell: true });
  logStreams[containerName] = proc;
  proc.stdout.on('data', d => sendToRenderer('container-log', { container: containerName, line: d.toString() }));
  proc.stderr.on('data', d => sendToRenderer('container-log', { container: containerName, line: d.toString() }));
  proc.on('close', () => { delete logStreams[containerName]; });
}

function stopLogStreaming(containerName) {
  if (logStreams[containerName]) {
    logStreams[containerName].kill();
    delete logStreams[containerName];
  }
}

// ─── Windows ───────────────────────────────────────────────────────────────
function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 480,
    height: 300,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    skipTaskbar: true,
    resizable: false,
    webPreferences: { nodeIntegration: false, contextIsolation: true },
  });
  splashWindow.loadFile(path.join(__dirname, 'renderer', 'splash.html'));
  splashWindow.center();
}

function createStartupWindow() {
  startupWindow = new BrowserWindow({
    width: 700,
    height: 480,
    frame: false,
    transparent: false,
    alwaysOnTop: false,
    resizable: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
  });
  startupWindow.loadFile(path.join(__dirname, 'renderer', 'startup.html'));
  startupWindow.center();

  // Handle close attempt during startup
  startupWindow.on('close', (e) => {
    if (!startupComplete) {
      e.preventDefault();
    }
  });
}

function createMainWindow() {
  const iconPath = getIconPath();
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1024,
    minHeight: 700,
    show: false,
    title: 'EntityKart Enterprise',
    icon: iconPath || undefined,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
      webSecurity: false,
    },
    frame: false,
    backgroundColor: '#0a0e1a',
  });

  mainWindow.loadFile(path.join(__dirname, 'renderer', 'app.html'));

  mainWindow.once('ready-to-show', () => {
    if (startupWindow && !startupWindow.isDestroyed()) startupWindow.close();
    if (splashWindow && !splashWindow.isDestroyed()) splashWindow.close();
    mainWindow.show();
    mainWindow.focus();
    startupComplete = true;
  });

  mainWindow.on('closed', () => { mainWindow = null; });

  // System tray
  if (iconPath) {
    const img = nativeImage.createFromPath(iconPath).resize({ width: 16, height: 16 });
    tray = new Tray(img);
    const contextMenu = Menu.buildFromTemplate([
      { label: 'Open EntityKart', click: () => { mainWindow && mainWindow.focus(); } },
      { type: 'separator' },
      { label: 'Stop Services', click: () => stopAllServices() },
      { type: 'separator' },
      { label: 'Quit', click: () => { startupComplete = true; app.quit(); } },
    ]);
    tray.setToolTip('EntityKart Enterprise');
    tray.setContextMenu(contextMenu);
    tray.on('double-click', () => mainWindow && mainWindow.focus());
  }
}

// ─── IPC Handlers ──────────────────────────────────────────────────────────
ipcMain.handle('get-service-status', async () => {
  await pollServiceHealth();
  return { services: serviceStatuses };
});

ipcMain.handle('start-services', async () => {
  await startAllServices();
  return { success: true };
});

ipcMain.handle('stop-services', async () => {
  return await stopAllServices();
});

ipcMain.handle('restart-service', async (_, containerName) => {
  try {
    await runCommand('docker', ['restart', containerName], os.tmpdir());
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('start-log-stream', (_, containerName) => {
  startLogStreaming(containerName);
  return { success: true };
});

ipcMain.handle('stop-log-stream', (_, containerName) => {
  stopLogStreaming(containerName);
  return { success: true };
});

ipcMain.handle('open-browser', async (_, url) => {
  await shell.openExternal(url || FRONTEND_URL);
  return { success: true };
});

ipcMain.handle('get-services-config', () => SERVICES);

// Window control IPC channels
ipcMain.on('window-minimize', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (win) win.minimize();
});

ipcMain.on('window-maximize', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (win) {
    if (win.isMaximized()) {
      win.unmaximize();
    } else {
      win.maximize();
    }
  }
});

ipcMain.on('window-close', (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (win) win.close();
});


// ─── App Lifecycle ─────────────────────────────────────────────────────────
app.whenReady().then(async () => {
  createSplashWindow();

  // Short delay to show splash
  await new Promise(r => setTimeout(r, 1500));

  if (splashWindow && !splashWindow.isDestroyed()) splashWindow.close();
  createStartupWindow();

  // Step 1: Check / start Docker
  sendToRenderer('startup-log', '🐳 Checking Docker Engine...');
  const dockerReady = await isDockerRunning();
  if (!dockerReady) {
    sendToRenderer('startup-log', '🚀 Starting Docker Desktop...');
    await startDockerDesktop();
    const waited = await waitForDocker(120000);
    if (!waited) {
      sendToRenderer('startup-log', '❌ Docker Engine did not start. Please launch Docker Desktop manually.');
      sendToRenderer('startup-status', { phase: 'Docker unavailable', progress: 0 });
      return;
    }
  }
  sendToRenderer('startup-log', '✅ Docker Engine ready');
  sendToRenderer('startup-status', { phase: 'Docker ready', progress: 5 });

  // Step 2: Start all services
  await startAllServices();

  // Step 3: Poll until all services up (max 5 min)
  const maxWait = 300000;
  const start = Date.now();
  const coreServices = SERVICES.filter(s => s.required);

  while (Date.now() - start < maxWait) {
    const ready = await pollServiceHealth();
    const coreReady = coreServices.every(s => serviceStatuses[s.name] === 'healthy');

    if (coreReady) {
      sendToRenderer('startup-log', `🎉 Core services ready! Launching EntityKart...`);
      sendToRenderer('startup-status', { phase: 'Launching app...', progress: 100 });
      break;
    }

    await new Promise(r => setTimeout(r, 4000));
  }

  // Step 4: Open main window
  createMainWindow();
});

app.on('window-all-closed', async () => {
  // Stop log streams
  Object.keys(logStreams).forEach(c => stopLogStreaming(c));

  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (!mainWindow) createMainWindow();
});

// Clean up on quit
app.on('before-quit', () => {
  startupComplete = true;
  Object.keys(logStreams).forEach(c => stopLogStreaming(c));
});
