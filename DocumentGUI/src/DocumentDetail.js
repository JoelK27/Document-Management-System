import { 
    getDocument,
    deleteDocument,
    getDocumentPreviewBlob,
    getDocumentFileUrl
} from './api/client.js';

// === Delete Modal (gleich wie FrontPage) ===
let deleteModal = null;
let pendingDeleteId = null;

function createDeleteModal() {
    const modal = document.createElement('div');
    modal.id = 'delete-confirmation-modal';
    modal.className = 'fixed inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center p-4 z-50';
    modal.style.display = 'none';
    modal.innerHTML = `
        <div class="bg-background-light dark:bg-background-dark rounded-xl p-8 max-w-sm w-full text-center shadow-2xl">
            <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-500/10 mb-4">
                <svg class="h-8 w-8 text-red-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" stroke-linecap="round" stroke-linejoin="round"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-black/90 dark:text-white/90 mb-2">Are you sure?</h3>
            <p class="text-black/60 dark:text-white/60 mb-6">This action will permanently delete the document. This cannot be undone.</p>
            <div class="flex gap-4">
                <button id="delete-cancel-btn" class="flex-1 px-6 py-3 bg-black/5 dark:bg-white/10 text-black/80 dark:text-white/80 font-bold rounded-lg hover:bg-black/10 dark:hover:bg-white/20 transition-colors">Cancel</button>
                <button id="delete-confirm-btn" class="flex-1 px-6 py-3 bg-red-500/80 text-white font-bold rounded-lg hover:bg-red-500 transition-colors">Delete</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    return modal;
}

function showDeleteModal(id) {
    pendingDeleteId = id;
    deleteModal.style.display = 'flex';
}

function hideDeleteModal() {
    pendingDeleteId = null;
    deleteModal.style.display = 'none';
}

async function proceedWithDelete(id) {
    try {
        await deleteDocument(id);
        window.location.href = '/';  // Zur Hauptseite
    } catch (e) {
        console.error('Delete failed', e);
        hideDeleteModal();
        alert('Failed to delete document. Please try again.');
    }
}

const qs = new URLSearchParams(location.search);
const docId = qs.get('id');

const els = {
    title: document.getElementById('document-title'),
    name: document.getElementById('document-name'),
    size: document.getElementById('document-size'),
    date: document.getElementById('document-upload-date'),
    summary: document.getElementById('document-summery'),
    preview: document.getElementById('document-preview'),
    deleteBtn: document.getElementById('delete-document-btn')
};

function formatSize(bytes) {
    if (bytes == null) return '-';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let i = 0; 
    let v = bytes;
    while (v >= 1024 && i < units.length - 1) { 
        v /= 1024; 
        i++; 
    }
    return (i === 0 ? v.toFixed(0) : v.toFixed(1)) + ' ' + units[i];
}

function formatDate(str) {
    if (!str) return '-';
    return str.replace('T', ' ').replace(/\.\d+$/, '').replace('Z', '');
}

async function loadPreview(id, doc) {
    if (!doc || !isPdf(doc)) {
        els.preview.innerHTML = '<div class="text-sm opacity-60">No preview</div>';
        return;
    }
    
    els.preview.innerHTML = '<div class="text-sm opacity-60">Loading PDF…</div>';
    try {
        const blobUrl = await getDocumentFileUrl(id);

        els.preview.classList.add('p-0');
        els.preview.style.backgroundImage = 'none';
        els.preview.innerHTML = `
            <iframe 
                src="${blobUrl}#view=FitH" 
                class="w-full h-full rounded-lg"
                style="border: none;"
                onload="console.log('PDF loaded successfully')"
                onerror="console.error('PDF load failed')"
            ></iframe>
        `;
    } catch (e) {
        console.error('Error loading preview:', e);
        els.preview.innerHTML = '<div class="text-red-500 text-sm">Failed to load preview</div>';
    }
}

function isPdf(doc) {
    return (doc.mimeType && doc.mimeType.toLowerCase() === 'application/pdf') ||
           (doc.fileName && doc.fileName.toLowerCase().endsWith('.pdf'));
}

function attachDeleteHandler() {
    if (!els.deleteBtn) return;
    els.deleteBtn.addEventListener('click', (e) => {
        e.preventDefault();
        showDeleteModal(docId);
    });
}

async function init() {
    // Prüfe ob ID vorhanden ist
    if (!docId) {
        console.log('No document ID provided, redirecting to 404');
        window.location.href = '404.html';
        return;
    }

    try {
        console.log('Loading document with ID:', docId);
        const doc = await getDocument(docId);
        
        // Falls kein Dokument zurückgegeben wird (sollte nicht passieren bei korrektem Backend)
        if (!doc) {
            console.log('Document not found (empty response), redirecting to 404');
            window.location.href = '404.html';
            return;
        }

        // Dokument-Details anzeigen
        els.title.textContent = doc.title || 'Untitled Document';
        els.name.textContent = doc.fileName || 'Unknown';
        els.size.textContent = formatSize(doc.size);
        els.date.textContent = formatDate(doc.uploadDate);
        els.summary.textContent = doc.summary || 'No summary available';

        // Status
        const statusEl = document.getElementById('document-status'); // ID muss im HTML existieren (siehe unten)
        if (statusEl) {
            const status = doc.ocrJobStatus || 'PENDING';
            let icon = 'hourglass_empty';
            let text = 'Processing';
            let colorClass = 'text-yellow-600 dark:text-yellow-400';

            if (status === 'COMPLETED') {
                icon = 'check_circle';
                text = 'Completed';
                colorClass = 'text-green-600 dark:text-green-400';
            } else if (status === 'FAILED') {
                icon = 'error';
                text = 'Failed';
                colorClass = 'text-red-600 dark:text-red-400';
            }

            statusEl.className = `inline-flex items-center gap-1.5 font-medium ${colorClass}`;
            statusEl.innerHTML = `<span class="material-symbols-outlined text-base">${icon}</span> ${text}`;
        }

        // Preview laden
        await loadPreview(docId, doc);
        
        // Delete Handler anhängen
        attachDeleteHandler();
        
        console.log('Document loaded successfully');
        
    } catch (error) {
        console.error('Error loading document:', error);
        
        // Spezifische Behandlung für verschiedene Fehlertypen
        if (error.response) {
            // Server hat geantwortet, aber mit Fehlercode
            if (error.response.status === 404) {
                console.log('Document not found (404), redirecting to 404 page');
                window.location.href = '404.html';
                return;
            } else if (error.response.status >= 400 && error.response.status < 500) {
                // Andere Client-Fehler (400, 401, 403, etc.)
                console.log(`Client error (${error.response.status}), redirecting to 404 page`);
                window.location.href = '404.html';
                return;
            }
        } else if (error.request) {
            // Request wurde gesendet, aber keine Antwort erhalten (Netzwerk-Fehler)
            console.log('Network error, showing error message');
            els.title.textContent = 'Network Error';
            els.preview.innerHTML = '<div class="text-red-500 text-sm">Unable to connect to server. Please try again later.</div>';
            return;
        }
        
        // Fallback für alle anderen Fehler -> 404
        console.log('Unknown error, redirecting to 404 page');
        window.location.href = '404.html';
    }
}

// Modal initialisieren und Event Listener anhängen
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM loaded, initializing DocumentDetail page');
    
    deleteModal = createDeleteModal();
    
    document.getElementById('delete-cancel-btn').addEventListener('click', () => {
        hideDeleteModal();
    });

    document.getElementById('delete-confirm-btn').addEventListener('click', () => {
        if (pendingDeleteId !== null) {
            proceedWithDelete(pendingDeleteId);
        }
    });

    // Dokument laden
    init();
});