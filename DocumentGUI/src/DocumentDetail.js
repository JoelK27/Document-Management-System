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
        window.location.href = 'FrontPage.html';
    } catch (e) {
        console.error('Delete failed', e);
        hideDeleteModal();
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

function formatSize(bytes){
    if (bytes == null) return '-';
    const units = ['B','KB','MB','GB','TB'];
    let i = 0; let v = bytes;
    while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
    return (i === 0 ? v.toFixed(0) : v.toFixed(1)) + ' ' + units[i];
}

function formatDate(str){
    if(!str) return '-';
    return str.replace('T',' ').replace(/\.\d+$/,'').replace('Z','');
}


async function loadPreview(id, doc){
    if (!doc || !isPdf(doc)) {
        els.preview.innerHTML = '<div class="text-sm opacity-60">No preview</div>';
        return;
    }
    // Browser-eigenen PDF Viewer verwenden
    els.preview.innerHTML = '<div class="text-sm opacity-60">Loading PDF…</div>';
    try {
        // Variante 1: Direkt Endpoint einbetten (wenn Controller Content-Disposition inline sendet)
        // const pdfUrl = `http://localhost:8080/api/documents/${id}/file`;

        // Variante 2: Blob URL (funktioniert auch wenn Server "attachment" sendet)
        const blobUrl = await getDocumentFileUrl(id);

        // Container vorbereiten (volle Höhe/Scroll)
        els.preview.classList.add('p-0');
        els.preview.style.backgroundImage = 'none';
        els.preview.innerHTML = `
            <iframe 
                src="${blobUrl}#view=FitH" 
                class="w-full h-full rounded-lg"
                style="border:0;"
                title="PDF Preview">
            </iframe>
        `;
        // Optional Fallback-Link
        const fallback = document.createElement('a');
        fallback.href = blobUrl;
        fallback.target = '_blank';
        fallback.rel = 'noopener';
        fallback.className = 'absolute top-2 right-2 text-xs px-2 py-1 rounded bg-black/50 text-white hover:bg-black/70';
        fallback.textContent = 'Open in new tab';
        els.preview.style.position = 'relative';
        els.preview.appendChild(fallback);
    } catch (e) {
        els.preview.innerHTML = '<div class="text-xs text-red-500">Preview unavailable</div>';
        console.error(e);
    }
}

function isPdf(doc){
    return (doc.mimeType && doc.mimeType.toLowerCase() === 'application/pdf') ||
           (doc.fileName && doc.fileName.toLowerCase().endsWith('.pdf'));
}

function attachDeleteHandler() {
    if (!els.deleteBtn) return;
    els.deleteBtn.addEventListener('click', () => {
        showDeleteModal(docId);
    });
}

async function init(){
    if(!docId){
        els.title.textContent = 'Document not found';
        return;
    }
    try {
        const doc = await getDocument(docId);
        els.title.textContent = doc.title || 'Untitled';
        els.name.textContent = doc.fileName || '-';
        els.size.textContent = formatSize(doc.size);
        els.date.textContent = formatDate(doc.uploadDate);
        els.summary.textContent = doc.summary || 'No summary available.';
        loadPreview(docId, doc);
        addDownloadButton(docId, doc);
        attachDeleteHandler();
    } catch (e) {
        els.title.textContent = 'Error loading document';
        els.preview.innerHTML = '<div class="text-red-500 text-sm">Failed to load.</div>';
        console.error(e);
    }
}

function addDownloadButton(id, doc){
    // Inject a download button near metadata header if needed
    let metaHeader = document.querySelector('h2.text-lg.font-semibold');
    if (metaHeader && !document.getElementById('download-doc-btn')) {
        const btn = document.createElement('button');
        btn.id = 'download-doc-btn';
        btn.className = 'ml-auto flex items-center gap-1 px-3 py-1.5 text-xs font-semibold rounded bg-primary text-white hover:bg-primary/90 transition-colors';
        btn.innerHTML = '<span class="material-symbols-outlined text-base">download</span>Download';
        metaHeader.parentElement.classList.add('flex','items-center','gap-3');
        metaHeader.parentElement.appendChild(btn);
        btn.addEventListener('click', async () => {
            try {
                const url = await getDocumentFileUrl(id);
                const a = document.createElement('a');
                a.href = url;
                a.download = doc.fileName || ('document-' + id);
                a.click();
            } catch (e) {
                console.error(e);
            }
        });
    }
}

// Am Ende (vor init()) Modal initialisieren:
deleteModal = createDeleteModal();
document.getElementById('delete-cancel-btn').addEventListener('click', hideDeleteModal);
document.getElementById('delete-confirm-btn').addEventListener('click', () => {
    if (pendingDeleteId) proceedWithDelete(pendingDeleteId);
});

// init aufrufen
init();