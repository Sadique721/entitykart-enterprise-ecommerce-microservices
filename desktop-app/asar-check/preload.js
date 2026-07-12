const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
    // Config
    checkConfigExists:  ()       => ipcRenderer.invoke('check-config-exists'),
    readConfig:         ()       => ipcRenderer.invoke('read-config'),
    saveConfig:         (cfg)    => ipcRenderer.invoke('save-config', cfg),

    // Services
    getServiceStatus:   ()       => ipcRenderer.invoke('get-service-status'),
    getServicesList:    ()       => ipcRenderer.invoke('get-services-list'),

    // Docker controls
    startAll:           ()       => ipcRenderer.invoke('start-all'),
    stopAll:            ()       => ipcRenderer.invoke('stop-all'),
    startService:       (name)   => ipcRenderer.invoke('start-service', name),
    stopService:        (name)   => ipcRenderer.invoke('stop-service', name),
    restartService:     (name)   => ipcRenderer.invoke('restart-service', name),

    // Logs
    streamLogs:         (svc)    => ipcRenderer.send('stream-logs', svc),
    stopLogStream:      ()       => ipcRenderer.send('stop-log-stream'),
    onLogData:          (cb)     => ipcRenderer.on('log-data', (_, d) => cb(d)),

    // Health
    checkHealth:        ()       => ipcRenderer.invoke('check-health'),
    onHealthResult:     (cb)     => ipcRenderer.on('health-result', (_, r) => cb(r)),

    // Window actions (wizard)
    closeWizard:        ()       => ipcRenderer.send('close-wizard'),
    openProjectFolder:  ()       => ipcRenderer.send('open-project-folder'),
});
