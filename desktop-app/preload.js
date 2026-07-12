'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// ─── Expose safe APIs to renderer ─────────────────────────────────────────
contextBridge.exposeInMainWorld('entitykart', {
  // Service management
  getServiceStatus: () => ipcRenderer.invoke('get-service-status'),
  getServicesConfig: () => ipcRenderer.invoke('get-services-config'),
  startServices: () => ipcRenderer.invoke('start-services'),
  stopServices: () => ipcRenderer.invoke('stop-services'),
  restartService: (containerName) => ipcRenderer.invoke('restart-service', containerName),

  // Log streaming
  startLogStream: (containerName) => ipcRenderer.invoke('start-log-stream', containerName),
  stopLogStream: (containerName) => ipcRenderer.invoke('stop-log-stream', containerName),

  // Browser
  openBrowser: (url) => ipcRenderer.invoke('open-browser', url),

  // Listeners (startup events)
  onStartupLog: (callback) => ipcRenderer.on('startup-log', (_, msg) => callback(msg)),
  onStartupStatus: (callback) => ipcRenderer.on('startup-status', (_, data) => callback(data)),
  onServiceStatus: (callback) => ipcRenderer.on('service-status', (_, data) => callback(data)),
  onContainerLog: (callback) => ipcRenderer.on('container-log', (_, data) => callback(data)),

  // Remove listeners
  removeAllListeners: (channel) => ipcRenderer.removeAllListeners(channel),

  // Window controls
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),
});
